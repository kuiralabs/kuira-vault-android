# Bug report — OZ `UnshieldedTreasury` deposit/withdraw non-functional on-chain (u128 bound → u64 VM compare)

Ready-to-post issue for OpenZeppelin `compact-contracts`, matching
`.github/ISSUE_TEMPLATE/01_bug_report.yml`. Verification appendix at the bottom
documents how we confirmed the root cause is upstream (Midnight toolchain + OZ
contract usage), not our SDK/shim/recompile.

---

## Prerequisites
- [x] I have read the Contributing Guidelines
- [x] I agree to follow the Code of Conduct
- [x] I have searched for existing issues that already report this problem, without success

## What are the steps to reproduce this issue?

```
1. Compose a contract on `multisig/UnshieldedTreasury` exposing its `_deposit(color, amount)`
   (or `_send`), and compile it (compactc 0.31.x, @midnight-ntwrk/compact-runtime 0.16.0).
2. Deploy + call `_deposit` against a REAL Midnight node — e.g. the LIVE backend
   (`MIDNIGHT_BACKEND=live`), NOT the dry-run simulator. The dry-run simulator cannot execute the
   `unshieldedBalance*` builtins, so the default test suite never exercises this path.
3. `_deposit`'s first line — the overflow guard
   `assert(unshieldedBalanceLte(disclose(color), Utils_UINT128_MAX() - disclose(amount)),
           "UnshieldedTreasury: overflow")`
   — fails during circuit execution. `_send`'s `unshieldedBalanceGte(color, amount)` fails identically.
```

## What happens?

`UnshieldedTreasury._deposit` and `._send` are **non-functional on-chain** — circuit execution
throws before any tokens move:

```
CompactError: Rust VM query failed: Execution(Decode(InvalidBuiltinDecode("u64")))
```

Root cause: the guards feed a `UINT128_MAX`-derived bound (`Utils_UINT128_MAX() - amount` ≈ 2¹²⁸)
into `unshieldedBalanceLte/Gte`, whose comparison is evaluated by the ledger VM's `lt` op — and
that op **narrows both operands to `u64`**:

```rust
// midnight-ledger onchain-vm/src/vm.rs
fn lt<D: DB>(a: &Value, b: &Value) -> Result<AlignedValue, OnchainProgramError<D>> {
    let a: u64 = (&**a).try_into()?;   // <- u64
    let b: u64 = (&**b).try_into()?;   // <- u64  (the ~2^128 bound cannot fit -> InvalidBuiltinDecode)
    Ok((a < b).into())
}
```

The bound exceeds `u64::MAX`, so the decode fails. This is a layer below the contract: the ledger
VM's comparison op is `u64` while protocol unshielded balances are `u128`
(`onchain-runtime/src/context.rs`: `balance: HashMap<TokenType, u128>`). `UnshieldedTreasury`
surfaces it by comparing against a genuinely-`u128` overflow bound.

## What were you expecting to happen?

The overflow/sufficiency guards evaluate and the deposit/withdraw proceed. Protocol unshielded
balances are `u128`, so a `u128` overflow check on them should be evaluable — either the
`unshieldedBalance*` comparison should support `u128`, or `UnshieldedTreasury` should guard overflow
without feeding a `> u64::MAX` bound into the VM (e.g. via its own `_balances` map, which is
tracked/compared as `u128` in-circuit).

## Paste any relevant logs, error output, etc.

```shell
ContractCallException$CircuitExecutionFailed:
  Circuit 'depositUnshielded' failed: Circuit execution failed:
  CompactError: Error: Rust VM query failed: Error: native query returned non-JSON:
  {"error":"query failed: Execution(Decode(InvalidBuiltinDecode(\"u64\")))"}
```

## Additional context

- Reproduced by an SDK executing the compiled contract against a **localnet Midnight node**
  (ledger 8.0.x). The contract deploys and finalizes fine; only the `unshieldedBalance*`-guarded
  custody paths fail.
- **Likely needs a Midnight compactc/ledger fix** (u64 `lt` vs u128 balances) — flagged here because
  it renders `UnshieldedTreasury` unusable as written, and OZ's dry-run simulator can't surface it
  (those builtins aren't executable there), so the module's on-chain path is effectively untested.
- **Contract-side mitigation** (what we're doing meanwhile): perform the overflow/sufficiency checks
  against the module's own `_balances` / `getTokenBalance` (tracked `u128`, compared in-circuit)
  rather than the `unshieldedBalance*` builtins.
- Confident in the mechanism (u64 `lt` op vs `~2^128` bound, observed on a live node); not 100% on
  which layer owns the *fix* (stdlib emitting a u64 compare vs. the ledger op being u64) — left for triage.

## Code Reproduction URL

_(none yet — can provide a ~15-line minimal contract with a single
`unshieldedBalanceLte(color, UINT128_MAX - x)` call; note it requires a real node to trigger.)_

## Version

`0.31.0` — observed on **both 0.31.0 and 0.31.1**; independent of the compiler patch.

---

## Verification appendix (why this is NOT our bug)

Confirmed the root cause is upstream (Midnight toolchain) + OZ contract usage, not our
SDK / QuickJS shim / 0.31.1 recompile:

1. **Ledger VM is pristine upstream.** `onchain-vm/src/vm.rs` (`lt`),
   `base-crypto/src/fab/conversions.rs` (the `u128 → u64` narrowing that raises
   `InvalidBuiltinDecode("u64")`), and `onchain-runtime/src/context.rs` (`balance: …u128`) are all
   **unmodified by us** (clean `git status` in the vendored `midnight-ledger`).
2. **Not our recompile.** Compiling the same contract with **stock compactc 0.31.0** (OZ's own pin)
   produces the identical `_unshieldedBalanceLt_0` using the `'lt'` op — same as 0.31.1. So the u64
   comparison for `unshieldedBalance*` is stdlib/compiler behavior, not our patch.
3. **Not our shim/state.** The failing decode is the pushed `~2^128` bound (from
   `UINT128_MAX - amount` in the circuit), evaluated by the VM `lt` op — independent of our
   Map/Set state handling or on-chain-state fetch. The `_balances` map is empty at deposit time
   (first op), so no stored value is involved.

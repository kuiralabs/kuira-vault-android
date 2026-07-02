# Bug report — `unshieldedBalance*` builtins accept `Uint<128>` but compare as `u64` (ledger VM `lt`)

Primary report for the **Midnight** toolchain. The issue surfaces through OpenZeppelin's
`UnshieldedTreasury`, but the root cause is a Midnight-internal inconsistency: unshielded
balances are stored/typed as `u128`, yet the ledger VM's comparison op evaluates as `u64`.

**Where to file** (verified NOT already reported as of 2026-07-01): the root is in
`midnightntwrk/midnight-ledger` (the VM `lt` op, `onchain-vm/src/vm.rs`); if the fix is judged to
belong to codegen instead, `midnightntwrk/compact` (the stdlib emitting a u64 compare). Checked
midnight-ledger issues (46 total — no match) and OpenZeppelin/compact-contracts (only the closed
feature issue #479, no bug).

## Summary

`unshieldedBalanceLte / unshieldedBalanceGte / unshieldedBalanceLt / unshieldedBalanceGt`
have `Uint<128>` value parameters (compactc accepts a `Uint<128>` argument), and protocol
unshielded balances are stored as `u128`. But the comparison compiles to the ledger VM's `lt`
op, which **narrows both operands to `u64`**. Any call whose value exceeds `u64::MAX` fails at
circuit execution with:

```
Execution(Decode(InvalidBuiltinDecode("u64")))
```

So a `u128` overflow check on a `u128` balance — the natural use of these builtins — is not
evaluable. The type system gives no warning: the API is `Uint<128>`, but only `u64`-range values
work at runtime.

## Versions

- `compactc` **0.31.0 and 0.31.1** (identical generated code — not compiler-patch-specific)
- `@midnight-ntwrk/compact-runtime` **0.16.0**
- `midnight-ledger` **8.0.x** (observed on a localnet node)

## Steps to reproduce (confirmed via the `mn` CLI — Midnight tooling only)

Minimal contract (`repro.compact`, 6 lines):

```compact
pragma language_version >= 0.23.0;
import CompactStandardLibrary;

export circuit checkOverflow(color: Bytes<32>): Boolean {
  // 18446744073709551616 = 2^64 (one over u64::MAX) — a valid Uint<128> literal:
  return unshieldedBalanceLte(disclose(color), 18446744073709551616 as Uint<128>);
}
```

```bash
compact compile +0.31.1 src/repro.compact src/managed/repro
mn airdrop 1000 --wallet alice
mn dust register --wallet alice
mn contract deploy --managed src/managed/repro --wallet alice --network undeployed   # succeeds
mn contract call  --managed src/managed/repro --wallet alice --network undeployed \
   --circuit checkOverflow --address <deployed-addr> --args '{"color":[1,1,...,1]}'   # FAILS
```

The deploy succeeds; the **call fails** during circuit execution:

```
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
    at Module.queryLedgerState (@midnight-ntwrk/compact-runtime/dist/circuit-context.js:145)
    at Contract._unshieldedBalanceGt_0  (managed/repro/contract/index.js:137)
    at Contract._unshieldedBalanceLte_0 (managed/repro/contract/index.js:172)
    at Contract._checkOverflow_0        (managed/repro/contract/index.js:178)
    at checkOverflow                    (managed/repro/contract/index.js:90)
```

The error text is decisive: **"failed to decode for built-in type u64 *after successful
typecheck*"** — `compactc` accepts the `Uint<128>` argument (typecheck passes), but the runtime
comparison decodes `u64`. Reproduced entirely inside Midnight's own stack (`mn` →
`@midnight-ntwrk/midnight-js-contracts` → `@midnight-ntwrk/compact-runtime`); the same failure
appears in the native Rust ledger as `InvalidBuiltinDecode("u64")`.

(Real-world trigger: OpenZeppelin `multisig/UnshieldedTreasury._deposit` uses exactly
`unshieldedBalanceLte(color, UINT128_MAX - amount)`; `._send` uses `unshieldedBalanceGte`. Minimal
repro dir: `repro/unshielded-balance-u64/`.)

## Root cause (code)

```rust
// midnight-ledger onchain-vm/src/vm.rs — the Lt op operand decode
fn lt<D: DB>(a: &Value, b: &Value) -> Result<AlignedValue, OnchainProgramError<D>> {
    let a: u64 = (&**a).try_into()?;   // <- narrows to u64
    let b: u64 = (&**b).try_into()?;   // <- ~2^128 bound cannot fit -> InvalidBuiltinDecode("u64")
    Ok((a < b).into())
}
```
```rust
// midnight-ledger base-crypto/src/fab/conversions.rs — the narrowing that raises the error
u128::try_from(value)?.try_into().map_err(|_| InvalidBuiltinDecode(stringify!($ty)))
```
```rust
// midnight-ledger onchain-runtime/src/context.rs — balances ARE u128
pub balance: storage::storage::HashMap<TokenType, u128, D>,
```

The generated code for `unshieldedBalanceLte` emits a `'lt'` op over the `u128` value — confirmed
identical on compactc 0.31.0 and 0.31.1.

## Expected

Either:
- **(a)** `unshieldedBalance*` comparisons evaluate over the full `Uint<128>` range (make the VM
  comparison / the builtin `u128`-capable), consistent with `u128` balances; or
- **(b)** if these builtins are intentionally `u64`-only, narrow their signatures to `Uint<64>`
  so `compactc` rejects `Uint<128>` arguments at compile time instead of failing at runtime.

Today the API type (`Uint<128>`) and the runtime behavior (`u64`) disagree silently.

## Impact

Any contract that overflow/sufficiency-checks a `u128` unshielded balance via these builtins is
non-functional on-chain. OpenZeppelin's `UnshieldedTreasury` (deposit + withdraw) is one concrete
case; it went unnoticed because the dry-run simulator can't execute `unshieldedBalance*`.

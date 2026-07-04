# Minimal repro — `unshieldedBalance*` accepts `Uint<128>` but the ledger VM compares as `u64`

A ~6-line contract that reproduces, using **only Midnight's `mn` CLI + toolchain** (no third-party
SDK), a mismatch where an `unshieldedBalance*` builtin type-checks a `Uint<128>` argument but the
runtime comparison decodes `u64` — so any value above `u64::MAX` fails at circuit execution.

Full analysis: `../../docs/midnight-unshielded-balance-u128-bug.md`.

## The contract — `src/repro.compact`

```compact
pragma language_version >= 0.23.0;
import CompactStandardLibrary;

// 18446744073709551616 = 2^64 (one over u64::MAX) — a valid Uint<128> literal.
export circuit checkOverflow(color: Bytes<32>): Boolean {
  return unshieldedBalanceLte(disclose(color), 18446744073709551616 as Uint<128>);
}
```

## Reproduce (verified end-to-end via `mn`)

```bash
# 1. Compile (compactc 0.31.0 or 0.31.1 — identical result)
compact compile +0.31.1 src/repro.compact src/managed/repro

# 2. Fund a dev wallet and register dust; WAIT for the dust to mature before deploying
mn airdrop 1000 --wallet alice
mn dust register --wallet alice
#   (wait until `mn dust --wallet alice` shows a positive matured balance)

# 3. Deploy (succeeds)
ADDR=$(mn contract deploy --managed src/managed/repro --wallet alice --network undeployed \
        | grep -oE '[0-9a-f]{64}' | head -1)

# 4. Call checkOverflow with any 32-byte color (FAILS)
mn contract call --managed src/managed/repro --wallet alice --network undeployed \
  --circuit checkOverflow --address "$ADDR" \
  --args "{\"color\":[$(python3 -c "print(','.join(['1']*32))")]}"
```

## Result

Compile + deploy succeed. The **call fails** during circuit execution:

```
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
  at Module.queryLedgerState (@midnight-ntwrk/compact-runtime/.../circuit-context.js:145)
  at Contract._unshieldedBalanceGt_0  (managed/repro/contract/index.js)
  at Contract._unshieldedBalanceLte_0 (managed/repro/contract/index.js)
  at Contract._checkOverflow_0        (managed/repro/contract/index.js)
```

The message is the crux: **"failed to decode for built-in type u64 *after successful typecheck*"** —
`compactc` accepts the `Uint<128>` argument (typecheck passes), but the runtime comparison decodes
`u64`. The native Rust ledger reports the same as `InvalidBuiltinDecode("u64")`.

## Why

- `unshieldedBalance{Lte,Gte,Lt,Gt}(Bytes<32>, Uint<128>)` type-check with a `Uint<128>` value.
- The comparison compiles to the ledger VM's `lt` op, which narrows both operands to `u64`
  (`midnight-ledger onchain-vm/src/vm.rs`).
- Protocol unshielded balances are themselves `u128` (`onchain-runtime/src/context.rs`:
  `balance: HashMap<TokenType, u128>`).
- So any value `> u64::MAX` (e.g. `2^64`, or `UINT128_MAX - amount` as OpenZeppelin's
  `UnshieldedTreasury` overflow guard uses) fails to decode at runtime despite a clean typecheck.

Versions: compactc **0.31.0 and 0.31.1** (identical codegen), `@midnight-ntwrk/compact-runtime`
**0.16.0**, ledger **8.0.x** (localnet). No Kuira/third-party SDK involved — reproduced entirely
with `mn` → `@midnight-ntwrk/midnight-js-contracts` → `@midnight-ntwrk/compact-runtime`.

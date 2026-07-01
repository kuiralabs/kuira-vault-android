# Minimal repro — `unshieldedBalance*` u128 arg decoded as u64

A ~6-line contract that reproduces, using **only Midnight's `mn` CLI + toolchain** (no third-party
SDK), the failure where an `unshieldedBalance*` builtin accepts a `Uint<128>` argument at
compile time but the runtime comparison decodes `u64`.

See `../../docs/midnight-unshielded-balance-u128-bug.md` for the full analysis.

## The contract — `src/repro.compact`

```compact
export circuit checkOverflow(color: Bytes<32>): Boolean {
  // 2^64 = 18446744073709551616, one over u64::MAX — a valid Uint<128> literal.
  return unshieldedBalanceLte(disclose(color), 18446744073709551616 as Uint<128>);
}
```

## Reproduce

```bash
compact compile +0.31.1 src/repro.compact src/managed/repro

mn airdrop 1000 --wallet alice
mn dust register --wallet alice
# wait for dust to mature, then:
ADDR=$(mn contract deploy --managed src/managed/repro --wallet alice --network undeployed | grep -oE '[0-9a-f]{64}' | head -1)

# color = any 32 bytes
mn contract call --managed src/managed/repro --wallet alice --network undeployed \
  --circuit checkOverflow --address "$ADDR" \
  --args "{\"color\":[$(python3 -c "print(','.join(['1']*32))")]}"
```

## Result

Deploy succeeds. The **call fails**:

```
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
  at Module.queryLedgerState (@midnight-ntwrk/compact-runtime/.../circuit-context.js:145)
  at Contract._unshieldedBalanceGt_0  (managed/repro/contract/index.js:137)
  at Contract._unshieldedBalanceLte_0 (managed/repro/contract/index.js:172)
  at Contract._checkOverflow_0        (managed/repro/contract/index.js:178)
```

## Why

- `unshieldedBalanceLte(Bytes<32>, Uint<128>)` type-checks with a `Uint<128>` value.
- The comparison compiles to the ledger VM's `lt` op, which narrows operands to `u64`
  (`midnight-ledger onchain-vm/src/vm.rs`).
- Protocol unshielded balances are themselves `u128` (`onchain-runtime/src/context.rs`).
- So any value `> u64::MAX` (e.g. `2^64`, or `UINT128_MAX - amount` as OZ's `UnshieldedTreasury`
  uses) fails to decode at runtime, despite a clean typecheck.

Versions: compactc 0.31.0 **and** 0.31.1 (identical codegen), `@midnight-ntwrk/compact-runtime`
0.16.0, ledger 8.0.x.

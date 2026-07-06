# `unshieldedBalance*` accepts `Uint<128>` but the comparison decodes `u64`

`unshieldedBalance{Lte,Gte,Lt,Gt}` accept a `Uint<128>` value and type-check cleanly, but the
runtime comparison decodes the operand as `u64`. Any value above `u64::MAX` fails at circuit
execution, even though protocol unshielded balances are themselves `u128`.

## Run

The compiled contract is committed, so no Compact toolchain is needed — just Node and npm:

```bash
npm install
node sdk-repro.mjs
```

This runs the circuit in-memory with `@midnight-ntwrk/compact-runtime` and fails with the error
under [Result](#result).

To recompile the contract from source instead (needs `compactc` 0.31.x):

```bash
compact compile +0.31.1 src/repro.compact src/managed/repro
```

`src/repro.compact`:

```compact
pragma language_version >= 0.23.0;
import CompactStandardLibrary;

// 18446744073709551616 = 2^64, one over u64::MAX — a valid Uint<128> literal.
export circuit checkOverflow(color: Bytes<32>): Boolean {
  return unshieldedBalanceLte(disclose(color), 18446744073709551616 as Uint<128>);
}
```

## Result

```
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
    at Module.queryLedgerState (@midnight-ntwrk/compact-runtime/dist/circuit-context.js:145)
    at Contract._unshieldedBalanceGt_0  (managed/repro/contract/index.js)
    at Contract._unshieldedBalanceLte_0 (managed/repro/contract/index.js)
    at Contract._checkOverflow_0        (managed/repro/contract/index.js)
```

`compactc` accepts the `Uint<128>` argument (the type-check passes), but the comparison decodes
`u64` at runtime. The same failure occurs when the circuit is called on a deployed contract.

## Root cause

- `unshieldedBalance{Lte,Gte,Lt,Gt}(Bytes<32>, Uint<128>)` type-check with a `Uint<128>` value.
- The comparison compiles to the ledger VM's `lt` op, which narrows both operands to `u64`
  (`midnight-ledger onchain-vm/src/vm.rs`).
- Protocol unshielded balances are `u128` (`onchain-runtime/src/context.rs`:
  `balance: HashMap<TokenType, u128>`).

So any value `> u64::MAX` (e.g. `2^64`, or `UINT128_MAX - amount` as an overflow guard) fails to
decode at runtime despite a clean type-check.

## Versions

- `compactc` 0.31.0 and 0.31.1 (identical generated code)
- `@midnight-ntwrk/compact-runtime` 0.16.0
- `midnight-ledger` 8.0.x

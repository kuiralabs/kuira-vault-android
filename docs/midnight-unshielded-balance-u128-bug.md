# `unshieldedBalance*` accepts `Uint<128>` but the comparison decodes `u64`

## Summary

`unshieldedBalanceLte`, `unshieldedBalanceGte`, `unshieldedBalanceLt`, and `unshieldedBalanceGt`
accept a `Uint<128>` value and type-check cleanly, and protocol unshielded balances are stored as
`u128`. But the comparison compiles to the ledger VM's `lt` op, which narrows both operands to
`u64`. Any value above `u64::MAX` fails at circuit execution:

```
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
```

The native ledger reports the same as `Execution(Decode(InvalidBuiltinDecode("u64")))`.

## Versions

- `compactc` 0.31.0 and 0.31.1 (identical generated code)
- `@midnight-ntwrk/compact-runtime` 0.16.0
- `midnight-ledger` 8.0.x

## Steps to reproduce

A 6-line contract, run in-memory with `@midnight-ntwrk/compact-runtime`:

```compact
pragma language_version >= 0.23.0;
import CompactStandardLibrary;

// 18446744073709551616 = 2^64 (one over u64::MAX), a valid Uint<128> literal.
export circuit checkOverflow(color: Bytes<32>): Boolean {
  return unshieldedBalanceLte(disclose(color), 18446744073709551616 as Uint<128>);
}
```

```js
import {
  createConstructorContext,
  createCircuitContext,
  dummyContractAddress,
} from '@midnight-ntwrk/compact-runtime';
import { Contract } from './managed/repro/contract/index.js';

const coinPublicKey = '00'.repeat(32);
const contract = new Contract({});
const ctorCtx = createConstructorContext(undefined, coinPublicKey);
const init = contract.initialState(ctorCtx);
const circuitCtx = createCircuitContext(
  dummyContractAddress(), coinPublicKey, init.currentContractState, init.currentPrivateState,
);

contract.impureCircuits.checkOverflow(circuitCtx, new Uint8Array(32).fill(1));
```

```
compact compile +0.31.1 repro.compact managed/repro
node sdk-repro.mjs
```

The circuit execution throws:

```
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
    at Module.queryLedgerState (@midnight-ntwrk/compact-runtime/dist/circuit-context.js:145)
    at Contract._unshieldedBalanceGt_0
    at Contract._unshieldedBalanceLte_0
    at Contract._checkOverflow_0
```

The type-check accepts the `Uint<128>` argument; the runtime comparison decodes `u64`. The same
failure occurs when the circuit is called on a deployed contract.

## Root cause

```rust
// onchain-vm/src/vm.rs — the Lt op operand decode
fn lt<D: DB>(a: &Value, b: &Value) -> Result<AlignedValue, OnchainProgramError<D>> {
    let a: u64 = (&**a).try_into()?;   // narrows to u64
    let b: u64 = (&**b).try_into()?;   // a value > u64::MAX cannot fit -> InvalidBuiltinDecode("u64")
    Ok((a < b).into())
}
```

```rust
// onchain-runtime/src/context.rs — unshielded balances are u128
pub balance: storage::storage::HashMap<TokenType, u128, D>,
```

The generated code for `unshieldedBalance*` emits an `lt` op over the `u128` value — identical on
compactc 0.31.0 and 0.31.1.

## Expected

Either:

- **(a)** `unshieldedBalance*` comparisons evaluate over the full `Uint<128>` range, consistent
  with `u128` balances; or
- **(b)** if the builtins are intentionally `u64`-only, their signatures narrow to `Uint<64>` so
  `compactc` rejects a `Uint<128>` argument at compile time instead of failing at runtime.

Today the API type (`Uint<128>`) and the runtime behavior (`u64`) disagree silently.

## Impact

Any contract that checks a `u128` unshielded balance via these builtins is non-functional on-chain.
A common case is an overflow guard of the form `unshieldedBalanceLte(color, UINT128_MAX - amount)`,
whose `~2^128` bound triggers the failure.

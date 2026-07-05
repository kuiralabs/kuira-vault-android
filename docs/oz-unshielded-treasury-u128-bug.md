# `UnshieldedTreasury` deposit/withdraw fail on-chain (u128 bound → u64 comparison)

Ready-to-post issue for `OpenZeppelin/compact-contracts`, matching
`.github/ISSUE_TEMPLATE/01_bug_report.yml`.

## Prerequisites
- [x] I have read the Contributing Guidelines
- [x] I agree to follow the Code of Conduct
- [x] I have searched for existing issues that already report this problem, without success

## What are the steps to reproduce this issue?

```
1. Compose a contract on multisig/UnshieldedTreasury exposing its _deposit(color, amount)
   (or _send), and compile it (compactc 0.31.x, @midnight-ntwrk/compact-runtime 0.16.0).
2. Call _deposit — its first line is an overflow guard:
     assert(unshieldedBalanceLte(disclose(color), Utils_UINT128_MAX() - disclose(amount)),
            "UnshieldedTreasury: overflow")
   _send's sufficiency guard is unshieldedBalanceGte(color, amount).
3. The circuit execution fails at the guard.
```

## What happens?

`UnshieldedTreasury._deposit` and `._send` fail during circuit execution, before any tokens move:

```
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
```

The guards pass a `UINT128_MAX`-derived bound (`UINT128_MAX - amount`, ~2^128) to
`unshieldedBalance{Lte,Gte}`. These builtins accept a `Uint<128>` argument and type-check cleanly,
but the comparison decodes the operand as `u64` at runtime, so any value above `u64::MAX` fails.

## What were you expecting to happen?

The overflow/sufficiency guards evaluate and deposit/withdraw proceed. Protocol unshielded balances
are `u128`, so a `u128` bound should be evaluable.

## Paste any relevant logs, error output, etc.

```shell
CompactError: Error: failed to decode for built-in type u64 after successful typecheck
    at Module.queryLedgerState (@midnight-ntwrk/compact-runtime/dist/circuit-context.js:145)
    at Contract._unshieldedBalanceGt_0
    at Contract._unshieldedBalanceLte_0
```

## Additional context

The underlying issue is in the Compact toolchain: `unshieldedBalance*` type-check a `Uint<128>`
argument but compile to a comparison that decodes `u64` (protocol unshielded balances are `u128`).
It renders `UnshieldedTreasury`'s deposit/withdraw guards unusable as written, and the dry-run
simulator does not execute `unshieldedBalance*`, so the module's on-chain path is not covered by the
default tests. This likely needs a compactc/ledger fix; a contract-side option is to check the
module's own tracked `_balances` (`getTokenBalance`, `u128` in-circuit) instead of the builtins.

## Code Reproduction URL

https://github.com/nel349/unshielded-balance-u64-repro — `npm install && node sdk-repro.mjs`.

A 6-line contract reproduces the underlying failure with `@midnight-ntwrk/compact-runtime`, no OZ
modules:

```compact
pragma language_version >= 0.23.0;
import CompactStandardLibrary;

export circuit checkOverflow(color: Bytes<32>): Boolean {
  // 18446744073709551616 = 2^64, one over u64::MAX, a valid Uint<128> literal.
  return unshieldedBalanceLte(disclose(color), 18446744073709551616 as Uint<128>);
}
```

Run in-memory with `contract.impureCircuits.checkOverflow(circuitContext, color)` — the call fails
with the same `failed to decode for built-in type u64` error.

## Version

`0.31.0` — observed on both 0.31.0 and 0.31.1.

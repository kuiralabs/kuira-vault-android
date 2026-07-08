# Vault contract

An M-of-N multisig treasury in Compact, composed from the vendored
[OpenZeppelin Compact Contracts](https://github.com/OpenZeppelin/compact-contracts)
multisig modules (`../oz/multisig/`, MIT, pinned at `v0.3.0-alpha` — see
[`../oz/UPSTREAM.md`](../oz/UPSTREAM.md)):

```compact
// Vault.compact (excerpt) — the composition
import "../oz/multisig/ProposalManager"   prefix Proposal_;
import "../oz/multisig/UnshieldedTreasury" prefix Treasury_;
import "../oz/multisig/SignerManager"<Either<ZswapCoinPublicKey, ContractAddress>> prefix Signer_;
```

Five exported actions — permissionless `depositUnshielded`, signer-gated
`proposeWithdrawal` / `approve` / `revokeApproval`, threshold-gated
permissionless `execute` — plus view circuits (`getThreshold`,
`getUnshieldedBalance`, `getProposal`, `getApprovalCount`, `isSigner`, …)
that the app reads via `MidnightContract.read()`.

Unit tests (signer/threshold/proposal accounting, no chain) run against
the OZ dry-run simulator: `npm test`.

## Toolchain (pinned)

| Layer | Version |
|---|---|
| `compactc` (compiler) | **0.31.1** |
| Language (`pragma language_version`) | **0.23.0** |
| `@midnight-ntwrk/compact-runtime` | **0.16.0** |

Mismatched versions surface as `language version X.Y.Z mismatch` at
compile time. The three numbers are intentionally independent — the
compiler binary, the source language, and the JS runtime each version
on their own cadence.

## Build the artifacts

The Android app consumes `src/managed/Vault/` — `contract/index.js`
(contract class), `keys/*.prover` / `keys/*.verifier` (one pair per
circuit). These are committed to the repo so the app builds out of the
box; rebuild only if you edit `Vault.compact` or the vendored modules.

```bash
compact compile +0.31.1 src/Vault.compact src/managed/Vault
```

## Verify against a localnet

```bash
mn localnet up                                      # if not running
mn airdrop 1000 --wallet dev-alice                  # fund a wallet
mn dust register --wallet dev-alice --network undeployed

# Deploy takes constructor args (3 signers + threshold), so the app's
# deploy flow is the practical path. The canonical verification is the
# on-chain e2e, which deploys with three real wallets and runs the full
# governance flow:
#   VAULT_E2E_CLASS=...VaultDeployE2ETest#governance_full_flow_2of3 ../fund-vault-e2e.sh
```

## When the Compact toolchain bumps

The committed artifacts go stale. To upgrade:

1. `compact update` to install the new toolchain version.
2. `compact --version` and `compact compile --runtime-version` /
   `--language-version` to confirm the new triple.
3. Bump `engines.compactc` in this `package.json` to the new compiler.
4. Bump `@midnight-ntwrk/compact-runtime` dep to the new runtime.
5. Bump `pragma language_version` in `Vault.compact` (and check the
   vendored `../oz` modules) to the new language version.
6. `rm -rf src/managed/Vault && npm run compile`.
7. Copy the regenerated `contract/index.js` into the app's assets and
   the circuit keys into `app/src/main/assets/keys/` (the contract
   Gradle plugin provisions these on build).
8. Re-verify with `npm test` + the on-chain e2e.
9. Commit the regenerated artifacts.

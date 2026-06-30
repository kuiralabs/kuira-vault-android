# Vendored OpenZeppelin Compact Contracts

This directory is a **pinned, editable snapshot** of OpenZeppelin's Compact
contract library. We vendor it (rather than submodule it) because the Vault
deliberately **corrects parts that upstream ships as placeholders** — those
edits live here, in place, and must not be clobbered by an upstream pull.

| | |
|---|---|
| Upstream | https://github.com/OpenZeppelin/compact-contracts |
| Version | `v0.3.0-alpha` |
| Commit | `c096e317e8609a20c719bd2a69fad3ea2c36f181` |
| Vendored | `contracts/src/` only (the `.compact` sources + `audits/`, `LICENSE`, `CHANGELOG.md`). The JS/TS toolchain (turbo, vitest, their simulator) is intentionally omitted — we compile `.compact` directly with `compactc` and execute through the Kuira compact-engine. |
| License | MIT (see `LICENSE`) |

## Toolchain it was verified against

- `compactc` **0.31.1** (language `0.23.0`, runtime `0.16.0`)
- Generated code targets **ledger-8.0.2**, which the Kuira SDK's bundled
  **8.0.3** runtime handles. We stay on 8.0.3.
- All 31 real modules (audited access/security/token/utils/crypto + the
  unaudited multisig suite) compile clean under 0.31.1.

## Audit status (why we touch only what we touch)

The May 2026 OpenZeppelin audit covers **access, security, token, utils** at
`v0.1.0` (see `audits/`). The **multisig** suite is newer, **not in audit
scope**, and upstream documents that its **signature verification is stubbed
pending ECDSA + Keccak primitives** (upstream #475). The Vault keeps the
audited modules as-is and replaces that stub with a real, enforced M-of-N
proof-of-key check (modelled on the audited `access/ZOwnablePK`).

## Our corrections

Tracked as they land (each notes the upstream file + what changed and why):

- _(pending)_ `multisig/` — replace stubbed signature verification with
  enforced proof-of-key threshold auth.

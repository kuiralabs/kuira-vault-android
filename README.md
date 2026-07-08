# Kuira Vault — Android

A working **M-of-N multisig treasury** on [Midnight](https://midnight.network),
as a native Android dApp. Real money movement on a privacy L1 — deposits fund
the contract with actual wallet UTXOs, withdrawals pay recipients with actual
UTXOs — governed by a threshold of independent signer wallets, each a
self-custody passkey identity on its own device.

```
        deposit            propose              approve            execute
  anyone ──💰──▶ vault   signer ──📋──▶ #1   signers ─✍️✍️─▶ 2/2   anyone ──✅──▶ recipient
                          "2 NIGHT → addr"     (M of N met)         funds move
```

No proposal moves money. No single key moves money. Once M of N distinct
signers approve, *anyone* can settle it — the approvals are the authorization,
not the executor's identity.

---

## Why this repo is interesting

- **The full unshielded money path, both directions.** `depositUnshielded`
  claims value the transaction must actually supply (wallet UTXOs in, change
  back); `execute` makes the contract pay the recipient a brand-new UTXO. Both
  are exercised end-to-end against a real node — this is the part most contract
  demos skip.
- **Chain truth, not local state.** Threshold, treasury balance, proposals,
  per-signer approval state — all read from the contract via view-circuit
  calls (`MidnightContract.read` / batched `readMany`, one state snapshot per
  refresh). A device that **Connects** to a Vault it didn't deploy sees exactly
  what the deployer sees, and approvals update live as other signers act.
- **Real multi-party governance on phones.** One biometric forges a passkey
  identity + embedded wallet per device; distinct devices are distinct signers.
  The on-chain e2e proves a true 2-of-3: three wallets, two distinct approvals,
  a permissionless execute.
- **A guided ceremony UI.** Every proposal is a card with a plain-words status
  (`Awaiting approvals` / `Ready to execute` / `✓ Completed`), the recipient as
  a real wallet address (flagged when it's yours), absolute approval counts,
  and exactly the one action that's valid next — a signer is never steered into
  a contract assert.

The contract composes
[OpenZeppelin Compact Contracts](https://github.com/OpenZeppelin/compact-contracts)'
multisig modules (`SignerManager` + `ProposalManager` + `UnshieldedTreasury`);
identity + wallet plumbing come from the
[Kuira SDK](https://github.com/kuiralabs/kuira-sdk-android).

---

## Run it

```bash
./gradlew :app:assembleDebug
```

> **Heads-up:** this app pins Kuira SDK `0.1.0-alpha05`, which is still rolling
> out to Maven Central — external builds resolve it once that release lands.

Prerequisites, once:

| What | Where |
|---|---|
| `applicationId` + `PASSKEY_RP_ID` + hosted `assetlinks.json` | [Bind your app to a passkey domain](https://kuiralabs.github.io/kuira-sdk-android/recipes/bind-your-app-to-a-passkey-domain/) |
| A localnet (`mn localnet up`) | node `0.22.5`+ — its Compact runtime `0.16.0` matches the compiled contract |

Then fund and go:

1. **Forge sigil** in the app → copy the wallet address.
2. `mn airdrop 1000 --wallet <addr> --network undeployed`
3. **Register dust** in the wallet panel (in-app — the CLI can't sign for the
   embedded wallet), wait ~30s.
4. **Deploy Vault** → **Deposit** → **Propose** → **Approve** → **Execute
   withdrawal**. Threshold 1 lets one device run the whole ceremony solo.

---

## The real thing: two devices, real approvals

1. **Device B** (co-signer): forge a sigil, fund it, copy **your signer key**
   from the deploy card → send it to A.
2. **Device A** (deployer): paste B's key as a co-signer, pick threshold 2,
   **Deploy** → **Copy vault address** → send it to B.
3. **Device B**: paste into **Connect to an existing Vault**. B now sees the
   live treasury and proposals, and approves as a signer — A watches the
   count tick to 2/2, then either device executes.

Identity caveat: two devices on the **same Google account** share a sigil
(same seed → the *same signer*) — real M-of-N needs devices on different
accounts.

---

## Under the hood

- **Contract** — `contract/src/Vault.compact`: ~160 lines of glue over the
  vendored OZ modules. Signer-gating is `ownPublicKey()` against the deployed
  signer set; the threshold check gates `execute`; asserts run client-side
  during proving, so an invalid action fails before anything is submitted.
- **Money** — a deposit attaches a fallible unshielded offer (wallet UTXOs in,
  change out) signed *after* proving; a withdrawal attaches an output-only
  offer naming the recipient the contract's claimed spend must match. The
  node's per-segment balance check keeps everyone honest.
- **Reads** — the app tracks nothing it can read: one batched `readMany` per
  refresh loads threshold, balance, membership, and every proposal (with
  per-signer approval state) from a single chain snapshot, and a ledger stream
  triggers refreshes when the contract's state changes.
- **Rebuilding the contract** — toolchain pins and the upgrade recipe live in
  [`contract/README.md`](contract/README.md).

---

## Tests

- **Contract simulator** (no chain): `cd contract && npm test` — signer,
  threshold, and proposal-lifecycle accounting.
- **On-chain e2e** (localnet + one emulator):
  ```bash
  VAULT_E2E_CLASS="com.kuiralabs.starter.counter.VaultDeployE2ETest#governance_full_flow_2of3" \
    ./fund-vault-e2e.sh
  ```
  Three fresh wallets run the full 2-of-3 ceremony, asserting every read —
  threshold, balance, proposal fields, absolute statuses, approvals — against
  chain truth, including the batched snapshot path the app ships with.

---

## Known limitations

| Gap | Today | Closes when |
|---|---|---|
| The **proposer** isn't shown | The on-chain `Proposal` struct doesn't record it | `Vault.compact` gains a proposer map (contract change + redeploy) |
| `unshieldedBalance*` builtins compare as u64 (upstream bug — [public repro](https://github.com/nel349/unshielded-balance-u64-repro)) | The vendored treasury guards via its own `_balances` map | The upstream fix lands |
| No in-app faucet | Funding is a terminal step | The SDK ships a localnet airdrop helper |

---

## Troubleshooting

- **Forge fails (`RP_ID_MISMATCH`, PRF errors, silent biometric dismissal)** —
  one cause: the rpId doesn't match a hosted `assetlinks.json` listing this
  app's package + signing fingerprint. Recheck the
  [binding walkthrough](https://kuiralabs.github.io/kuira-sdk-android/recipes/bind-your-app-to-a-passkey-domain/),
  then **uninstall + reinstall** (`adb install -r` doesn't refresh passkey
  state).
- **Deploy hangs at "Balancing"** — zero DUST. Register dust in-app, wait
  ~30s, retry.
- **Balances/approvals stale** — a tx takes a block (~3s localnet) and a fresh
  contract takes a beat to index; the app retries through it. Still stale
  after ~60s → `adb logcat | grep VaultViewModel`.
- **`Custom error: 171` after a localnet reset** — the fresh chain vs the
  wallet's cached dust/UTXO state. Clear app data and re-fund (automatic
  invalidation is a tracked SDK item).
- **Physical device** — `adb reverse` ports 9944/8088/6300. **minSdk** stays
  30. **`language version mismatch`** → see
  [`contract/README.md`](contract/README.md#when-the-compact-toolchain-bumps).

---

## Acknowledgements

The multisig modules the Vault composes come from
**[OpenZeppelin Compact Contracts](https://github.com/OpenZeppelin/compact-contracts)**
(MIT), vendored at `v0.3.0-alpha` under [`contract/oz/`](contract/oz/) with
their license and audit reports intact — see
[`contract/oz/UPSTREAM.md`](contract/oz/UPSTREAM.md) for the exact pin and the
local modifications. Thank you to the OpenZeppelin team for building the
Compact library this Vault stands on.

## License

Apache 2.0 — see [LICENSE](LICENSE). The vendored OpenZeppelin Compact
contracts under `contract/oz/` remain MIT — see
[`contract/oz/LICENSE`](contract/oz/LICENSE).

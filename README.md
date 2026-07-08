# Kuira Vault — Android

A multisig confidential treasury on Midnight, as an Android dApp. Sigil
identity + embedded wallet + an OpenZeppelin-composed **M-of-N Vault**
contract: deposit NIGHT into a shared treasury, propose a withdrawal,
collect approvals from distinct signer wallets, execute — with every
balance, proposal, and approval read live from chain, so a co-signer
device that connects to an existing Vault sees the same truth as the
deployer.

Built on the Kuira starter (identity + wallet plumbing unchanged); the
contract and UI are the Vault.

---

## What this gives you

- **Identity + Wallet** — the SDK's `PanelBar` in floating mode drops two
  draggable chips over your content: a **sigil** chip (one biometric forges a
  passkey-PRF DID + wallet seed) and a **wallet** chip (NIGHT + DUST balance,
  receive-QR, dust registration, the network picker, and settings). Tap a chip to
  expand its sheet; drag it to dock at a screen edge.
- **Contract** — `contract/src/Vault.compact`, composed from the vendored
  OpenZeppelin Compact multisig modules (`SignerManager` + `ProposalManager` +
  `UnshieldedTreasury`, `contract/oz/`). Five actions: permissionless
  `depositUnshielded`, signer-gated `proposeWithdrawal` / `approve` /
  `revokeApproval`, and threshold-gated permissionless `execute` (the approvals
  are the authorization, not the executor's identity). Deposits fund the
  contract with real wallet UTXOs; execute pays the recipient with a real UTXO —
  the full unshielded money path in both directions.
- **Multi-device multisig** — deploy a 3-signer Vault (threshold 1–3) with
  co-signer public keys, share the vault address, and a second device
  **Connects** to it: threshold, treasury balance, proposals, per-signer
  approval state, and signer membership are all read from chain via
  `MidnightContract.read()` (view-circuit calls — no local tracking). Approvals
  update live as other signers act.
- **Guided governance UI** — each proposal is a card with a plain-words status
  (`Awaiting approvals` / `Ready to execute` / `✓ Completed`), the recipient as
  a real wallet address (flagged when it's yours), absolute approval counts,
  and exactly the action that's valid next.

---

## Quick start

```bash
./gradlew :app:assembleDebug                # 5 to 7 minutes on a cold cache
```

Open in Android Studio. To actually run on a device, complete the four
**Before you run** items below.

The app consumes the Kuira SDK `0.1.0-alpha05` from **mavenLocal** —
publish it from the SDK repo first (`./gradlew publishToMavenLocal`).

---

## Before you run

Four prep steps before the Sigil-forge path will succeed on a device.
The first three are single-string edits; the fourth is a terminal
command:

| What | Where | Currently |
|---|---|---|
| `applicationId` + `namespace` | `app/build.gradle.kts` | `com.kuiralabs.starter.counter` |
| `PASSKEY_RP_ID` | `app/src/main/java/com/kuiralabs/starter/counter/di/PasskeyConfigModule.kt` | `"REPLACE_ME_WITH_YOUR_DOMAIN.example"` |
| `assetlinks.json` | hosted at `https://<your rpId>/.well-known/assetlinks.json` | not hosted |
| Wallet funding (localnet) | terminal — `mn` CLI | manual step, see below |

The `assetlinks.json` content must declare your app's signing
fingerprint. Full walkthrough — including release-keystore generation,
multi-fingerprint setup, and hosting on GitHub Pages / Vercel /
Cloudflare: [Bind your app to a passkey
domain](https://kuiralabs.github.io/kuira-sdk-android/recipes/bind-your-app-to-a-passkey-domain/).

---

## Funding the embedded wallet

The Sigil-forge gives you a brand-new wallet with zero NIGHT and zero
DUST. Compact contracts need DUST to pay tx fees, and the SDK won't
deploy until both are present.

**On localnet (`MidnightNetwork.UNDEPLOYED`):**

> **Node version matters.** The Vault contract is compiled with
> `compactc 0.31.1` against Compact runtime **0.16.0** — the runtime the
> `mn localnet` default node ships. On an older node the deploy fails
> with a runtime-version mismatch. Stick with the default and you're
> fine.

1. Open the app, tap **Forge sigil** in the panel.
2. After forge, copy the wallet address from `WalletStatusPanel`.
3. In a terminal, airdrop NIGHT to that address:
   ```bash
   mn airdrop 1000 --wallet <addr> --network undeployed
   ```
4. Back in the app, tap **Register dust** in the wallet panel to generate DUST
   from that NIGHT. Do this **in-app** — the CLI's `mn dust register` takes a
   *named* wallet from `mn wallet generate`, so it can't target the app's
   embedded wallet address.
5. Wait ~30 seconds for DUST to appear, then run the flow: **Deploy Vault** →
   **Deposit** → **Propose** → **Approve** → **Execute withdrawal**.

**On PREPROD:** use the public faucet (link via the wallet panel's
copy-address button) instead of `mn airdrop`, then tap **Register dust** in
the wallet panel — the same in-app step as localnet.

---

## Running the multisig across two devices

1. **Device B** (co-signer): forge a sigil, fund the wallet, then copy **your
   signer key** from the deploy card and send it to device A.
2. **Device A** (deployer): paste B's key as a co-signer, pick the threshold,
   **Deploy** — then **Copy vault address** and send it to B.
3. **Device B**: paste the address into **Connect to an existing Vault** — it
   reads the Vault's state from chain and can approve as a signer.

One identity caveat: two devices signed into the **same Google account** share
a sigil (same seed → same key → the *same signer*), so a real M-of-N needs
devices on different accounts.

---

## Project layout

```
contract/                                 ← the on-chain piece
  src/Vault.compact                         the multisig treasury (OZ-composed)
  src/managed/Vault/                        compiled artifacts (committed)
  oz/                                       vendored OpenZeppelin Compact modules
  test/                                     simulator unit tests (vitest)
  package.json                              pins compactc + runtime versions
  README.md                                 rebuild + verify recipe

app/                                      ← the Android app
  build.gradle.kts                          io.github.kuiralabs.contract Gradle plugin
  src/main/java/.../
    KuiraStarterApp.kt                      @HiltAndroidApp
    MainActivity.kt                         AppCompatActivity + Compose
    di/PasskeyConfigModule.kt               Passkey rpId binding
    data/VaultContract.kt                   MidnightContract wrapper (actions + chain reads)
    data/VaultStore.kt                      which Vault this device points at, per network
    ui/VaultScreen.kt                       floating PanelBar overlay + VaultCard
    ui/VaultCard.kt                         deploy / connect / deposit / propose / approve / execute
    ui/VaultViewModel.kt                    chain-truth state machine + live approval stream
    ui/VaultUiState.kt                      sealed interface + ProposalView
  src/androidTest/.../VaultDeployE2ETest.kt on-chain e2e (3 wallets, full 2-of-3 governance)

fund-vault-e2e.sh                         ← host harness: funds fresh e2e wallets via mn
```

---

## Pinned versions

| Layer | Version |
|---|---|
| Kuira SDK | `0.1.0-alpha05` (mavenLocal) |
| AGP | `8.13.2` |
| Kotlin | `2.3.20` |
| KSP | `2.3.6` |
| Hilt | `2.58` |
| Compose BOM | `2026.03.01` |
| JDK | `17` |
| `compactc` | `0.31.1` |
| Compact language pragma | `0.23.0` |
| `@midnight-ntwrk/compact-runtime` | `0.16.0` |

The Compact toolchain triple moves independently. See
[`contract/README.md`](contract/README.md) for the upgrade recipe.

---

## Known limitations today

| Gap | Workaround here | Closes when |
|---|---|---|
| **The proposer isn't shown.** The on-chain `Proposal` struct doesn't record who proposed. | The UI shows recipient + amount + approvals only. | `Vault.compact` gains a proposer map written in `proposeWithdrawal` (contract change + redeploy). |
| **`unshieldedBalance*` builtins compare as u64** (upstream Midnight toolchain bug — reported, with a public minimal repro at [nel349/unshielded-balance-u64-repro](https://github.com/nel349/unshielded-balance-u64-repro)). | The vendored `UnshieldedTreasury` guards via its own `_balances` map instead. | The upstream fix lands and the vendored module reverts to the builtins. |
| **No in-app airdrop / faucet button.** | Funding is a terminal step (`mn airdrop … --network undeployed`). | The SDK ships an in-app airdrop helper for localnet. |
| **`androidx.security:security-crypto` is deprecated by Google industry-wide.** | Used for `VaultStore`; compile-time warnings are expected. | Google's recommended replacement stabilises. |

---

## FAQ

**Q: Forge fails with `RP_ID_MISMATCH` / `PRF authentication failed` /
"credential creation failed" / silent biometric prompt dismissal.**
A: All four are surface symptoms of the same underlying problem —
the rpId you set in `PasskeyConfigModule.kt` does not match an
`assetlinks.json` that lists this app's package + signing-cert
fingerprint at `https://<rpId>/.well-known/assetlinks.json`.

Checklist:

1. `PASSKEY_RP_ID` in `di/PasskeyConfigModule.kt` is the domain you
   control.
2. `https://<rpId>/.well-known/assetlinks.json` returns HTTP 200 with
   `Content-Type: application/json` (no redirect, no auth wall, no
   stale CDN cache from a previous app's content).
3. The fingerprint in that file matches `./gradlew signingReport`
   output for the build you installed (debug vs release have
   different fingerprints — re-publish when you swap configs).
4. **Uninstall + reinstall the app** after the assetlinks file lands;
   `adb install -r` doesn't refresh passkey state on some devices.

**Q: Deploy hangs at "Balancing".**
A: The wallet has zero DUST. Tap **Register dust** in the wallet panel, then
wait ~30 seconds and retry deploy. (Register in-app, not via CLI — `mn dust
register` needs a named `mn wallet generate` wallet and can't target the app's
embedded address.)

**Q: The balance or approvals don't update after an action.**
A: The Vault re-reads chain state after every action and on each on-chain
change (the ledger stream). A tx takes one block to land (~3s localnet, ~6s
PREPROD), and a freshly-deployed contract takes a beat to be indexed — the app
retries through that window. If it's still stale after ~60s, check
`adb logcat | grep VaultViewModel` for read errors — the indexer URL may not
be reachable.

**Q: Sends fail with `Custom error: 171` after a localnet reset.**
A: A network reset gives a fresh chain, but the wallet's on-device dust/UTXO
caches still hold the dead chain's state. Clear the app's data (or wipe the
SDK's dust + UTXO stores) and re-fund. Automatic invalidation on chain reset
is a tracked SDK item.

**Q: `mn localnet up` fails on Windows with `spawnSync ... cmd.exe ETIMEDOUT`.**
A: A known `mn`-on-Windows issue. Start the stack directly instead:
```bash
docker compose -f ~/.midnight/localnet/compose.yml up -d
```

**Q: I'm on a physical device, or an x86_64 (Intel) emulator.**
A: Two setup notes:
- **Physical device:** forward the localnet ports to the phone —
  `adb reverse tcp:9944 tcp:9944`, `adb reverse tcp:8088 tcp:8088`,
  `adb reverse tcp:6300 tcp:6300`.
- **x86_64 emulator:** works on SDK `alpha04+` (the native lib ships an x86_64
  `.so`).

**Q: Build fails with `Manifest merger failed: minSdkVersion 28 cannot
be smaller than version 30`.**
A: The SDK requires minSdk 30 (Block Store + CredentialManager). Don't
downgrade it.

**Q: Build fails with `language version X.Y.Z mismatch`.**
A: You upgraded `compactc` or edited `pragma language_version` without
matching the other. See [`contract/README.md` § When the Compact toolchain
bumps](contract/README.md#when-the-compact-toolchain-bumps).

**Q: Sigil restore on a fresh device doesn't see the previous wallet.**
A: Block Store binds the backup to the Google Play Services account on
the device. If the second device is signed into a different Google
account, it will see `SigilStatus.None`, not `BackupAvailable`. Sign
into the same account or forge a new sigil on the second device.

---

## Tests

- **Contract simulator** (no chain): `cd contract && npm test` — the multisig
  accounting (signers, thresholds, proposal lifecycle) against the OZ
  dry-run simulator.
- **On-chain e2e** (localnet + one emulator):
  ```bash
  VAULT_E2E_CLASS="com.kuiralabs.starter.counter.VaultDeployE2ETest#governance_full_flow_2of3" \
    ./fund-vault-e2e.sh
  ```
  Three fresh wallets deploy a 2-of-3 Vault, deposit, propose, approve to
  threshold, and execute the withdrawal — asserting every read (threshold,
  balance, proposal fields, absolute statuses, approvals) against chain truth.

---

## Acknowledgements

The multisig modules the Vault composes (`SignerManager`, `ProposalManager`,
`UnshieldedTreasury`) come from
**[OpenZeppelin Compact Contracts](https://github.com/OpenZeppelin/compact-contracts)**
(MIT), vendored at `v0.3.0-alpha` under [`contract/oz/`](contract/oz/) with
their license and audit reports intact — see
[`contract/oz/UPSTREAM.md`](contract/oz/UPSTREAM.md) for the exact pin and the
local modifications (notably the `unshieldedBalance*` u64 workaround). Thank
you to the OpenZeppelin team for building the Compact library this Vault
stands on.

---

## License

Apache 2.0 — see [LICENSE](LICENSE). The vendored OpenZeppelin Compact
contracts under `contract/oz/` remain MIT — see
[`contract/oz/LICENSE`](contract/oz/LICENSE).

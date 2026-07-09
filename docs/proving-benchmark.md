# Device proving benchmark

Measures what it costs to prove the Private Vault's **contract circuits on-device**, so we can
decide — with numbers, not guesses — whether the heavier Tier 2 / Tier 3 circuits will fit on a
real phone or whether they need a different proving strategy first.

## Why this exists

Two facts make on-device proving cost a real design constraint:

1. **Contract circuits always prove locally.** `MidnightConfig` wires the `LocalProofProvider`;
   `ProvingMode.REMOTE` only offloads *wallet* transactions, never contract calls. So every
   `deposit / propose / approve / execute` proof runs on the device's CPU and RAM.
2. **A private vault can't just offload to a remote prover.** Remote proving ships the *witness* —
   the circuit's private inputs — to a third-party proof server. For a selective-disclosure vault
   that would hand the secrets (recipient, amount, membership) to exactly the party the design
   keeps them from. So "prove it remotely if it's too heavy" is not free here; it trades the
   product's whole privacy claim.

Together these mean: if a circuit is too heavy to prove locally, the answer is a smarter proving
architecture (route by cost **and** privacy — tracked as SDK item #58), not a config flag. This
benchmark is the measurement that tells us when that work becomes necessary.

## How it works

`app/src/androidTest/.../PrivateVaultProvingBenchmark.kt` runs the same on-chain ceremony as the
e2e (`create → deposit → propose → approve×2 → execute`) but wraps each proving call in:

- an **off-thread RSS sampler** polling `/proc/self/status` `VmRSS` every 40 ms, tracking the peak
  (proving is native/rayon work the JVM heap counters don't see, so resident set size is the right
  gauge of "how close to OOM");
- a **Proving-stage timer** — the SDK emits `ContractCallStage.Proving` then `Balancing`, so the
  window between them isolates proof time from chain I/O.

It logs a table to logcat (`PrivateVaultBench`) and writes it to
`<externalFiles>/proving-benchmark.txt`. Like the e2e it rides the funding harness and **skips**
(never fails) when localnet/funding is absent.

### Running it

Localnet up (node 9944, indexer 8088, proof server 6300), one emulator, then:

```
ANDROID_SERIAL=emulator-5554 \
VAULT_E2E_CLASS=com.kuiralabs.vault.PrivateVaultProvingBenchmark \
./fund-vault-e2e.sh
```

The harness services the device's `KUIRA_FUND_REQ` funding markers via `mn airdrop`.

### Reading the numbers

- **proving(ms)** is the number that matters for circuit cost — the rest of the wall time is chain
  round-trips (fetch state, balance, submit, finalize).
- **peakRSS(MB)** is process-wide and the native allocator retains pages between circuits, so a
  later row's peak includes memory earlier ones kept. Read the **absolute peak** (especially
  `pvExecute`, the heaviest) as "how close did the app get to OOM," not as a clean per-circuit
  delta.

## Results

### 2026-07-09 — Tier 1, 2 GB emulator

Device: `totalRAM=1974MB`, `jvmHeapLimit=192MB`, 4 cores (prover uses cores−1). A deliberately
memory-constrained profile. Contract circuits LOCAL, wallet txs REMOTE.

| circuit | wall (ms) | proving (ms) | peak RSS (MB) |
|---|---:|---:|---:|
| deploy | 22134 | 5 | 391 |
| pvDepositUnshielded | 25112 | 212 | 397 |
| pvProposeWithdrawal | 22603 | 1126 | 461 |
| pvApprove #1 | 24100 | 1853 | 587 |
| pvApprove #2 | 17404 | 1532 | 583 |
| **pvExecute** (k=16, + unshielded withdrawal) | 24092 | **1906** | **607** |

Global peak RSS across the ceremony: **607 MB**.

**Conclusion:** for Tier 1, local proving is not a concern. Every circuit — including the k=16
`pvExecute` — proves in under 2 s with the whole app peaking at 607 MB on a 2 GB device, no OOM.
Proving is <8 % of each call's wall time; chain I/O dominates. `deploy`'s ~0 ms proving confirms
the constructor publishes no contract proof. **#58 adaptive proving is not required for Tier 1.**

**Caveat:** this is an x86 host-backed emulator; a real ARM phone proves slower (budget a few× on
wall time) and manages memory more tightly. Even several× the proving time stays in seconds, and
607 MB peak is transferable as an order-of-magnitude figure, but confirm on real hardware before
relying on a tight margin.

## What to do next time

The point of keeping the harness is that Tier 2 and Tier 3 add heavier circuits, and the plan
flags Tier 3's nullifier/membership proofs as the heaviest of all. **As each new circuit lands,
re-run this benchmark and add a dated section above.** Pull in #58 only when a measured circuit
actually blows the budget — not speculatively. Watch for:

- **peak RSS approaching the device budget** (a 2–3 GB phone gives a foreground app well over 1 GB
  before the low-memory killer, but the margin shrinks with heavier circuits);
- **proving time crossing single-digit seconds** on real hardware, where it stops being invisible
  behind chain I/O and starts needing a progress UX (or offload).

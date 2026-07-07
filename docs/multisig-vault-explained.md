# How the Multisig Vault Works — From Contract to UI

A teaching rundown of the whole implementation: the concept, the contract, the money
mechanics, the SDK, the app, and the UI — with the "why" at each layer.

---

## 1. The big idea: what a multisig actually is

**Analogy:** a company treasury with a rule — *"any large payment needs 2 of our 3 executives to
sign off."* No single person can move the money; but you don't need all three either. That "2 of 3"
is the **threshold** (M) and the number of executives is **N**. This is an **M-of-N multisig**.

On a blockchain there are no people signing paper — there are **keys**. Each "executive" is a
wallet, identified by its **public key**. "Signing off" = sending a transaction from that key. The
**contract** is the rulebook: it counts how many distinct authorized keys have approved a payment,
and only releases the money once the count reaches the threshold.

So a multisig is three things bundled: **an identity list** (who the signers are), **an
approval-counting rule** (need M of them), and **a vault holding the funds**. That maps exactly to
the three building blocks we composed.

---

## 2. The lifecycle (the "ceremony")

Every withdrawal goes through a fixed dance:

```
Deploy(signers = [A,B,C], threshold = 2)
   ↓
Deposit funds into the treasury
   ↓
A proposes a withdrawal  →  "Proposal #1: send 2 NIGHT to X"
   ↓
A approves #1   (1/2)
B approves #1   (2/2 — threshold reached)
   ↓
anyone executes #1  →  money leaves the vault to X
```

**Two points that surprise people:**

- The withdrawal is **not** executed by whoever proposed it. Proposing and approving are separate —
  a deliberate, reviewable process, like a purchase order that needs sign-offs.
- **Execute is permissionless.** Once 2 of 3 have approved, *anyone* can press the final button —
  even a stranger. Why is that safe? Because **the approvals are the authority, not the executor's
  identity.** The money can only go where the approved proposal said it goes.

---

## 3. Layer 1 — The contract (the rulebook)

Our contract, `Vault.compact`, is written in **Compact** (Midnight's smart-contract language) and is
composed from three **OpenZeppelin** modules — audited building blocks, so we didn't reinvent them:

| Module | Its job | Key state it keeps |
|---|---|---|
| **SignerManager** | Who is a signer + the threshold | the N signer keys, M |
| **ProposalManager** | The proposal lifecycle | `_proposals` (id → {recipient, token, amount, status}), `_nextProposalId` |
| **UnshieldedTreasury** | Holds and moves the actual tokens | `_balances` (token → amount) |

`Vault.compact` is the thin glue that wires them into the five actions the outside world calls:

- **`depositUnshielded(token, amount)`** → `Treasury._deposit` — pulls tokens in.
- **`proposeWithdrawal(recipient, token, amount)`** → checks the caller is a signer, then
  `Proposal.createProposal` → returns a new **id** (the first is `1`).
- **`approve(id)`** → checks the proposal is active, the caller is a signer, and hasn't already
  approved; then records the approval (bumps `_approvalCount[id]` and marks that signer in
  `_proposalApprovals[id]`).
- **`revokeApproval(id)`** → the inverse.
- **`execute(id)`** → checks `_approvalCount[id] ≥ threshold`, reads the proposal's
  recipient/token/amount, calls `Treasury._send`, and marks the proposal executed.

**Where the "rules" live:** they're `assert` statements inside the circuits — `assertSigner`,
`assertThresholdMet`, `!alreadyApproved`. If a rule is violated (a non-signer tries to approve, or
you try to execute below threshold), the transaction **fails before it's ever sent to the network**.
The rules are enforced at proving time on the caller's device, so a bad action never even reaches
the chain.

---

## 4. The identity model (who is a signer?)

Inside a circuit, `ownPublicKey()` returns **the caller's coin public key** — derived from their
wallet's seed. Our helper `getCaller()` wraps it, and the signer checks compare it against the
deployed signer list.

The single most important consequence:

> **1 wallet = 1 seed = 1 public key = 1 signer.**

So a *real* 2-of-3 needs **two different wallets** (ideally on two different devices/people) to
approve. One device physically cannot be two distinct signers — the contract even rejects the same
signer approving twice. "Multisig" implies "multiple parties," not just "multiple clicks."

---

## 5. Layer 2 — The money mechanics (the genuinely hard part)

This is where most of the engineering went, and it reveals how blockchain money actually moves.

**Unshielded tokens on Midnight work like Bitcoin: UTXOs.** Money isn't a balance in an account;
it's a set of discrete "coins" (unspent transaction outputs). A transaction consumes some coins
(inputs) and creates new ones (outputs), and **inputs must exactly balance outputs** (plus fees).
The network rejects any transaction that doesn't balance.

Now apply that to a contract vault:

### Depositing (money flows wallet → contract)

When you call `depositUnshielded(5 NIGHT)`, the contract *claims* 5 NIGHT via `receiveUnshielded`.
But claiming isn't providing — the transaction still has to **supply** those 5 NIGHT from your
wallet's coins. So the SDK attaches an **unshielded offer**:

- **inputs** = your wallet coins (say, one 10-NIGHT coin),
- **output** = 3-NIGHT change back to you (if you overshoot),
- and the contract absorbs the 5.
- Net: `10 in − 3 change − 5 to contract = 0`. Balanced.

If we *don't* attach that offer, the contract's side goes to −5 and the network rejects it (that was
**error 138**). And the input coins must be **signed by your key** — but *after* the transaction is
proven, because proving rewrites the bytes you sign over (that was **error 175**). These two errors
show the invisible rules a chain enforces.

### Withdrawing (money flows contract → recipient)

The mirror image. When `execute` calls `sendUnshielded`, the contract *provides* the value, so the
offer needs:

- **no inputs** (the contract has the money),
- **one output** = the recipient's new coin,
- **no signature** (nothing of yours is being spent).

Simpler than the deposit — and it worked on the first try because we'd already learned the rules
from the deposit.

**One honest edge:** the treasury's overflow guard uses a Midnight built-in (`unshieldedBalance*`)
that has an upstream bug — it accepts a 128-bit number but compares as 64-bit. We worked around it
in our copy of the contract and filed a bug report with a minimal reproduction. A good real-world
lesson: sometimes the platform itself has bugs; you isolate, work around, and report them.

---

## 6. Layer 3 — The SDK (turning a "call" into a finalized transaction)

When the app calls `MidnightContract.call("execute", id, withdrawalOffer)`, the SDK runs a pipeline:

1. **Fetch** the contract's current on-chain state.
2. **Execute** the circuit in an embedded JavaScript engine (QuickJS) — this runs the `assert` rules
   and produces a *transcript* of what the circuit did.
3. **Prove** it locally — generate the zero-knowledge proof (Midnight is a privacy chain; the proof
   shows the circuit ran correctly without revealing private inputs).
4. **Attach + sign** the money offer (deposit: sign after proving; withdrawal: no signing).
5. **Balance** — add the tiny "dust" fee that pays for the transaction, and seal it.
6. **Submit** and **wait for finalization** on the network.

Two SDK fixes were needed specifically for the multisig:

- **`ownPublicKey` identity:** the caller's key was packaged in the wrong shape, so `getCaller()`
  returned garbage and every signer check broke. The counter demo never used identity, so this bug
  had been hiding. *Access-control features exercise code paths simpler apps never touch.*
- **Enum arguments:** the withdrawal recipient has a `kind` field (an enum). Compact enums must be
  passed as plain numbers, but the SDK was sending them as big-integers. *Type systems at language
  boundaries are fussy.*

---

## 7. Layer 4 — The app "backend" + the read model

Three app files sit between the UI and the SDK:

- **`VaultContract`** — a thin wrapper that turns each action into an SDK call, marshals the
  arguments (the `Recipient` struct, the enum, the 32-byte addresses), and builds the
  deposit/withdrawal offers.
- **`VaultViewModel`** — the brain. A state machine with three states: **NotReady** (no wallet yet),
  **ReadyToDeploy** (wallet funded, no vault), **Deployed** (show the vault). It runs the actions
  and streams a live "call stage" so the UI can show progress.
- **`VaultStore`** — encrypted local storage for what the app tracks.

**The read model is the key teaching point here**, because it exposes a real limitation:

- The SDK can **read** on-chain ledger *fields* (via `observeLedger`), but it **cannot call a "view"
  function** to get a computed answer — its call path only *submits* transactions and throws the
  return value away.
- Only one governance field, **`_approvalCount`**, is exposed at the top level of the contract's
  state. The rest (treasury balance, proposal details) lives *inside* the composed modules, out of
  read reach.

So we split it:

- **Read live from chain:** `_approvalCount[id]` — the "X of N approved" number. This is exactly the
  value that changes when *someone else's* wallet approves, so it *must* come from the chain.
- **Track locally:** the threshold, the signer list, each proposal's recipient/amount, and total
  deposits (treasury balance = deposits − withdrawals). The app knows all of these because *it*
  performed those actions.

*In one line:* "We read from chain the one thing we can't know ourselves — other people's approvals
— and remember the rest ourselves. A cleaner design would add a 'view-call' path to the SDK; that's
a noted follow-up."

---

## 8. Layer 5 — The UI

`VaultScreen` hosts a single `VaultCard` (plus the floating wallet/sigil pills and the network
selector, reused from the SDK's wallet kit). The card renders whichever state the ViewModel is in:

- **Deploy card:** a threshold picker (1 / 2 / 3), two optional co-signer key fields, and a "Deploy
  N-of-3 Vault" button.
- **Treasury card:** the balance, plus a deposit field + button.
- **Proposals:** a "new proposal" form (recipient + amount), and a list of proposals each showing
  **"X/N approved"** with **Approve** and **Execute** buttons (Execute stays locked until the
  threshold is met).
- During any call, a staged progress bar (execute → prove → balance → submit) instead of a bare
  spinner.

---

## 9. Follow one withdrawal through the whole stack (the payoff)

A single "send 2 NIGHT" from tap to settled:

1. **UI:** user taps **Execute** on proposal #1.
2. **ViewModel:** `execute(1)` — looks up the proposal's recipient + amount from local store, builds
   the **withdrawal offer** (one output to the recipient) via the SDK, calls `VaultContract.execute`.
3. **VaultContract → SDK:** `MidnightContract.call("execute", 1, withdrawalOffer)`.
4. **SDK:** fetches state → runs the `execute` circuit in QuickJS. Inside the circuit,
   `assertThresholdMet(2)` passes (because `_approvalCount[1]` = 2), `Treasury._send` fires a
   `sendUnshielded` effect.
5. **SDK:** proves it locally, attaches the recipient-output offer, adds the dust fee, submits.
6. **Chain:** checks the transaction balances (the offer's output exactly matches the contract's
   `sendUnshielded`), verifies the proof, and finalizes.
7. **Result:** a new 2-NIGHT coin exists for the recipient; the proposal is marked executed; the app
   decrements the tracked treasury balance and grays out the row.

Every one of those steps is real and device-verified.

---

## 10. The honest edges (good for Q&A)

- **Single device = single signer.** The on-device test proved true 2-of-3 by spinning up **three
  separate wallets** and funding each; the UI on one phone can do the full flow at threshold 1, and
  *show* the multisig state (1/2) at higher thresholds while it waits for other devices.
- **The treasury balance isn't read from chain yet** (the view-call gap) — it's tracked locally. A
  named follow-up.
- **We found and reported a genuine platform bug** (the u128/u64 comparison), with a minimal
  reproduction — the kind of thing that happens when you push a real dApp onto a young chain.

---

## One-sentence summary

*A multisig vault is a contract that holds money and only releases it when M of N registered
key-holders have separately approved a specific payment — and making that work on a UTXO chain means
every deposit and withdrawal has to be balanced coin-for-coin and signed at exactly the right
moment.*

# Private Vault — selective-disclosure multisig treasury

This design lives NEXT TO the current (public-ledger) Vault: a sibling contract and an
alternative UI in this same repo, so the two can be compared side by side — same
governance, opposite disclosure philosophy.

**Goal:** evolve the Vault from a fully-public multisig into Midnight's flagship privacy
demonstration: outsiders learn nothing meaningful, signers govern with full context, a
recipient sees only their own payment, and an auditor can be granted a complete view —
each party's visibility a deliberate act of disclosure, not an accident of the ledger.

**Why this matters:** today every signer identity, every proposal (recipient + amount),
every approval vote, and the treasury balance are world-readable — our own
Connect-to-Vault feature reads them with nothing but the contract address. That is a
bank statement plus meeting minutes, published. The point of Midnight is that this is a
*choice*, and the Vault should demonstrate choosing well.

---

## Decisions

1. **Threshold: committed (hidden).** The governance rule is private. Execution proves
   "approvals ≥ threshold" against the commitment without revealing the number.
   *Honest caveat:* every successful execution publicly proves `threshold ≤ N` for that
   proposal's public count N, so over many executions outsiders learn an upper bound.
   Hiding it still denies the exact rule and denies it entirely for a young vault.
2. **Approvals: count public, identity hidden — from everyone, co-signers included.**
   Anyone can watch a proposal's approval count tick 0 → 1 → 2; nobody (not even a
   fellow signer) learns *who* approved. Accountability is collective, not individual.
   The strict form of this requires anonymous-membership approvals (Tier 3); Tier 1
   delivers it against outsiders first (see Tiers, and the interim-honesty note there).
3. **Auditor: opt-in, sees everything necessary, default rules now, policy engine later.**
   Default rule set:
   - Granting an auditor is itself a governance act — proposed and threshold-approved
     like a withdrawal, so no single signer can unilaterally open the books.
   - The auditor receives the vault's full read view: signer set, threshold, all
     proposals with contents, approval counts, treasury balance and movement history.
     (Approval *identities* stay hidden even from the auditor under decision 2 —
     "everything necessary" is the books, not the ballots.)
   - The auditor can act on nothing — a pure read grant.
   - Revocation is forward-only: rotate the viewing material so future activity is
     dark to the ex-auditor. What was already seen cannot be unseen; the plan never
     pretends otherwise.
4. **Disclosure transport: one-time invite bootstrap, then on-chain encrypted payloads.**
   (Spelled out below — it's the piece that makes the whole design elegant instead
   of clunky.)

---

## The transport question, explained (decision 4)

Hidden data still has to reach the people entitled to see it. A proposal's recipient
and amount live on-chain only as a commitment (a fingerprint); the actual values — the
*preimage* — must travel to co-signers somehow, or nobody can review what they're
approving. Three ways to move preimages:

- **Manual sharing** (copy/QR every proposal to every signer): honest but clunky —
  N-signer vaults turn every proposal into a group-chat chore. Fine for a demo, not
  impressive.
- **Off-chain service**: a server relaying encrypted payloads. Efficient but it
  reintroduces exactly the trusted middleman a blockchain demo should not need.
- **On-chain encrypted payloads** (chosen): each proposal carries, next to its public
  commitment, a small ciphertext of its own preimage encrypted under a **vault viewing
  key** — a symmetric key shared by the vault's members. Any signer's app decrypts
  proposals straight from chain state; a joining device needs nothing but the vault
  address and the key. The chain itself is the messaging channel; outsiders see only
  ciphertext.

The viewing key itself is distributed exactly once per member, at membership time: the
**vault invite** (a QR/share gesture, the same UX as today's signer-key exchange)
carries it. One manual moment at the door, everything after flows through the chain.
The auditor grant is the same mechanism — the grant proposal, once approved, results in
the auditor receiving the viewing material. Revocation = rotate the key via a
governance act; subsequent payloads use the new key.

*Balance achieved:* one symmetric key (no per-signer ciphertext blow-up that would leak
the signer count), no external infrastructure, self-contained on-chain UX, one
unavoidable manual step confined to the invite. Impressive, elegant, efficient — in
that order of visibility.

---

## Target disclosure matrix

| Data | Outsider | Co-signer | Recipient | Auditor (granted) |
|---|---|---|---|---|
| Vault exists / is active | sees (unavoidable) | full | — | full |
| Signer identities | hidden | full | — | full |
| Signer count | hidden (bounded inference only) | full | — | full |
| Threshold | hidden (≤-bound leaks per execution) | full | — | full |
| Treasury balance | hidden | full | — | full |
| Proposal recipient + amount | hidden | full | own payment only | full |
| Approval count per proposal | visible | visible | — | visible |
| Who approved | hidden | **hidden** | — | **hidden** |

**Residual leakage stated plainly:** transaction timing and volume, DUST fees
(unshielded by protocol design), approval-count dynamics, and the executed-threshold
upper bound. The matrix above is the **target** end-state: in shipped **Tier 1** the
treasury balance (and deposit/withdraw flows) are still **public** — money stays
unshielded until Tier 2. This is strong confidentiality, not invisibility, and the demo
should say so — credibility is part of the demonstration.

---

## Tiers — each independently shippable, each with an adversarial gate

### Tier 1 — Hide the vault from the world
Signer set, threshold, and proposal contents become commitments; proposal preimages
ride on-chain encrypted under the vault viewing key; approvals are recorded under
per-signer pseudonymous tags rather than raw public keys. Money stays unshielded — the
validated deposit/withdraw paths untouched. The invite/join flow replaces
Connect-to-Vault for members (address + viewing key instead of address alone).

*Interim honesty:* in Tier 1, co-signers (who hold the signer-set preimages) can still
correlate which pseudonymous tag is whom. Decision 2's strict "hidden from co-signers
too" lands in Tier 3; outsiders are fully blind from Tier 1 on.

*Delivers:* the disclosure UX (invite, join, decrypt-and-review), which is the actual
product of this milestone.

### Tier 2 — Hide the money
Swap the unshielded treasury for the vendored OZ shielded treasury (already in
`contract/oz` at v0.3.0-alpha: shielded deposits merge into one hidden coin per token
color; withdrawals leave as shielded notes to the recipient). The recipient's app sees
its own incoming note; nobody else sees amount or destination.

*Decision inside this tier:* OZ's shielded treasury keeps public cumulative
received/sent totals per color as an audit hook — lift them into commitments (auditor
reads via viewing material) to match the matrix above.

*This is the SDK lift:* the contract money paths speak unshielded offers today;
contract-held shielded coins are new SDK capability. The on-device engine already runs
the OZ shielded ops, so the work is the wallet-side path, not the engine.

### Tier 3 — Hide the hand that signs
Approve becomes an anonymous membership proof: the circuit proves "I am one of the
committed signers and I have not approved this proposal before" via a one-time
nullifier, and the ledger stores only the nullifier and the count. Execution proves
count ≥ committed threshold. Decision 2 lands in full: identities hidden from
co-signers and auditor alike, with double-approval still impossible.

*Hardest circuits; done last, on top of two working tiers.*

---

## What we will test (the adversarial gates)

Every tier ships with two e2e suites on localnet, in one spirit — a privacy claim
without a failing attacker is just a hope:

**Function (the ceremony still works):** deposit → propose → decrypt-and-review on a
second signer device → approve to threshold → execute → recipient paid. Multi-wallet,
real chain, absolute-value assertions — the same bar as the current 2-of-3 e2e.

**Privacy (the attacker fails):** a fresh outsider wallet armed with our own read
tooling — the strongest public reader we have — attacks the deployed vault and the
suite asserts it CANNOT recover:
- any signer identity or the signer count (Tier 1+)
- any proposal's recipient or amount from state or ciphertext (Tier 1+)
- the threshold (beyond the documented ≤-bound after execution) (Tier 1+)
- the treasury balance or a withdrawal's amount/destination (Tier 2+)
- which signer produced any given approval — attempted this time from a *co-signer's*
  full-knowledge position, not just an outsider's (Tier 3)
Plus the negative controls: a holder of the viewing key CAN decrypt and verify every
commitment; a revoked (rotated-out) auditor CANNOT read post-rotation activity.

---

## Open questions / risks (tracked, not blockers)

- **OZ maturity:** the shielded/multisig modules are v0.3.0-alpha; treat surprises as
  upstream-reportable (as with the unshieldedBalance u64 report) and check the shielded builtins for
  analogous width bugs before Tier 2 relies on them.
- **Proving cost on device:** membership/nullifier circuits (Tier 3) are the heaviest;
  budget a device-proving benchmark before committing to circuit shape. Harness +
  Tier 1 baseline: `docs/proving-benchmark.md` (Tier 1 proves <2s / 607MB peak on a
  2GB device — fine; re-run per new circuit as Tier 2/3 land).
- **Key hygiene:** the vault viewing key is a long-lived secret on every member device —
  it belongs in the same protected storage tier as wallet material, and rotation must
  be a first-class flow, not an afterthought.
- **Read-model shift:** members read the vault by decrypting state, not by public view
  circuits — the app's chain-truth layer gains a "verify preimage against commitment"
  discipline. Design this once, in Tier 1, and Tiers 2–3 inherit it.
- **Later policy engine (auditor):** the default rules above are deliberately simple;
  scoped/expiring audit grants and per-data-class policies are a follow-on milestone.

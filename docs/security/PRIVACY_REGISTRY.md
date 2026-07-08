# Privacy vulnerability registry

Every privacy claim the Private Vault makes is backed by a **demonstrated,
dead exploit** — not an assertion. For each entry below, `contract/test/security.test.ts`
runs the same attack twice: against a deliberately naive build where it
**succeeds**, and against `PrivateVault.compact` where it **fails**. A green
`npm test` means each row of the disclosure matrix in
[`../private-vault-plan.md`](../private-vault-plan.md) is defended by a real,
reproduced attack that our design kills.

The naive builds are real compiled contracts, never mocks:
- **VULN-01** targets `Vault.compact` — the public sibling, which genuinely
  leaks identity. It is the baseline the private variant improves on.
- **VULN-02/03/04** target `PrivateVaultVulnerable.compact` — a build that makes
  the "obvious" mistake a developer following the OpenZeppelin ShieldedMultiSig
  presets would make. **Never deployed.**

| VULN | Attack | Naive build does | PrivateVault does | Matrix row defended |
|---|---|---|---|---|
| **01** | Confirm a suspected signer identity | Exports `isSigner(pubkey)` — a public membership oracle anyone can call | No key→membership query exists; membership is only proven by the caller presenting their OWN secret salt | Signer identities → hidden |
| **02** | Dictionary-test a suspected key against the roster | Stores a single **public** instance salt on-chain, so `H(guessKey, publicSalt)` is recomputable and matches the roster | Per-signer **secret** salt (invite material), never on-chain — no recomputation is possible | Signer identities → hidden |
| **03** | Read a proposal's recipient + amount | `disclose()`s recipient and amount into plaintext ledger maps | Stores only a commitment `H(recipient, color, amount, nonce, …)` plus an opaque ciphertext payload; no preimage on-chain | Proposal recipient + amount → hidden |
| **04** | Trace one signer's votes across proposals | Approval tag = `H(pubkey, id)` — no salt, so a known key's tag is recomputable and links approvals | Tag = `H(pubkey, salt, id)` — needs the signer's secret salt, so tags are neither recomputable nor linkable across proposals | Who approved → hidden |

## Honest residual leakage (not defended, by design)

Stated in the plan and repeated here so it is never mistaken for an oversight:

- **Tier 1 execution reveals recipient + amount** in the transaction's unshielded
  outputs — inherent to unshielded custody. Closed by Tier 2 (shielded treasury).
- **Approval counts are public** by product decision (governance transparency
  without identity).
- Each successful execution publicly proves `threshold ≤ that proposal's count`,
  an upper bound that tightens over many executions.
- Transaction **timing** and DUST fees are unshielded at the protocol level.

## Running it

```bash
cd contract && npm test
```

`security.test.ts` is part of the default suite; the naive builds are compiled
alongside the real contracts by `pretest`.

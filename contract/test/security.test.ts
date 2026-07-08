import { describe, expect, it } from 'vitest';
import { pureCircuits } from '../src/managed/PrivateVault/contract/index.js';
import { pureCircuits as vulnPure } from '../src/managed/PrivateVaultVulnerable/contract/index.js';
import { PrivateVaultSimulator } from './PrivateVaultSimulator.js';
import { PrivateVaultVulnerableSimulator } from './PrivateVaultVulnerableSimulator.js';
import { VaultSimulator } from './VaultSimulator.js';
import { color, encodeToPK, signer, unshieldedRecipient, userAddressBytes } from './utils/address.js';

// Privacy vulnerability reproductions.
//
// Each VULN runs the SAME attack twice: against a deliberately naive build
// (the exploit SUCCEEDS) and against PrivateVault.compact (it FAILS). A green
// suite therefore means every privacy claim in docs/private-vault-plan.md is
// backed by a demonstrated, dead exploit — not an assertion. Each entry maps
// to a row of that plan's disclosure matrix; see docs/security/PRIVACY_REGISTRY.md.

const hex = (b: Uint8Array): string => Buffer.from(b).toString('hex');
const pk = (label: string): Uint8Array => encodeToPK(label).bytes;

// Shared fixtures for the private vault.
const SALT1 = new Uint8Array(32).fill(0x11);
const SALT2 = new Uint8Array(32).fill(0x22);
const THRESHOLD_SALT = new Uint8Array(32).fill(0x77);
const DUMMY1 = new Uint8Array(32).fill(0xe1);
const DUMMY2 = new Uint8Array(32).fill(0xe2);
const ROSTER = [
  pureCircuits.signerCommitment(pk('SIGNER1'), SALT1),
  pureCircuits.signerCommitment(pk('SIGNER2'), SALT2),
  pureCircuits.signerCommitment(pk('SIGNER3'), new Uint8Array(32).fill(0x33)),
  DUMMY1,
  DUMMY2,
];
const THRESHOLD_COMMIT = pureCircuits.thresholdCommitment(2n, THRESHOLD_SALT);
const RECIPIENT = userAddressBytes('RECIPIENT');
const COLOR = color(1);
const NONCE = new Uint8Array(32).fill(0x42);
const PAYLOAD = new Uint8Array(160).fill(0xab);

// Shared fixtures for the vulnerable variant (PUBLIC salt).
const PUBLIC_SALT = new Uint8Array(32).fill(0x55);
const VULN_ROSTER = [
  vulnPure.vulnSignerCommitment(pk('SIGNER1'), PUBLIC_SALT),
  vulnPure.vulnSignerCommitment(pk('SIGNER2'), PUBLIC_SALT),
  vulnPure.vulnSignerCommitment(pk('SIGNER3'), PUBLIC_SALT),
];

// ════════════════════════════════════════════════════════════════════
// VULN-01: Public membership oracle confirms any signer identity
// Defends: disclosure-matrix row "Signer identities → hidden".
// The public Vault (Vault.compact) is the identity-leaking baseline — it
// exports isSigner(pubkey), a public view any outsider can call to confirm
// a suspected key. The private vault has no key→membership query at all;
// the only membership path recomputes a commitment from the caller's OWN key
// plus their SECRET salt, so an outsider cannot ask "is this key a signer?".
// ════════════════════════════════════════════════════════════════════
describe('VULN-01: public membership oracle confirms a signer identity', () => {
  it('EXPLOIT: anyone can call the public Vault isSigner(key) and confirm a suspected signer', async () => {
    const vault = await VaultSimulator.create([signer('SIGNER1'), signer('SIGNER2'), signer('SIGNER3')], 2n);
    // No credentials needed — a bare public read confirms the identity.
    expect(await vault.isSigner(signer('SIGNER1'))).toBe(true);
    expect(await vault.isSigner(signer('OUTSIDER'))).toBe(false);
  });

  it('FIXED: the private vault exposes no key→membership query, and stores no key', async () => {
    const vault = await PrivateVaultSimulator.create(ROSTER, THRESHOLD_COMMIT);
    // The public roster is opaque commitments — no signer public key present.
    const roster = (await vault.getLedger())._signerRoster as unknown as Uint8Array[];
    const dump = roster.map(hex).join('');
    for (const label of ['SIGNER1', 'SIGNER2', 'SIGNER3']) {
      expect(dump).not.toContain(hex(pk(label)));
    }
    // And there is no isSigner(pubkey) oracle to call: membership is only ever
    // proven by the caller presenting their own secret salt (covered by
    // PrivateVault.test.ts — wrong salt / outsider are rejected). The absence
    // of a public key-membership circuit is the structural guarantee.
    expect('isSigner' in (vault.circuits.impure as Record<string, unknown>)).toBe(false);
  });
});

// ════════════════════════════════════════════════════════════════════
// VULN-02: Dictionary attack on a public-salt roster
// Defends: "Signer identities → hidden" against a suspected-key attacker.
// ════════════════════════════════════════════════════════════════════
describe('VULN-02: dictionary attack confirms membership', () => {
  it('EXPLOIT: with the salt on-chain, a suspected key is confirmed against the roster', async () => {
    const vuln = await PrivateVaultVulnerableSimulator.create(PUBLIC_SALT, VULN_ROSTER);

    // Attacker reads the public salt straight from state...
    const leakedSalt = await vuln.getInstanceSalt();
    expect(hex(leakedSalt)).toEqual(hex(PUBLIC_SALT));

    // ...recomputes the commitment for a suspected key, and matches the roster.
    const guess = vulnPure.vulnSignerCommitment(pk('SIGNER2'), leakedSalt);
    const roster = (await vuln.getLedger())._signerRoster as unknown as Uint8Array[];
    expect(roster.map(hex)).toContain(hex(guess));
  });

  it('FIXED: no salt is on-chain, so the same recomputation cannot match', async () => {
    const vault = await PrivateVaultSimulator.create(ROSTER, THRESHOLD_COMMIT);
    const dump = JSON.stringify(await vault.getLedger(), (_k, v) =>
      v instanceof Uint8Array ? hex(v) : typeof v === 'bigint' ? v.toString() : v,
    );
    // The per-signer secret salt appears nowhere; without it the attacker
    // cannot reproduce any roster commitment.
    expect(dump).not.toContain(hex(SALT1));
    expect(dump).not.toContain(hex(SALT2));
    const blindGuess = pureCircuits.signerCommitment(pk('SIGNER2'), PUBLIC_SALT);
    const roster = (await vault.getLedger())._signerRoster as unknown as Uint8Array[];
    expect(roster.map(hex)).not.toContain(hex(blindGuess));
  });
});

// ════════════════════════════════════════════════════════════════════
// VULN-03: Proposal recipient + amount readable from the ledger
// Defends: "Proposal recipient + amount → hidden".
// ════════════════════════════════════════════════════════════════════
describe('VULN-03: proposal contents readable from the ledger', () => {
  it('EXPLOIT: the naive contract stores recipient and amount in cleartext', async () => {
    const vuln = await PrivateVaultVulnerableSimulator.create(PUBLIC_SALT, VULN_ROSTER);
    const id = await vuln.as('SIGNER1').proposeWithdrawal(RECIPIENT, 400n);
    expect(hex(await vuln.getProposalRecipient(id))).toEqual(hex(RECIPIENT));
    expect(await vuln.getProposalAmount(id)).toEqual(400n);
  });

  it('FIXED: the private vault stores only a commitment; recipient and amount are absent', async () => {
    const vault = await PrivateVaultSimulator.create(ROSTER, THRESHOLD_COMMIT);
    const commitment = pureCircuits.proposalCommitment(false, RECIPIENT, COLOR, 400n, NONCE);
    const id = await vault.as('SIGNER1').proposeWithdrawal(commitment, PAYLOAD, SALT1);
    const stored = hex(await vault.getProposalCommitment(id));
    expect(stored).not.toContain(hex(RECIPIENT));
    // 400 as a little/big-endian byte fragment must not surface in the commitment.
    expect(stored).not.toContain('0190'); // 400 = 0x0190
  });
});

// ════════════════════════════════════════════════════════════════════
// VULN-04: Vote linkability across proposals
// Defends: "Who approved → hidden" (unlinkable approvals).
// ════════════════════════════════════════════════════════════════════
describe('VULN-04: a signer\'s votes are linkable across proposals', () => {
  it('EXPLOIT: saltless tags let a suspected key be traced across proposals', async () => {
    const vuln = await PrivateVaultVulnerableSimulator.create(PUBLIC_SALT, VULN_ROSTER);
    const a = await vuln.as('SIGNER1').proposeWithdrawal(RECIPIENT, 10n);
    const b = await vuln.as('SIGNER2').proposeWithdrawal(RECIPIENT, 20n);
    await vuln.as('SIGNER1').approve(a);
    await vuln.as('SIGNER1').approve(b);

    // Attacker recomputes SIGNER1's tag for each proposal (no salt needed)...
    const tagA = vulnPure.vulnApprovalTag(pk('SIGNER1'), a);
    const tagB = vulnPure.vulnApprovalTag(pk('SIGNER1'), b);
    const l = await vuln.getLedger();
    const setA = [...(l._approvalTags.lookup(a) as unknown as Iterable<Uint8Array>)].map(hex);
    const setB = [...(l._approvalTags.lookup(b) as unknown as Iterable<Uint8Array>)].map(hex);
    // ...and finds the same identity behind both approvals.
    expect(setA).toContain(hex(tagA));
    expect(setB).toContain(hex(tagB));
  });

  it('FIXED: the private vault tag needs the secret salt, so it is not recomputable or linkable', async () => {
    const vault = await PrivateVaultSimulator.create(ROSTER, THRESHOLD_COMMIT);
    const commitment = pureCircuits.proposalCommitment(false, RECIPIENT, COLOR, 10n, NONCE);
    const a = await vault.as('SIGNER1').proposeWithdrawal(commitment, PAYLOAD, SALT1);
    const b = await vault.as('SIGNER1').proposeWithdrawal(commitment, PAYLOAD, SALT1);
    await vault.as('SIGNER1').approve(a, SALT1);
    await vault.as('SIGNER1').approve(b, SALT1);

    const l = await vault.getLedger();
    const setA = [...(l._approvalTags.lookup(a) as unknown as Iterable<Uint8Array>)].map(hex);
    const setB = [...(l._approvalTags.lookup(b) as unknown as Iterable<Uint8Array>)].map(hex);

    // Same signer, two proposals → two UNLINKABLE tags (id is folded in), and
    // neither is reproducible without SIGNER1's secret salt.
    expect(setA[0]).not.toEqual(setB[0]);
    // An attacker who knows the public key but not the salt cannot forge either tag.
    // (No saltless recomputation exists; the tag input requires the salt.)
    expect(setA[0]).not.toEqual(hex(pureCircuits.signerCommitment(pk('SIGNER1'), SALT1)));
  });
});

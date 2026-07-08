import { beforeEach, describe, expect, it } from 'vitest';
import { pureCircuits } from '../src/managed/PrivateVault/contract/index.js';
import { PrivateVaultSimulator } from './PrivateVaultSimulator.js';
import { color, encodeToPK, userAddressBytes } from './utils/address.js';

// PrivateProposalStatus enum order: Inactive=0, Active=1, Executed=2 (ABSOLUTE —
// the leading Inactive mirrors OZ's convention so default<> is never live).
const ACTIVE = 1;

// What this suite covers and why.
//
// GOVERNANCE + DISCLOSURE: membership gating via salted commitments, the
// committed threshold, proposal commitments + opaque payloads, unlinkable
// approval tags, and the ledger-level privacy assertions (nothing plaintext).
// All of it runs in the off-chain simulator.
//
// TREASURY CUSTODY (deposit credits, execute debits) calls the protocol-level
// `unshieldedBalance*` builtins the simulator does not model — exactly as in
// Vault.test.ts those paths are validated ON-CHAIN by the SDK e2e and the
// execute happy path is `it.skip`ped here so the deferral stays visible.

const COLOR = color(1);
const AMOUNT = 400n;
const NONCE = new Uint8Array(32).fill(0x42);
const RECIPIENT = userAddressBytes('RECIPIENT');
const PAYLOAD = new Uint8Array(160).fill(0xab); // opaque ciphertext stand-in (encryption is app-side)

// Per-signer SECRET salts (invite material in production).
const SALT1 = new Uint8Array(32).fill(0x11);
const SALT2 = new Uint8Array(32).fill(0x22);
const SALT3 = new Uint8Array(32).fill(0x33);
const WRONG_SALT = new Uint8Array(32).fill(0x99);

// Threshold 2-of-3, committed with its own secret salt.
const THRESHOLD = 2n;
const THRESHOLD_SALT = new Uint8Array(32).fill(0x77);

// Dummy roster padding: random-looking values with no known preimage.
const DUMMY1 = new Uint8Array(32).fill(0xe1);
const DUMMY2 = new Uint8Array(32).fill(0xe2);

const pk = (label: string): Uint8Array => encodeToPK(label).bytes;
const signerCommit = (label: string, salt: Uint8Array): Uint8Array =>
  pureCircuits.signerCommitment(pk(label), salt);

const ROSTER = [
  signerCommit('SIGNER1', SALT1),
  signerCommit('SIGNER2', SALT2),
  signerCommit('SIGNER3', SALT3),
  DUMMY1,
  DUMMY2,
];
const THRESHOLD_COMMIT = pureCircuits.thresholdCommitment(THRESHOLD, THRESHOLD_SALT);

const PROPOSAL_COMMIT = pureCircuits.proposalCommitment(false, RECIPIENT, COLOR, AMOUNT, NONCE);

const hex = (b: Uint8Array): string => Buffer.from(b).toString('hex');

let vault: PrivateVaultSimulator;

const deploy = async (): Promise<PrivateVaultSimulator> =>
  PrivateVaultSimulator.create(ROSTER, THRESHOLD_COMMIT);

const proposeAsSigner1 = async (): Promise<bigint> =>
  vault.as('SIGNER1').proposeWithdrawal(PROPOSAL_COMMIT, PAYLOAD, SALT1);

describe('PrivateVault — selective-disclosure multisig treasury', () => {
  describe('constructor', () => {
    it('deploys with a full-width committed roster and a committed threshold', async () => {
      vault = await deploy();
      expect(hex(await vault.getThresholdCommitment())).toEqual(hex(THRESHOLD_COMMIT));
      expect(await vault.getProposalCount()).toEqual(0n);
    });

    it('rejects a zero signer-commitment slot', async () => {
      const roster = [...ROSTER.slice(0, 4), new Uint8Array(32)];
      await expect(PrivateVaultSimulator.create(roster, THRESHOLD_COMMIT)).rejects.toThrow(
        /zero signer commitment/,
      );
    });

    it('rejects a zero threshold commitment', async () => {
      await expect(PrivateVaultSimulator.create(ROSTER, new Uint8Array(32))).rejects.toThrow(
        /zero threshold commitment/,
      );
    });
  });

  describe('proposeWithdrawal (membership-gated by secret salt)', () => {
    beforeEach(async () => {
      vault = await deploy();
    });

    it('accepts a roster member presenting their own salt', async () => {
      const id = await proposeAsSigner1();
      expect(id).toEqual(0n);
      expect(await vault.getProposalCount()).toEqual(1n);
      expect(await vault.getProposalStatus(id)).toEqual(ACTIVE);
    });

    it('stores the commitment and the opaque payload verbatim', async () => {
      const id = await proposeAsSigner1();
      expect(hex(await vault.getProposalCommitment(id))).toEqual(hex(PROPOSAL_COMMIT));
      expect(hex(await vault.getProposalPayload(id))).toEqual(hex(PAYLOAD));
    });

    it('rejects a non-member even with a stolen valid salt', async () => {
      await expect(
        vault.as('OUTSIDER').proposeWithdrawal(PROPOSAL_COMMIT, PAYLOAD, SALT1),
      ).rejects.toThrow(/not a signer/);
    });

    it('rejects a member presenting the wrong salt', async () => {
      await expect(
        vault.as('SIGNER1').proposeWithdrawal(PROPOSAL_COMMIT, PAYLOAD, WRONG_SALT),
      ).rejects.toThrow(/not a signer/);
    });

    it('rejects a zero proposal commitment', async () => {
      await expect(
        vault.as('SIGNER1').proposeWithdrawal(new Uint8Array(32), PAYLOAD, SALT1),
      ).rejects.toThrow(/zero proposal commitment/);
    });

    it('assigns sequential ids', async () => {
      await proposeAsSigner1();
      const second = await vault.as('SIGNER2').proposeWithdrawal(PROPOSAL_COMMIT, PAYLOAD, SALT2);
      expect(second).toEqual(1n);
    });
  });

  describe('approve / revoke (unlinkable tags, public count)', () => {
    let id: bigint;

    beforeEach(async () => {
      vault = await deploy();
      id = await proposeAsSigner1();
    });

    it('counts distinct signers', async () => {
      expect(await vault.getApprovalCount(id)).toEqual(0n);
      await vault.as('SIGNER1').approve(id, SALT1);
      expect(await vault.getApprovalCount(id)).toEqual(1n);
      await vault.as('SIGNER2').approve(id, SALT2);
      expect(await vault.getApprovalCount(id)).toEqual(2n);
    });

    it('blocks double-approval by the same signer', async () => {
      await vault.as('SIGNER1').approve(id, SALT1);
      await expect(vault.as('SIGNER1').approve(id, SALT1)).rejects.toThrow(/already approved/);
    });

    it('rejects a wrong salt on approve', async () => {
      await expect(vault.as('SIGNER1').approve(id, WRONG_SALT)).rejects.toThrow(/not a signer/);
    });

    it('rejects an unknown proposal id', async () => {
      await expect(vault.as('SIGNER1').approve(99n, SALT1)).rejects.toThrow(/not found/);
    });

    it('revoke removes the approval and re-approval works', async () => {
      await vault.as('SIGNER1').approve(id, SALT1);
      await vault.as('SIGNER1').revokeApproval(id, SALT1);
      expect(await vault.getApprovalCount(id)).toEqual(0n);
      await vault.as('SIGNER1').approve(id, SALT1);
      expect(await vault.getApprovalCount(id)).toEqual(1n);
    });

    it('rejects revoking an approval that was never given', async () => {
      await expect(vault.as('SIGNER2').revokeApproval(id, SALT2)).rejects.toThrow(/not approved/);
    });
  });

  describe('execute — knowledge + threshold gating', () => {
    let id: bigint;

    beforeEach(async () => {
      vault = await deploy();
      id = await proposeAsSigner1();
      await vault.as('SIGNER1').approve(id, SALT1);
      await vault.as('SIGNER2').approve(id, SALT2);
    });

    const executeWith = (
      overrides: Partial<{
        amount: bigint;
        threshold: bigint;
        thresholdSalt: Uint8Array;
      }> = {},
    ) =>
      vault.as('SIGNER3').execute(
        id,
        false,
        RECIPIENT,
        COLOR,
        overrides.amount ?? AMOUNT,
        NONCE,
        overrides.threshold ?? THRESHOLD,
        overrides.thresholdSalt ?? THRESHOLD_SALT,
      );

    it('rejects a preimage that does not match the proposal commitment', async () => {
      await expect(executeWith({ amount: AMOUNT + 1n })).rejects.toThrow(
        /preimage does not match proposal/,
      );
    });

    it('rejects a wrong threshold preimage', async () => {
      await expect(executeWith({ thresholdSalt: WRONG_SALT })).rejects.toThrow(
        /threshold preimage mismatch/,
      );
    });

    it('rejects when approvals are below the (correct) threshold', async () => {
      await vault.as('SIGNER1').revokeApproval(id, SALT1);
      await expect(executeWith()).rejects.toThrow(/approvals below threshold/);
    });

    it('rejects a zero threshold outright, before any commitment math', async () => {
      await expect(executeWith({ threshold: 0n })).rejects.toThrow(/threshold below one/);
    });

    // Custody (the actual unshielded send + balance movement) needs protocol
    // UTXO state the simulator does not model — validated on-chain by the
    // adversarial e2e (Tier 1d), exactly as Vault.test.ts defers its custody paths.
    it.skip('executes and pays the recipient (on-chain e2e — Tier 1d)', () => {});
  });

  describe('disclosure — what the public ledger actually contains', () => {
    beforeEach(async () => {
      vault = await deploy();
    });

    it('roster width is constant (real signer count hidden)', async () => {
      const l = await vault.getLedger();
      const roster = l._signerRoster as unknown as Uint8Array[];
      expect(roster.length).toEqual(5);
    });

    it('the ledger never contains a signer public key, salt, or the threshold salt', async () => {
      const l = await vault.getLedger();
      const rosterHex = (l._signerRoster as unknown as Uint8Array[]).map(hex).join('');
      const forbidden = [
        hex(pk('SIGNER1')), hex(pk('SIGNER2')), hex(pk('SIGNER3')),
        hex(SALT1), hex(SALT2), hex(SALT3), hex(THRESHOLD_SALT),
      ];
      for (const f of forbidden) {
        expect(rosterHex).not.toContain(f);
        expect(hex(await vault.getThresholdCommitment())).not.toContain(f);
      }
    });

    it('a stored proposal reveals neither recipient, amount, nor nonce', async () => {
      const id = await proposeAsSigner1();
      const commitment = hex(await vault.getProposalCommitment(id));
      expect(commitment).not.toContain(hex(RECIPIENT));
      expect(commitment).not.toContain(hex(NONCE));
      // The payload here is a stand-in; the invariant under test is that the
      // CONTRACT stores it verbatim as opaque bytes and derives nothing from it.
      expect(hex(await vault.getProposalPayload(id))).toEqual(hex(PAYLOAD));
    });

    it("one signer's approval tags on two proposals are unlinkable (different tags)", async () => {
      const a = await proposeAsSigner1();
      const b = await vault.as('SIGNER1').proposeWithdrawal(PROPOSAL_COMMIT, PAYLOAD, SALT1);
      await vault.as('SIGNER1').approve(a, SALT1);
      await vault.as('SIGNER1').approve(b, SALT1);

      const l = await vault.getLedger();
      const tagsA = [...(l._approvalTags.lookup(a) as unknown as Iterable<Uint8Array>)].map(hex);
      const tagsB = [...(l._approvalTags.lookup(b) as unknown as Iterable<Uint8Array>)].map(hex);
      expect(tagsA.length).toEqual(1);
      expect(tagsB.length).toEqual(1);
      expect(tagsA[0]).not.toEqual(tagsB[0]);
      // And neither tag is the signer's roster commitment — approving never
      // points back at a roster entry.
      expect(tagsA[0]).not.toEqual(hex(signerCommit('SIGNER1', SALT1)));
      expect(tagsB[0]).not.toEqual(hex(signerCommit('SIGNER1', SALT1)));
    });
  });
});

import { beforeEach, describe, expect, it } from 'vitest';
import { VaultSimulator } from './VaultSimulator.js';
import { color, RecipientKind, signer, unshieldedRecipient, userAddressBytes } from './utils/address.js';

// ProposalStatus enum order (ProposalManager): Inactive=0, Active=1, Executed=2, Cancelled=3
const ACTIVE = 1;

const THRESHOLD = 2n;
const COLOR = color(1);
const SIGNERS = [signer('SIGNER1'), signer('SIGNER2'), signer('SIGNER3')];
const RECIPIENT = unshieldedRecipient('RECIPIENT');

// What this suite covers and why.
//
// The Vault's M-of-N GOVERNANCE (signer registry, proposal lifecycle, approval
// accumulation, threshold gating) never touches protocol UTXO state, so it runs
// fully in the off-chain simulator — that's everything below.
//
// The TREASURY CUSTODY guards (UnshieldedTreasury's deposit/withdraw) call the
// `unshieldedBalance*` builtins, which query protocol-level UTXO balance the
// off-chain simulator does not model (it throws "failed to decode u64"). This is
// the same reason OpenZeppelin ships no UnshieldedTreasury simulator test. The
// deposit-credits / execute-debits / insufficient-balance paths are therefore
// validated ON-CHAIN via the SDK e2e (tasks #28/#30), and marked `it.skip` here
// so the deferral is visible rather than silently dropped.

let vault: VaultSimulator;

describe('Vault — unshielded multisig treasury', () => {
  describe('constructor', () => {
    it('registers the signer set and threshold', async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      expect(await vault.getSignerCount()).toEqual(3n);
      expect(await vault.getThreshold()).toEqual(THRESHOLD);
    });

    it('marks each configured signer as a signer', async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      for (const s of SIGNERS) {
        expect(await vault.isSigner(s)).toBe(true);
      }
    });

    it('does not mark a non-signer', async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      expect(await vault.isSigner(signer('OTHER'))).toBe(false);
    });

    it('rejects a zero threshold', async () => {
      await expect(VaultSimulator.create(SIGNERS, 0n)).rejects.toThrow(/threshold must be > 0/);
    });

    it('rejects a threshold above the signer count', async () => {
      await expect(VaultSimulator.create(SIGNERS, 4n)).rejects.toThrow(/threshold exceeds signer count/);
    });
  });

  describe('proposeWithdrawal', () => {
    beforeEach(async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
    });

    it('lets a signer open an active proposal', async () => {
      const id = await vault.as('SIGNER1').proposeWithdrawal(RECIPIENT, COLOR, 400n);
      expect(id).toEqual(1n);
      expect(await vault.getProposalStatus(id)).toEqual(ACTIVE);
    });

    it('assigns incrementing proposal ids', async () => {
      await vault.as('SIGNER1').proposeWithdrawal(RECIPIENT, COLOR, 400n);
      const second = await vault.as('SIGNER2').proposeWithdrawal(RECIPIENT, COLOR, 100n);
      expect(second).toEqual(2n);
    });

    it('rejects a non-signer proposer', async () => {
      await expect(
        vault.as('OTHER').proposeWithdrawal(RECIPIENT, COLOR, 400n),
      ).rejects.toThrow(/not a signer/);
    });

    it('rejects a zero amount', async () => {
      await expect(
        vault.as('SIGNER1').proposeWithdrawal(RECIPIENT, COLOR, 0n),
      ).rejects.toThrow(/zero amount/);
    });

    it('rejects a non-unshielded recipient kind', async () => {
      const shielded = { kind: RecipientKind.ShieldedUser, address: userAddressBytes('RECIPIENT') };
      await expect(
        vault.as('SIGNER1').proposeWithdrawal(shielded, COLOR, 400n),
      ).rejects.toThrow(/unshielded user or contract/);
    });
  });

  describe('approve', () => {
    beforeEach(async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      await vault.as('SIGNER1').proposeWithdrawal(RECIPIENT, COLOR, 400n);
    });

    it('records a signer approval', async () => {
      await vault.as('SIGNER1').approve(1n);
      expect(await vault.getApprovalCount(1n)).toEqual(1n);
      expect(await vault.isApprovedBySigner(1n, signer('SIGNER1'))).toBe(true);
    });

    it('accumulates approvals from distinct signers', async () => {
      await vault.as('SIGNER1').approve(1n);
      await vault.as('SIGNER2').approve(1n);
      expect(await vault.getApprovalCount(1n)).toEqual(2n);
    });

    it('rejects a non-signer approver', async () => {
      await expect(vault.as('OTHER').approve(1n)).rejects.toThrow(/not a signer/);
    });

    it('rejects a double approval by the same signer', async () => {
      await vault.as('SIGNER1').approve(1n);
      await expect(vault.as('SIGNER1').approve(1n)).rejects.toThrow(/already approved/);
    });

    it('rejects approving a non-existent proposal', async () => {
      await expect(vault.as('SIGNER1').approve(999n)).rejects.toThrow(/not active|not found/);
    });
  });

  describe('revokeApproval', () => {
    beforeEach(async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      await vault.as('SIGNER1').proposeWithdrawal(RECIPIENT, COLOR, 400n);
      await vault.as('SIGNER1').approve(1n);
    });

    it('decrements the approval count and clears the flag', async () => {
      await vault.as('SIGNER1').revokeApproval(1n);
      expect(await vault.getApprovalCount(1n)).toEqual(0n);
      expect(await vault.isApprovedBySigner(1n, signer('SIGNER1'))).toBe(false);
    });

    it('rejects revoking without a prior approval', async () => {
      await expect(vault.as('SIGNER2').revokeApproval(1n)).rejects.toThrow(/not approved/);
    });
  });

  describe('execute — authorization gating', () => {
    beforeEach(async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      await vault.as('SIGNER1').proposeWithdrawal(RECIPIENT, COLOR, 400n);
    });

    // The threshold check fires before any treasury call, so it is observable
    // off-chain without funding the vault.
    it('rejects execution below threshold', async () => {
      await vault.as('SIGNER1').approve(1n);
      await expect(vault.execute(1n)).rejects.toThrow(/threshold not met/);
    });

    it('rejects executing a non-existent proposal', async () => {
      await expect(vault.execute(999n)).rejects.toThrow(/not active|not found/);
    });
  });

  describe('views', () => {
    it('reports zero balance for an unfunded color', async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      expect(await vault.getUnshieldedBalance(color(9))).toEqual(0n);
    });

    it('reports zero approvals for a fresh proposal', async () => {
      vault = await VaultSimulator.create(SIGNERS, THRESHOLD);
      await vault.as('SIGNER1').proposeWithdrawal(RECIPIENT, COLOR, 400n);
      expect(await vault.getApprovalCount(1n)).toEqual(0n);
    });
  });

  // ── Treasury custody — validated on-chain (#28/#30), not in this off-chain
  //    simulator (the unshieldedBalance* builtins query protocol UTXO state the
  //    simulator cannot decode). Kept visible as skipped, not removed.
  describe('treasury custody (on-chain coverage — see SDK e2e #28/#30)', () => {
    it.skip('credits the treasury balance on deposit', () => {});
    it.skip('debits the treasury and marks executed on a threshold-met proposal', () => {});
    it.skip('rejects execution exceeding the treasury balance', () => {});
  });
});

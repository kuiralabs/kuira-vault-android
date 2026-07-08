// PrivateVault simulator — thin typed wrapper over the OZ compact-simulator.
// `.as(label)` (from the base) sets ownPublicKey() so membership-gated circuits
// can be driven as a specific signer; the signer's SECRET SALT is passed per
// call, exactly as the app will supply it from invite material.

import { createSimulator, type SimulatorOptions } from '@openzeppelin/compact-simulator';
import {
  type Ledger,
  ledger,
  Contract as PrivateVault,
} from '../src/managed/PrivateVault/contract/index.js';
import { EmptyPrivateState, emptyWitnesses } from './EmptyWitnesses.js';

type PrivateVaultArgs = readonly [signerCommitments: Uint8Array[], thresholdCommitment: Uint8Array];

const PrivateVaultSimulatorBase = createSimulator<
  EmptyPrivateState,
  ReturnType<typeof ledger>,
  ReturnType<typeof emptyWitnesses>,
  PrivateVault<EmptyPrivateState>,
  PrivateVaultArgs
>({
  contractFactory: (witnesses) => new PrivateVault<EmptyPrivateState>(witnesses),
  defaultPrivateState: () => EmptyPrivateState,
  contractArgs: (signerCommitments, thresholdCommitment) => [signerCommitments, thresholdCommitment],
  ledgerExtractor: (state) => ledger(state),
  witnessesFactory: () => emptyWitnesses(),
  artifactName: 'PrivateVault',
});

export class PrivateVaultSimulator extends PrivateVaultSimulatorBase {
  static async create(
    signerCommitments: Uint8Array[],
    thresholdCommitment: Uint8Array,
    options: SimulatorOptions<EmptyPrivateState, ReturnType<typeof emptyWitnesses>> = {},
  ): Promise<PrivateVaultSimulator> {
    // biome-ignore lint/complexity/noThisInStatic: super.create keeps the subclass `this`
    return super.create([signerCommitments, thresholdCommitment], options) as Promise<PrivateVaultSimulator>;
  }

  // Deposit (permissionless)
  public depositUnshielded(color: Uint8Array, amount: bigint): Promise<[]> {
    return this.circuits.impure.pvDepositUnshielded(color, amount);
  }

  // Proposal lifecycle
  public proposeWithdrawal(commitment: Uint8Array, payload: Uint8Array, salt: Uint8Array): Promise<bigint> {
    return this.circuits.impure.pvProposeWithdrawal(commitment, payload, salt);
  }
  public approve(id: bigint, salt: Uint8Array): Promise<[]> {
    return this.circuits.impure.pvApprove(id, salt);
  }
  public revokeApproval(id: bigint, salt: Uint8Array): Promise<[]> {
    return this.circuits.impure.pvRevokeApproval(id, salt);
  }
  public execute(
    id: bigint,
    recipientIsContract: boolean,
    recipient: Uint8Array,
    color: Uint8Array,
    amount: bigint,
    nonce: Uint8Array,
    threshold: bigint,
    thresholdSalt: Uint8Array,
  ): Promise<[]> {
    return this.circuits.impure.pvExecute(
      id, recipientIsContract, recipient, color, amount, nonce, threshold, thresholdSalt,
    );
  }

  // Views
  public getProposalCount(): Promise<bigint> {
    return this.circuits.impure.getProposalCount();
  }
  public getProposalCommitment(id: bigint): Promise<Uint8Array> {
    return this.circuits.impure.getProposalCommitment(id);
  }
  public getProposalPayload(id: bigint): Promise<Uint8Array> {
    return this.circuits.impure.getProposalPayload(id);
  }
  public getProposalStatus(id: bigint): Promise<number> {
    return this.circuits.impure.getProposalStatus(id);
  }
  public getApprovalCount(id: bigint): Promise<bigint> {
    return this.circuits.impure.getApprovalCount(id);
  }
  public getUnshieldedBalance(color: Uint8Array): Promise<bigint> {
    return this.circuits.impure.getUnshieldedBalance(color);
  }
  public getThresholdCommitment(): Promise<Uint8Array> {
    return this.circuits.impure.getThresholdCommitment();
  }

  public getLedger(): Promise<Ledger> {
    return this.getPublicState();
  }
}

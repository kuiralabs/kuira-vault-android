// Vault simulator — thin typed wrapper over the OZ compact-simulator, exposing
// the Vault's circuits. `.as(label)` (from the base) sets ownPublicKey() so
// signer-gated circuits can be driven as a specific signer.

import { createSimulator, type SimulatorOptions } from '@openzeppelin/compact-simulator';
import { type Ledger, ledger, Contract as Vault } from '../src/managed/Vault/contract/index.js';
import { EmptyPrivateState, emptyWitnesses } from './EmptyWitnesses.js';

type EitherPKAddress = {
  is_left: boolean;
  left: { bytes: Uint8Array };
  right: { bytes: Uint8Array };
};
type Recipient = { kind: number; address: Uint8Array };
type Proposal = { to: Recipient; color: Uint8Array; amount: bigint; status: number };
type VaultArgs = readonly [signers: EitherPKAddress[], thresh: bigint];

const VaultSimulatorBase = createSimulator<
  EmptyPrivateState,
  ReturnType<typeof ledger>,
  ReturnType<typeof emptyWitnesses>,
  Vault<EmptyPrivateState>,
  VaultArgs
>({
  contractFactory: (witnesses) => new Vault<EmptyPrivateState>(witnesses),
  defaultPrivateState: () => EmptyPrivateState,
  contractArgs: (signers, thresh) => [signers, thresh],
  ledgerExtractor: (state) => ledger(state),
  witnessesFactory: () => emptyWitnesses(),
  artifactName: 'Vault',
});

export class VaultSimulator extends VaultSimulatorBase {
  static async create(
    signers: EitherPKAddress[],
    thresh: bigint,
    options: SimulatorOptions<EmptyPrivateState, ReturnType<typeof emptyWitnesses>> = {},
  ): Promise<VaultSimulator> {
    // biome-ignore lint/complexity/noThisInStatic: super.create keeps the subclass `this`
    return super.create([signers, thresh], options) as Promise<VaultSimulator>;
  }

  // Deposit (permissionless)
  public depositUnshielded(color: Uint8Array, amount: bigint): Promise<[]> {
    return this.circuits.impure.depositUnshielded(color, amount);
  }

  // Proposal lifecycle
  public proposeWithdrawal(to: Recipient, color: Uint8Array, amount: bigint): Promise<bigint> {
    return this.circuits.impure.proposeWithdrawal(to, color, amount);
  }
  public approve(id: bigint): Promise<[]> {
    return this.circuits.impure.approve(id);
  }
  public revokeApproval(id: bigint): Promise<[]> {
    return this.circuits.impure.revokeApproval(id);
  }
  public execute(id: bigint): Promise<[]> {
    return this.circuits.impure.execute(id);
  }

  // Views
  public isApprovedBySigner(id: bigint, signer: EitherPKAddress): Promise<boolean> {
    return this.circuits.impure.isApprovedBySigner(id, signer);
  }
  public getApprovalCount(id: bigint): Promise<bigint> {
    return this.circuits.impure.getApprovalCount(id);
  }
  public getUnshieldedBalance(color: Uint8Array): Promise<bigint> {
    return this.circuits.impure.getUnshieldedBalance(color);
  }
  public getProposal(id: bigint): Promise<Proposal> {
    return this.circuits.impure.getProposal(id);
  }
  public getProposalStatus(id: bigint): Promise<number> {
    return this.circuits.impure.getProposalStatus(id);
  }
  public getSignerCount(): Promise<bigint> {
    return this.circuits.impure.getSignerCount();
  }
  public getThreshold(): Promise<bigint> {
    return this.circuits.impure.getThreshold();
  }
  public isSigner(account: EitherPKAddress): Promise<boolean> {
    return this.circuits.impure.isSigner(account);
  }

  public getLedger(): Promise<Ledger> {
    return this.getPublicState();
  }
}

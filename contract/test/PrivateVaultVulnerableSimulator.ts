// Simulator for the DELIBERATELY VULNERABLE PrivateVault variant. Exists only
// so the security suite can drive the naive contract and demonstrate each
// exploit landing. Never a shipping component.

import { createSimulator, type SimulatorOptions } from '@openzeppelin/compact-simulator';
import {
  type Ledger,
  ledger,
  Contract as PrivateVaultVulnerable,
} from '../src/managed/PrivateVaultVulnerable/contract/index.js';
import { EmptyPrivateState, emptyWitnesses } from './EmptyWitnesses.js';

type Args = readonly [instanceSalt: Uint8Array, signerRoster: Uint8Array[]];

const Base = createSimulator<
  EmptyPrivateState,
  ReturnType<typeof ledger>,
  ReturnType<typeof emptyWitnesses>,
  PrivateVaultVulnerable<EmptyPrivateState>,
  Args
>({
  contractFactory: (witnesses) => new PrivateVaultVulnerable<EmptyPrivateState>(witnesses),
  defaultPrivateState: () => EmptyPrivateState,
  contractArgs: (instanceSalt, signerRoster) => [instanceSalt, signerRoster],
  ledgerExtractor: (state) => ledger(state),
  witnessesFactory: () => emptyWitnesses(),
  artifactName: 'PrivateVaultVulnerable',
});

export class PrivateVaultVulnerableSimulator extends Base {
  static async create(
    instanceSalt: Uint8Array,
    signerRoster: Uint8Array[],
    options: SimulatorOptions<EmptyPrivateState, ReturnType<typeof emptyWitnesses>> = {},
  ): Promise<PrivateVaultVulnerableSimulator> {
    // biome-ignore lint/complexity/noThisInStatic: super.create keeps the subclass `this`
    return super.create([instanceSalt, signerRoster], options) as Promise<PrivateVaultVulnerableSimulator>;
  }

  public proposeWithdrawal(recipient: Uint8Array, amount: bigint): Promise<bigint> {
    return this.circuits.impure.proposeWithdrawal(recipient, amount);
  }
  public approve(id: bigint): Promise<[]> {
    return this.circuits.impure.approve(id);
  }
  public getInstanceSalt(): Promise<Uint8Array> {
    return this.circuits.impure.getInstanceSalt();
  }
  public getProposalRecipient(id: bigint): Promise<Uint8Array> {
    return this.circuits.impure.getProposalRecipient(id);
  }
  public getProposalAmount(id: bigint): Promise<bigint> {
    return this.circuits.impure.getProposalAmount(id);
  }
  public getLedger(): Promise<Ledger> {
    return this.getPublicState();
  }
}

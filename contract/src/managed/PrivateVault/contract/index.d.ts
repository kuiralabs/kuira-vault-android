import type * as __compactRuntime from '@midnight-ntwrk/compact-runtime';

export enum PrivateProposalStatus { Inactive = 0, Active = 1, Executed = 2 }

export type Witnesses<PS> = {
}

export type ImpureCircuits<PS> = {
  depositUnshielded(context: __compactRuntime.CircuitContext<PS>,
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    commitment_0: Uint8Array,
                    payload_0: Uint8Array,
                    salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>,
          id_0: bigint,
          salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  revokeApproval(context: __compactRuntime.CircuitContext<PS>,
                 id_0: bigint,
                 salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  execute(context: __compactRuntime.CircuitContext<PS>,
          id_0: bigint,
          recipientIsContract_0: boolean,
          recipient_0: Uint8Array,
          color_0: Uint8Array,
          amount_0: bigint,
          nonce_0: Uint8Array,
          threshold_0: bigint,
          thresholdSalt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  getProposalCount(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  getProposalCommitment(context: __compactRuntime.CircuitContext<PS>,
                        id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalPayload(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalStatus(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, PrivateProposalStatus>;
  getApprovalCount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  getUnshieldedBalance(context: __compactRuntime.CircuitContext<PS>,
                       color_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  getThresholdCommitment(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, Uint8Array>;
}

export type ProvableCircuits<PS> = {
  depositUnshielded(context: __compactRuntime.CircuitContext<PS>,
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    commitment_0: Uint8Array,
                    payload_0: Uint8Array,
                    salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>,
          id_0: bigint,
          salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  revokeApproval(context: __compactRuntime.CircuitContext<PS>,
                 id_0: bigint,
                 salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  execute(context: __compactRuntime.CircuitContext<PS>,
          id_0: bigint,
          recipientIsContract_0: boolean,
          recipient_0: Uint8Array,
          color_0: Uint8Array,
          amount_0: bigint,
          nonce_0: Uint8Array,
          threshold_0: bigint,
          thresholdSalt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  getProposalCount(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  getProposalCommitment(context: __compactRuntime.CircuitContext<PS>,
                        id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalPayload(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalStatus(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, PrivateProposalStatus>;
  getApprovalCount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  getUnshieldedBalance(context: __compactRuntime.CircuitContext<PS>,
                       color_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  getThresholdCommitment(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, Uint8Array>;
}

export type PureCircuits = {
  signerCommitment(pk_0: Uint8Array, salt_0: Uint8Array): Uint8Array;
  thresholdCommitment(threshold_0: bigint, salt_0: Uint8Array): Uint8Array;
  proposalCommitment(recipientIsContract_0: boolean,
                     recipient_0: Uint8Array,
                     color_0: Uint8Array,
                     amount_0: bigint,
                     nonce_0: Uint8Array): Uint8Array;
}

export type Circuits<PS> = {
  depositUnshielded(context: __compactRuntime.CircuitContext<PS>,
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    commitment_0: Uint8Array,
                    payload_0: Uint8Array,
                    salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>,
          id_0: bigint,
          salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  revokeApproval(context: __compactRuntime.CircuitContext<PS>,
                 id_0: bigint,
                 salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  execute(context: __compactRuntime.CircuitContext<PS>,
          id_0: bigint,
          recipientIsContract_0: boolean,
          recipient_0: Uint8Array,
          color_0: Uint8Array,
          amount_0: bigint,
          nonce_0: Uint8Array,
          threshold_0: bigint,
          thresholdSalt_0: Uint8Array): __compactRuntime.CircuitResults<PS, []>;
  signerCommitment(context: __compactRuntime.CircuitContext<PS>,
                   pk_0: Uint8Array,
                   salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, Uint8Array>;
  thresholdCommitment(context: __compactRuntime.CircuitContext<PS>,
                      threshold_0: bigint,
                      salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, Uint8Array>;
  proposalCommitment(context: __compactRuntime.CircuitContext<PS>,
                     recipientIsContract_0: boolean,
                     recipient_0: Uint8Array,
                     color_0: Uint8Array,
                     amount_0: bigint,
                     nonce_0: Uint8Array): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalCount(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  getProposalCommitment(context: __compactRuntime.CircuitContext<PS>,
                        id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalPayload(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalStatus(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, PrivateProposalStatus>;
  getApprovalCount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  getUnshieldedBalance(context: __compactRuntime.CircuitContext<PS>,
                       color_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  getThresholdCommitment(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, Uint8Array>;
}

export type Ledger = {
  readonly _signerRoster: Uint8Array[];
  readonly _thresholdCommitment: Uint8Array;
  readonly _nextProposalId: bigint;
  _proposalCommitment: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): Uint8Array;
    [Symbol.iterator](): Iterator<[bigint, Uint8Array]>
  };
  _proposalPayload: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): Uint8Array;
    [Symbol.iterator](): Iterator<[bigint, Uint8Array]>
  };
  _proposalStatus: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): PrivateProposalStatus;
    [Symbol.iterator](): Iterator<[bigint, PrivateProposalStatus]>
  };
  _approvalTags: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): {
      isEmpty(): boolean;
      size(): bigint;
      member(elem_0: Uint8Array): boolean;
      [Symbol.iterator](): Iterator<Uint8Array>
    }
  };
  _approvalCount: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): bigint;
    [Symbol.iterator](): Iterator<[bigint, bigint]>
  };
}

export type ContractReferenceLocations = any;

export declare const contractReferenceLocations : ContractReferenceLocations;

export declare class Contract<PS = any, W extends Witnesses<PS> = Witnesses<PS>> {
  witnesses: W;
  circuits: Circuits<PS>;
  impureCircuits: ImpureCircuits<PS>;
  provableCircuits: ProvableCircuits<PS>;
  constructor(witnesses: W);
  initialState(context: __compactRuntime.ConstructorContext<PS>,
               signerCommitments_0: Uint8Array[],
               thresholdCommitment_0: Uint8Array): __compactRuntime.ConstructorResult<PS>;
}

export declare function ledger(state: __compactRuntime.StateValue | __compactRuntime.ChargedState): Ledger;
export declare const pureCircuits: PureCircuits;

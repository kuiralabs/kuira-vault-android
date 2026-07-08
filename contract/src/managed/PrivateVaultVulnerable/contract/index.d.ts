import type * as __compactRuntime from '@midnight-ntwrk/compact-runtime';

export type Witnesses<PS> = {
}

export type ImpureCircuits<PS> = {
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    recipient_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  getInstanceSalt(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalRecipient(context: __compactRuntime.CircuitContext<PS>,
                       id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalAmount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
}

export type ProvableCircuits<PS> = {
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    recipient_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  getInstanceSalt(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalRecipient(context: __compactRuntime.CircuitContext<PS>,
                       id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalAmount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
}

export type PureCircuits = {
  vulnSignerCommitment(pk_0: Uint8Array, salt_0: Uint8Array): Uint8Array;
  vulnApprovalTag(pk_0: Uint8Array, id_0: bigint): Uint8Array;
}

export type Circuits<PS> = {
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    recipient_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  vulnSignerCommitment(context: __compactRuntime.CircuitContext<PS>,
                       pk_0: Uint8Array,
                       salt_0: Uint8Array): __compactRuntime.CircuitResults<PS, Uint8Array>;
  vulnApprovalTag(context: __compactRuntime.CircuitContext<PS>,
                  pk_0: Uint8Array,
                  id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getInstanceSalt(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalRecipient(context: __compactRuntime.CircuitContext<PS>,
                       id_0: bigint): __compactRuntime.CircuitResults<PS, Uint8Array>;
  getProposalAmount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
}

export type Ledger = {
  readonly _instanceSalt: Uint8Array;
  readonly _signerRoster: Uint8Array[];
  readonly _nextProposalId: bigint;
  _proposalRecipient: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): Uint8Array;
    [Symbol.iterator](): Iterator<[bigint, Uint8Array]>
  };
  _proposalAmount: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): bigint;
    [Symbol.iterator](): Iterator<[bigint, bigint]>
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
               instanceSalt_0: Uint8Array,
               signerRoster_0: Uint8Array[]): __compactRuntime.ConstructorResult<PS>;
}

export declare function ledger(state: __compactRuntime.StateValue | __compactRuntime.ChargedState): Ledger;
export declare const pureCircuits: PureCircuits;

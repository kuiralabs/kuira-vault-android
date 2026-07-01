import type * as __compactRuntime from '@midnight-ntwrk/compact-runtime';

export type Witnesses<PS> = {
}

export type ImpureCircuits<PS> = {
  depositUnshielded(context: __compactRuntime.CircuitContext<PS>,
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    to_0: { kind: number, address: Uint8Array },
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  revokeApproval(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  execute(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  isApprovedBySigner(context: __compactRuntime.CircuitContext<PS>,
                     id_0: bigint,
                     signer_0: { is_left: boolean,
                                 left: { bytes: Uint8Array },
                                 right: { bytes: Uint8Array }
                               }): __compactRuntime.CircuitResults<PS, boolean>;
  getApprovalCount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  getUnshieldedBalance(context: __compactRuntime.CircuitContext<PS>,
                       color_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  getProposal(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, { to: { kind: number,
                                                                                                                       address: Uint8Array
                                                                                                                     },
                                                                                                                 color: Uint8Array,
                                                                                                                 amount: bigint,
                                                                                                                 status: number
                                                                                                               }>;
  getProposalStatus(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, number>;
  getSignerCount(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  getThreshold(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  isSigner(context: __compactRuntime.CircuitContext<PS>,
           account_0: { is_left: boolean,
                        left: { bytes: Uint8Array },
                        right: { bytes: Uint8Array }
                      }): __compactRuntime.CircuitResults<PS, boolean>;
}

export type ProvableCircuits<PS> = {
  depositUnshielded(context: __compactRuntime.CircuitContext<PS>,
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    to_0: { kind: number, address: Uint8Array },
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  revokeApproval(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  execute(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  isApprovedBySigner(context: __compactRuntime.CircuitContext<PS>,
                     id_0: bigint,
                     signer_0: { is_left: boolean,
                                 left: { bytes: Uint8Array },
                                 right: { bytes: Uint8Array }
                               }): __compactRuntime.CircuitResults<PS, boolean>;
  getApprovalCount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  getUnshieldedBalance(context: __compactRuntime.CircuitContext<PS>,
                       color_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  getProposal(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, { to: { kind: number,
                                                                                                                       address: Uint8Array
                                                                                                                     },
                                                                                                                 color: Uint8Array,
                                                                                                                 amount: bigint,
                                                                                                                 status: number
                                                                                                               }>;
  getProposalStatus(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, number>;
  getSignerCount(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  getThreshold(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  isSigner(context: __compactRuntime.CircuitContext<PS>,
           account_0: { is_left: boolean,
                        left: { bytes: Uint8Array },
                        right: { bytes: Uint8Array }
                      }): __compactRuntime.CircuitResults<PS, boolean>;
}

export type PureCircuits = {
}

export type Circuits<PS> = {
  depositUnshielded(context: __compactRuntime.CircuitContext<PS>,
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  proposeWithdrawal(context: __compactRuntime.CircuitContext<PS>,
                    to_0: { kind: number, address: Uint8Array },
                    color_0: Uint8Array,
                    amount_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  approve(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  revokeApproval(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  execute(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, []>;
  isApprovedBySigner(context: __compactRuntime.CircuitContext<PS>,
                     id_0: bigint,
                     signer_0: { is_left: boolean,
                                 left: { bytes: Uint8Array },
                                 right: { bytes: Uint8Array }
                               }): __compactRuntime.CircuitResults<PS, boolean>;
  getApprovalCount(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, bigint>;
  getUnshieldedBalance(context: __compactRuntime.CircuitContext<PS>,
                       color_0: Uint8Array): __compactRuntime.CircuitResults<PS, bigint>;
  getProposal(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, { to: { kind: number,
                                                                                                                       address: Uint8Array
                                                                                                                     },
                                                                                                                 color: Uint8Array,
                                                                                                                 amount: bigint,
                                                                                                                 status: number
                                                                                                               }>;
  getProposalStatus(context: __compactRuntime.CircuitContext<PS>, id_0: bigint): __compactRuntime.CircuitResults<PS, number>;
  getSignerCount(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  getThreshold(context: __compactRuntime.CircuitContext<PS>): __compactRuntime.CircuitResults<PS, bigint>;
  isSigner(context: __compactRuntime.CircuitContext<PS>,
           account_0: { is_left: boolean,
                        left: { bytes: Uint8Array },
                        right: { bytes: Uint8Array }
                      }): __compactRuntime.CircuitResults<PS, boolean>;
}

export type Ledger = {
  _proposalApprovals: {
    isEmpty(): boolean;
    size(): bigint;
    member(key_0: bigint): boolean;
    lookup(key_0: bigint): {
      isEmpty(): boolean;
      size(): bigint;
      member(key_1: { is_left: boolean,
                      left: { bytes: Uint8Array },
                      right: { bytes: Uint8Array }
                    }): boolean;
      lookup(key_1: { is_left: boolean,
                      left: { bytes: Uint8Array },
                      right: { bytes: Uint8Array }
                    }): boolean;
      [Symbol.iterator](): Iterator<[{ is_left: boolean, left: { bytes: Uint8Array }, right: { bytes: Uint8Array } }, boolean]>
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
               signers_0: { is_left: boolean,
                            left: { bytes: Uint8Array },
                            right: { bytes: Uint8Array }
                          }[],
               thresh_0: bigint): __compactRuntime.ConstructorResult<PS>;
}

export declare function ledger(state: __compactRuntime.StateValue | __compactRuntime.ChargedState): Ledger;
export declare const pureCircuits: PureCircuits;

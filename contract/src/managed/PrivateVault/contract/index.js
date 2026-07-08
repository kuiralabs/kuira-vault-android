import * as __compactRuntime from '@midnight-ntwrk/compact-runtime';
__compactRuntime.checkRuntimeVersion('0.16.0');

export var PrivateProposalStatus;
(function (PrivateProposalStatus) {
  PrivateProposalStatus[PrivateProposalStatus['Inactive'] = 0] = 'Inactive';
  PrivateProposalStatus[PrivateProposalStatus['Active'] = 1] = 'Active';
  PrivateProposalStatus[PrivateProposalStatus['Executed'] = 2] = 'Executed';
})(PrivateProposalStatus || (PrivateProposalStatus = {}));

const _descriptor_0 = new __compactRuntime.CompactTypeBytes(32);

const _descriptor_1 = new __compactRuntime.CompactTypeUnsignedInteger(340282366920938463463374607431768211455n, 16);

const _descriptor_2 = new __compactRuntime.CompactTypeUnsignedInteger(18446744073709551615n, 8);

const _descriptor_3 = new __compactRuntime.CompactTypeEnum(2, 1);

const _descriptor_4 = __compactRuntime.CompactTypeBoolean;

const _descriptor_5 = new __compactRuntime.CompactTypeUnsignedInteger(255n, 1);

const _descriptor_6 = new __compactRuntime.CompactTypeBytes(160);

const _descriptor_7 = new __compactRuntime.CompactTypeVector(5, _descriptor_0);

const _descriptor_8 = new __compactRuntime.CompactTypeUnsignedInteger(65535n, 2);

class _ContractAddress_0 {
  alignment() {
    return _descriptor_0.alignment();
  }
  fromValue(value_0) {
    return {
      bytes: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.bytes);
  }
}

const _descriptor_9 = new _ContractAddress_0();

class _UserAddress_0 {
  alignment() {
    return _descriptor_0.alignment();
  }
  fromValue(value_0) {
    return {
      bytes: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.bytes);
  }
}

const _descriptor_10 = new _UserAddress_0();

class _Either_0 {
  alignment() {
    return _descriptor_4.alignment().concat(_descriptor_9.alignment().concat(_descriptor_10.alignment()));
  }
  fromValue(value_0) {
    return {
      is_left: _descriptor_4.fromValue(value_0),
      left: _descriptor_9.fromValue(value_0),
      right: _descriptor_10.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_4.toValue(value_0.is_left).concat(_descriptor_9.toValue(value_0.left).concat(_descriptor_10.toValue(value_0.right)));
  }
}

const _descriptor_11 = new _Either_0();

class _ZswapCoinPublicKey_0 {
  alignment() {
    return _descriptor_0.alignment();
  }
  fromValue(value_0) {
    return {
      bytes: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.bytes);
  }
}

const _descriptor_12 = new _ZswapCoinPublicKey_0();

class _ThresholdCommitmentInput_0 {
  alignment() {
    return _descriptor_5.alignment().concat(_descriptor_0.alignment().concat(_descriptor_0.alignment()));
  }
  fromValue(value_0) {
    return {
      threshold: _descriptor_5.fromValue(value_0),
      salt: _descriptor_0.fromValue(value_0),
      domain: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_5.toValue(value_0.threshold).concat(_descriptor_0.toValue(value_0.salt).concat(_descriptor_0.toValue(value_0.domain)));
  }
}

const _descriptor_13 = new _ThresholdCommitmentInput_0();

class _ProposalCommitmentInput_0 {
  alignment() {
    return _descriptor_4.alignment().concat(_descriptor_0.alignment().concat(_descriptor_0.alignment().concat(_descriptor_1.alignment().concat(_descriptor_0.alignment().concat(_descriptor_0.alignment())))));
  }
  fromValue(value_0) {
    return {
      recipientIsContract: _descriptor_4.fromValue(value_0),
      recipient: _descriptor_0.fromValue(value_0),
      color: _descriptor_0.fromValue(value_0),
      amount: _descriptor_1.fromValue(value_0),
      nonce: _descriptor_0.fromValue(value_0),
      domain: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_4.toValue(value_0.recipientIsContract).concat(_descriptor_0.toValue(value_0.recipient).concat(_descriptor_0.toValue(value_0.color).concat(_descriptor_1.toValue(value_0.amount).concat(_descriptor_0.toValue(value_0.nonce).concat(_descriptor_0.toValue(value_0.domain))))));
  }
}

const _descriptor_14 = new _ProposalCommitmentInput_0();

class _ApprovalTagInput_0 {
  alignment() {
    return _descriptor_0.alignment().concat(_descriptor_0.alignment().concat(_descriptor_2.alignment().concat(_descriptor_0.alignment())));
  }
  fromValue(value_0) {
    return {
      pk: _descriptor_0.fromValue(value_0),
      salt: _descriptor_0.fromValue(value_0),
      id: _descriptor_2.fromValue(value_0),
      domain: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.pk).concat(_descriptor_0.toValue(value_0.salt).concat(_descriptor_2.toValue(value_0.id).concat(_descriptor_0.toValue(value_0.domain))));
  }
}

const _descriptor_15 = new _ApprovalTagInput_0();

class _SignerCommitmentInput_0 {
  alignment() {
    return _descriptor_0.alignment().concat(_descriptor_0.alignment().concat(_descriptor_0.alignment()));
  }
  fromValue(value_0) {
    return {
      pk: _descriptor_0.fromValue(value_0),
      salt: _descriptor_0.fromValue(value_0),
      domain: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_0.toValue(value_0.pk).concat(_descriptor_0.toValue(value_0.salt).concat(_descriptor_0.toValue(value_0.domain)));
  }
}

const _descriptor_16 = new _SignerCommitmentInput_0();

class _Either_1 {
  alignment() {
    return _descriptor_4.alignment().concat(_descriptor_0.alignment().concat(_descriptor_0.alignment()));
  }
  fromValue(value_0) {
    return {
      is_left: _descriptor_4.fromValue(value_0),
      left: _descriptor_0.fromValue(value_0),
      right: _descriptor_0.fromValue(value_0)
    }
  }
  toValue(value_0) {
    return _descriptor_4.toValue(value_0.is_left).concat(_descriptor_0.toValue(value_0.left).concat(_descriptor_0.toValue(value_0.right)));
  }
}

const _descriptor_17 = new _Either_1();

export class Contract {
  witnesses;
  constructor(...args_0) {
    if (args_0.length !== 1) {
      throw new __compactRuntime.CompactError(`Contract constructor: expected 1 argument, received ${args_0.length}`);
    }
    const witnesses_0 = args_0[0];
    if (typeof(witnesses_0) !== 'object') {
      throw new __compactRuntime.CompactError('first (witnesses) argument to Contract constructor is not an object');
    }
    this.witnesses = witnesses_0;
    this.circuits = {
      pvDepositUnshielded: (...args_1) => {
        if (args_1.length !== 3) {
          throw new __compactRuntime.CompactError(`pvDepositUnshielded: expected 3 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const color_0 = args_1[1];
        const amount_0 = args_1[2];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('pvDepositUnshielded',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 112 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(color_0.buffer instanceof ArrayBuffer && color_0.BYTES_PER_ELEMENT === 1 && color_0.length === 32)) {
          __compactRuntime.typeError('pvDepositUnshielded',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 112 char 1',
                                     'Bytes<32>',
                                     color_0)
        }
        if (!(typeof(amount_0) === 'bigint' && amount_0 >= 0n && amount_0 <= 340282366920938463463374607431768211455n)) {
          __compactRuntime.typeError('pvDepositUnshielded',
                                     'argument 2 (argument 3 as invoked from Typescript)',
                                     'PrivateVault.compact line 112 char 1',
                                     'Uint<0..340282366920938463463374607431768211456>',
                                     amount_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_0.toValue(color_0).concat(_descriptor_1.toValue(amount_0)),
            alignment: _descriptor_0.alignment().concat(_descriptor_1.alignment())
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._pvDepositUnshielded_0(context,
                                                     partialProofData,
                                                     color_0,
                                                     amount_0);
        partialProofData.output = { value: [], alignment: [] };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      pvProposeWithdrawal: (...args_1) => {
        if (args_1.length !== 4) {
          throw new __compactRuntime.CompactError(`pvProposeWithdrawal: expected 4 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const commitment_0 = args_1[1];
        const payload_0 = args_1[2];
        const salt_0 = args_1[3];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('pvProposeWithdrawal',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 122 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(commitment_0.buffer instanceof ArrayBuffer && commitment_0.BYTES_PER_ELEMENT === 1 && commitment_0.length === 32)) {
          __compactRuntime.typeError('pvProposeWithdrawal',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 122 char 1',
                                     'Bytes<32>',
                                     commitment_0)
        }
        if (!(payload_0.buffer instanceof ArrayBuffer && payload_0.BYTES_PER_ELEMENT === 1 && payload_0.length === 160)) {
          __compactRuntime.typeError('pvProposeWithdrawal',
                                     'argument 2 (argument 3 as invoked from Typescript)',
                                     'PrivateVault.compact line 122 char 1',
                                     'Bytes<160>',
                                     payload_0)
        }
        if (!(salt_0.buffer instanceof ArrayBuffer && salt_0.BYTES_PER_ELEMENT === 1 && salt_0.length === 32)) {
          __compactRuntime.typeError('pvProposeWithdrawal',
                                     'argument 3 (argument 4 as invoked from Typescript)',
                                     'PrivateVault.compact line 122 char 1',
                                     'Bytes<32>',
                                     salt_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_0.toValue(commitment_0).concat(_descriptor_6.toValue(payload_0).concat(_descriptor_0.toValue(salt_0))),
            alignment: _descriptor_0.alignment().concat(_descriptor_6.alignment().concat(_descriptor_0.alignment()))
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._pvProposeWithdrawal_0(context,
                                                     partialProofData,
                                                     commitment_0,
                                                     payload_0,
                                                     salt_0);
        partialProofData.output = { value: _descriptor_2.toValue(result_0), alignment: _descriptor_2.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      pvApprove: (...args_1) => {
        if (args_1.length !== 3) {
          throw new __compactRuntime.CompactError(`pvApprove: expected 3 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const id_0 = args_1[1];
        const salt_0 = args_1[2];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('pvApprove',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 141 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(id_0) === 'bigint' && id_0 >= 0n && id_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('pvApprove',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 141 char 1',
                                     'Uint<0..18446744073709551616>',
                                     id_0)
        }
        if (!(salt_0.buffer instanceof ArrayBuffer && salt_0.BYTES_PER_ELEMENT === 1 && salt_0.length === 32)) {
          __compactRuntime.typeError('pvApprove',
                                     'argument 2 (argument 3 as invoked from Typescript)',
                                     'PrivateVault.compact line 141 char 1',
                                     'Bytes<32>',
                                     salt_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_2.toValue(id_0).concat(_descriptor_0.toValue(salt_0)),
            alignment: _descriptor_2.alignment().concat(_descriptor_0.alignment())
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._pvApprove_0(context,
                                           partialProofData,
                                           id_0,
                                           salt_0);
        partialProofData.output = { value: [], alignment: [] };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      pvRevokeApproval: (...args_1) => {
        if (args_1.length !== 3) {
          throw new __compactRuntime.CompactError(`pvRevokeApproval: expected 3 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const id_0 = args_1[1];
        const salt_0 = args_1[2];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('pvRevokeApproval',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 153 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(id_0) === 'bigint' && id_0 >= 0n && id_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('pvRevokeApproval',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 153 char 1',
                                     'Uint<0..18446744073709551616>',
                                     id_0)
        }
        if (!(salt_0.buffer instanceof ArrayBuffer && salt_0.BYTES_PER_ELEMENT === 1 && salt_0.length === 32)) {
          __compactRuntime.typeError('pvRevokeApproval',
                                     'argument 2 (argument 3 as invoked from Typescript)',
                                     'PrivateVault.compact line 153 char 1',
                                     'Bytes<32>',
                                     salt_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_2.toValue(id_0).concat(_descriptor_0.toValue(salt_0)),
            alignment: _descriptor_2.alignment().concat(_descriptor_0.alignment())
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._pvRevokeApproval_0(context,
                                                  partialProofData,
                                                  id_0,
                                                  salt_0);
        partialProofData.output = { value: [], alignment: [] };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      pvExecute: (...args_1) => {
        if (args_1.length !== 9) {
          throw new __compactRuntime.CompactError(`pvExecute: expected 9 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const id_0 = args_1[1];
        const recipientIsContract_0 = args_1[2];
        const recipient_0 = args_1[3];
        const color_0 = args_1[4];
        const amount_0 = args_1[5];
        const nonce_0 = args_1[6];
        const threshold_0 = args_1[7];
        const thresholdSalt_0 = args_1[8];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(id_0) === 'bigint' && id_0 >= 0n && id_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Uint<0..18446744073709551616>',
                                     id_0)
        }
        if (!(typeof(recipientIsContract_0) === 'boolean')) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 2 (argument 3 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Boolean',
                                     recipientIsContract_0)
        }
        if (!(recipient_0.buffer instanceof ArrayBuffer && recipient_0.BYTES_PER_ELEMENT === 1 && recipient_0.length === 32)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 3 (argument 4 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Bytes<32>',
                                     recipient_0)
        }
        if (!(color_0.buffer instanceof ArrayBuffer && color_0.BYTES_PER_ELEMENT === 1 && color_0.length === 32)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 4 (argument 5 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Bytes<32>',
                                     color_0)
        }
        if (!(typeof(amount_0) === 'bigint' && amount_0 >= 0n && amount_0 <= 340282366920938463463374607431768211455n)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 5 (argument 6 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Uint<0..340282366920938463463374607431768211456>',
                                     amount_0)
        }
        if (!(nonce_0.buffer instanceof ArrayBuffer && nonce_0.BYTES_PER_ELEMENT === 1 && nonce_0.length === 32)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 6 (argument 7 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Bytes<32>',
                                     nonce_0)
        }
        if (!(typeof(threshold_0) === 'bigint' && threshold_0 >= 0n && threshold_0 <= 255n)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 7 (argument 8 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Uint<0..256>',
                                     threshold_0)
        }
        if (!(thresholdSalt_0.buffer instanceof ArrayBuffer && thresholdSalt_0.BYTES_PER_ELEMENT === 1 && thresholdSalt_0.length === 32)) {
          __compactRuntime.typeError('pvExecute',
                                     'argument 8 (argument 9 as invoked from Typescript)',
                                     'PrivateVault.compact line 171 char 1',
                                     'Bytes<32>',
                                     thresholdSalt_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_2.toValue(id_0).concat(_descriptor_4.toValue(recipientIsContract_0).concat(_descriptor_0.toValue(recipient_0).concat(_descriptor_0.toValue(color_0).concat(_descriptor_1.toValue(amount_0).concat(_descriptor_0.toValue(nonce_0).concat(_descriptor_5.toValue(threshold_0).concat(_descriptor_0.toValue(thresholdSalt_0)))))))),
            alignment: _descriptor_2.alignment().concat(_descriptor_4.alignment().concat(_descriptor_0.alignment().concat(_descriptor_0.alignment().concat(_descriptor_1.alignment().concat(_descriptor_0.alignment().concat(_descriptor_5.alignment().concat(_descriptor_0.alignment())))))))
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._pvExecute_0(context,
                                           partialProofData,
                                           id_0,
                                           recipientIsContract_0,
                                           recipient_0,
                                           color_0,
                                           amount_0,
                                           nonce_0,
                                           threshold_0,
                                           thresholdSalt_0);
        partialProofData.output = { value: [], alignment: [] };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      signerCommitment(context, ...args_1) {
        return { result: pureCircuits.signerCommitment(...args_1), context };
      },
      thresholdCommitment(context, ...args_1) {
        return { result: pureCircuits.thresholdCommitment(...args_1), context };
      },
      proposalCommitment(context, ...args_1) {
        return { result: pureCircuits.proposalCommitment(...args_1), context };
      },
      getProposalCount: (...args_1) => {
        if (args_1.length !== 1) {
          throw new __compactRuntime.CompactError(`getProposalCount: expected 1 argument (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('getProposalCount',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 290 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: { value: [], alignment: [] },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._getProposalCount_0(context, partialProofData);
        partialProofData.output = { value: _descriptor_2.toValue(result_0), alignment: _descriptor_2.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      getProposalCommitment: (...args_1) => {
        if (args_1.length !== 2) {
          throw new __compactRuntime.CompactError(`getProposalCommitment: expected 2 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const id_0 = args_1[1];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('getProposalCommitment',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 294 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(id_0) === 'bigint' && id_0 >= 0n && id_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('getProposalCommitment',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 294 char 1',
                                     'Uint<0..18446744073709551616>',
                                     id_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_2.toValue(id_0),
            alignment: _descriptor_2.alignment()
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._getProposalCommitment_0(context,
                                                       partialProofData,
                                                       id_0);
        partialProofData.output = { value: _descriptor_0.toValue(result_0), alignment: _descriptor_0.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      getProposalPayload: (...args_1) => {
        if (args_1.length !== 2) {
          throw new __compactRuntime.CompactError(`getProposalPayload: expected 2 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const id_0 = args_1[1];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('getProposalPayload',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 299 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(id_0) === 'bigint' && id_0 >= 0n && id_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('getProposalPayload',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 299 char 1',
                                     'Uint<0..18446744073709551616>',
                                     id_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_2.toValue(id_0),
            alignment: _descriptor_2.alignment()
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._getProposalPayload_0(context,
                                                    partialProofData,
                                                    id_0);
        partialProofData.output = { value: _descriptor_6.toValue(result_0), alignment: _descriptor_6.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      getProposalStatus: (...args_1) => {
        if (args_1.length !== 2) {
          throw new __compactRuntime.CompactError(`getProposalStatus: expected 2 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const id_0 = args_1[1];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('getProposalStatus',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 304 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(id_0) === 'bigint' && id_0 >= 0n && id_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('getProposalStatus',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 304 char 1',
                                     'Uint<0..18446744073709551616>',
                                     id_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_2.toValue(id_0),
            alignment: _descriptor_2.alignment()
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._getProposalStatus_0(context,
                                                   partialProofData,
                                                   id_0);
        partialProofData.output = { value: _descriptor_3.toValue(result_0), alignment: _descriptor_3.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      getApprovalCount: (...args_1) => {
        if (args_1.length !== 2) {
          throw new __compactRuntime.CompactError(`getApprovalCount: expected 2 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const id_0 = args_1[1];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('getApprovalCount',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 309 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(typeof(id_0) === 'bigint' && id_0 >= 0n && id_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('getApprovalCount',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 309 char 1',
                                     'Uint<0..18446744073709551616>',
                                     id_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_2.toValue(id_0),
            alignment: _descriptor_2.alignment()
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._getApprovalCount_0(context,
                                                  partialProofData,
                                                  id_0);
        partialProofData.output = { value: _descriptor_5.toValue(result_0), alignment: _descriptor_5.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      getUnshieldedBalance: (...args_1) => {
        if (args_1.length !== 2) {
          throw new __compactRuntime.CompactError(`getUnshieldedBalance: expected 2 arguments (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        const color_0 = args_1[1];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('getUnshieldedBalance',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 316 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        if (!(color_0.buffer instanceof ArrayBuffer && color_0.BYTES_PER_ELEMENT === 1 && color_0.length === 32)) {
          __compactRuntime.typeError('getUnshieldedBalance',
                                     'argument 1 (argument 2 as invoked from Typescript)',
                                     'PrivateVault.compact line 316 char 1',
                                     'Bytes<32>',
                                     color_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: {
            value: _descriptor_0.toValue(color_0),
            alignment: _descriptor_0.alignment()
          },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._getUnshieldedBalance_0(context,
                                                      partialProofData,
                                                      color_0);
        partialProofData.output = { value: _descriptor_1.toValue(result_0), alignment: _descriptor_1.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      },
      getThresholdCommitment: (...args_1) => {
        if (args_1.length !== 1) {
          throw new __compactRuntime.CompactError(`getThresholdCommitment: expected 1 argument (as invoked from Typescript), received ${args_1.length}`);
        }
        const contextOrig_0 = args_1[0];
        if (!(typeof(contextOrig_0) === 'object' && contextOrig_0.currentQueryContext != undefined)) {
          __compactRuntime.typeError('getThresholdCommitment',
                                     'argument 1 (as invoked from Typescript)',
                                     'PrivateVault.compact line 320 char 1',
                                     'CircuitContext',
                                     contextOrig_0)
        }
        const context = { ...contextOrig_0, gasCost: __compactRuntime.emptyRunningCost() };
        const partialProofData = {
          input: { value: [], alignment: [] },
          output: undefined,
          publicTranscript: [],
          privateTranscriptOutputs: []
        };
        const result_0 = this._getThresholdCommitment_0(context,
                                                        partialProofData);
        partialProofData.output = { value: _descriptor_0.toValue(result_0), alignment: _descriptor_0.alignment() };
        return { result: result_0, context: context, proofData: partialProofData, gasCost: context.gasCost };
      }
    };
    this.impureCircuits = {
      pvDepositUnshielded: this.circuits.pvDepositUnshielded,
      pvProposeWithdrawal: this.circuits.pvProposeWithdrawal,
      pvApprove: this.circuits.pvApprove,
      pvRevokeApproval: this.circuits.pvRevokeApproval,
      pvExecute: this.circuits.pvExecute,
      getProposalCount: this.circuits.getProposalCount,
      getProposalCommitment: this.circuits.getProposalCommitment,
      getProposalPayload: this.circuits.getProposalPayload,
      getProposalStatus: this.circuits.getProposalStatus,
      getApprovalCount: this.circuits.getApprovalCount,
      getUnshieldedBalance: this.circuits.getUnshieldedBalance,
      getThresholdCommitment: this.circuits.getThresholdCommitment
    };
    this.provableCircuits = {
      pvDepositUnshielded: this.circuits.pvDepositUnshielded,
      pvProposeWithdrawal: this.circuits.pvProposeWithdrawal,
      pvApprove: this.circuits.pvApprove,
      pvRevokeApproval: this.circuits.pvRevokeApproval,
      pvExecute: this.circuits.pvExecute,
      getProposalCount: this.circuits.getProposalCount,
      getProposalCommitment: this.circuits.getProposalCommitment,
      getProposalPayload: this.circuits.getProposalPayload,
      getProposalStatus: this.circuits.getProposalStatus,
      getApprovalCount: this.circuits.getApprovalCount,
      getUnshieldedBalance: this.circuits.getUnshieldedBalance,
      getThresholdCommitment: this.circuits.getThresholdCommitment
    };
  }
  initialState(...args_0) {
    if (args_0.length !== 3) {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 3 arguments (as invoked from Typescript), received ${args_0.length}`);
    }
    const constructorContext_0 = args_0[0];
    const signerCommitments_0 = args_0[1];
    const thresholdCommitment_0 = args_0[2];
    if (typeof(constructorContext_0) !== 'object') {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 'constructorContext' in argument 1 (as invoked from Typescript) to be an object`);
    }
    if (!('initialZswapLocalState' in constructorContext_0)) {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 'initialZswapLocalState' in argument 1 (as invoked from Typescript)`);
    }
    if (typeof(constructorContext_0.initialZswapLocalState) !== 'object') {
      throw new __compactRuntime.CompactError(`Contract state constructor: expected 'initialZswapLocalState' in argument 1 (as invoked from Typescript) to be an object`);
    }
    if (!(Array.isArray(signerCommitments_0) && signerCommitments_0.length === 5 && signerCommitments_0.every((t) => t.buffer instanceof ArrayBuffer && t.BYTES_PER_ELEMENT === 1 && t.length === 32))) {
      __compactRuntime.typeError('Contract state constructor',
                                 'argument 1 (argument 2 as invoked from Typescript)',
                                 'PrivateVault.compact line 98 char 1',
                                 'Vector<5, Bytes<32>>',
                                 signerCommitments_0)
    }
    if (!(thresholdCommitment_0.buffer instanceof ArrayBuffer && thresholdCommitment_0.BYTES_PER_ELEMENT === 1 && thresholdCommitment_0.length === 32)) {
      __compactRuntime.typeError('Contract state constructor',
                                 'argument 2 (argument 3 as invoked from Typescript)',
                                 'PrivateVault.compact line 98 char 1',
                                 'Bytes<32>',
                                 thresholdCommitment_0)
    }
    const state_0 = new __compactRuntime.ContractState();
    let stateValue_0 = __compactRuntime.StateValue.newArray();
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    stateValue_0 = stateValue_0.arrayPush(__compactRuntime.StateValue.newNull());
    state_0.data = new __compactRuntime.ChargedState(stateValue_0);
    state_0.setOperation('pvDepositUnshielded', new __compactRuntime.ContractOperation());
    state_0.setOperation('pvProposeWithdrawal', new __compactRuntime.ContractOperation());
    state_0.setOperation('pvApprove', new __compactRuntime.ContractOperation());
    state_0.setOperation('pvRevokeApproval', new __compactRuntime.ContractOperation());
    state_0.setOperation('pvExecute', new __compactRuntime.ContractOperation());
    state_0.setOperation('getProposalCount', new __compactRuntime.ContractOperation());
    state_0.setOperation('getProposalCommitment', new __compactRuntime.ContractOperation());
    state_0.setOperation('getProposalPayload', new __compactRuntime.ContractOperation());
    state_0.setOperation('getProposalStatus', new __compactRuntime.ContractOperation());
    state_0.setOperation('getApprovalCount', new __compactRuntime.ContractOperation());
    state_0.setOperation('getUnshieldedBalance', new __compactRuntime.ContractOperation());
    state_0.setOperation('getThresholdCommitment', new __compactRuntime.ContractOperation());
    const context = __compactRuntime.createCircuitContext(__compactRuntime.dummyContractAddress(), constructorContext_0.initialZswapLocalState.coinPublicKey, state_0.data, constructorContext_0.initialPrivateState);
    const partialProofData = {
      input: { value: [], alignment: [] },
      output: undefined,
      publicTranscript: [],
      privateTranscriptOutputs: []
    };
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(0n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newMap(
                                                          new __compactRuntime.StateMap()
                                                        ).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(1n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_7.toValue(new Array(5).fill(new Uint8Array(32))),
                                                                                              alignment: _descriptor_7.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(2n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(new Uint8Array(32)),
                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(3n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(0n),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(4n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newMap(
                                                          new __compactRuntime.StateMap()
                                                        ).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(5n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newMap(
                                                          new __compactRuntime.StateMap()
                                                        ).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(6n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newMap(
                                                          new __compactRuntime.StateMap()
                                                        ).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(7n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newMap(
                                                          new __compactRuntime.StateMap()
                                                        ).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(8n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newMap(
                                                          new __compactRuntime.StateMap()
                                                        ).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    this._folder_0(context,
                   partialProofData,
                   ((context, partialProofData, t_0, c_0) =>
                    {
                      __compactRuntime.assert(!this._equal_0(c_0,
                                                             new Uint8Array(32)),
                                              'PrivateVault: zero signer commitment');
                      return t_0;
                    }),
                   [],
                   signerCommitments_0);
    __compactRuntime.assert(!this._equal_1(thresholdCommitment_0,
                                           new Uint8Array(32)),
                            'PrivateVault: zero threshold commitment');
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(1n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_7.toValue(signerCommitments_0),
                                                                                              alignment: _descriptor_7.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(2n),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(thresholdCommitment_0),
                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } }]);
    state_0.data = new __compactRuntime.ChargedState(context.currentQueryContext.state.state);
    return {
      currentContractState: state_0,
      currentPrivateState: context.currentPrivateState,
      currentZswapLocalState: context.currentZswapLocalState
    }
  }
  _left_0(value_0) {
    return { is_left: true, left: value_0, right: new Uint8Array(32) };
  }
  _left_1(value_0) {
    return { is_left: true, left: value_0, right: { bytes: new Uint8Array(32) } };
  }
  _right_0(value_0) {
    return { is_left: false, left: { bytes: new Uint8Array(32) }, right: value_0 };
  }
  _sendUnshielded_0(context, partialProofData, color_0, amount_0, recipient_0) {
    const tmp_0 = this._left_0(color_0);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { swap: { n: 0 } },
                                       { idx: { cached: true,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(7n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_17.toValue(tmp_0),
                                                                                              alignment: _descriptor_17.alignment() }).encode() } },
                                       { dup: { n: 1 } },
                                       { dup: { n: 1 } },
                                       'member',
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_1.toValue(amount_0),
                                                                                              alignment: _descriptor_1.alignment() }).encode() } },
                                       { swap: { n: 0 } },
                                       'neg',
                                       { branch: { skip: 4 } },
                                       { dup: { n: 2 } },
                                       { dup: { n: 2 } },
                                       { idx: { cached: true,
                                                pushPath: false,
                                                path: [ { tag: 'stack' }] } },
                                       'add',
                                       { ins: { cached: true, n: 2 } },
                                       { swap: { n: 0 } }]);
    const tmp_1 = this._left_0(color_0);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { swap: { n: 0 } },
                                       { idx: { cached: true,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(8n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell(__compactRuntime.alignedConcat(
                                                                                              { value: _descriptor_17.toValue(tmp_1),
                                                                                                alignment: _descriptor_17.alignment() },
                                                                                              { value: _descriptor_11.toValue(recipient_0),
                                                                                                alignment: _descriptor_11.alignment() }
                                                                                            )).encode() } },
                                       { dup: { n: 1 } },
                                       { dup: { n: 1 } },
                                       'member',
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_1.toValue(amount_0),
                                                                                              alignment: _descriptor_1.alignment() }).encode() } },
                                       { swap: { n: 0 } },
                                       'neg',
                                       { branch: { skip: 4 } },
                                       { dup: { n: 2 } },
                                       { dup: { n: 2 } },
                                       { idx: { cached: true,
                                                pushPath: false,
                                                path: [ { tag: 'stack' }] } },
                                       'add',
                                       { ins: { cached: true, n: 2 } },
                                       { swap: { n: 0 } }]);
    if (recipient_0.is_left
        &&
        this._equal_2(recipient_0.left.bytes,
                      _descriptor_9.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                partialProofData,
                                                                                [
                                                                                 { dup: { n: 2 } },
                                                                                 { idx: { cached: true,
                                                                                          pushPath: false,
                                                                                          path: [
                                                                                                 { tag: 'value',
                                                                                                   value: { value: _descriptor_5.toValue(0n),
                                                                                                            alignment: _descriptor_5.alignment() } }] } },
                                                                                 { popeq: { cached: true,
                                                                                            result: undefined } }]).value).bytes))
    {
      const tmp_2 = this._left_0(color_0);
      __compactRuntime.queryLedgerState(context,
                                        partialProofData,
                                        [
                                         { swap: { n: 0 } },
                                         { idx: { cached: true,
                                                  pushPath: true,
                                                  path: [
                                                         { tag: 'value',
                                                           value: { value: _descriptor_5.toValue(6n),
                                                                    alignment: _descriptor_5.alignment() } }] } },
                                         { push: { storage: false,
                                                   value: __compactRuntime.StateValue.newCell({ value: _descriptor_17.toValue(tmp_2),
                                                                                                alignment: _descriptor_17.alignment() }).encode() } },
                                         { dup: { n: 1 } },
                                         { dup: { n: 1 } },
                                         'member',
                                         { push: { storage: false,
                                                   value: __compactRuntime.StateValue.newCell({ value: _descriptor_1.toValue(amount_0),
                                                                                                alignment: _descriptor_1.alignment() }).encode() } },
                                         { swap: { n: 0 } },
                                         'neg',
                                         { branch: { skip: 4 } },
                                         { dup: { n: 2 } },
                                         { dup: { n: 2 } },
                                         { idx: { cached: true,
                                                  pushPath: false,
                                                  path: [ { tag: 'stack' }] } },
                                         'add',
                                         { ins: { cached: true, n: 2 } },
                                         { swap: { n: 0 } }]);
    }
    return [];
  }
  _receiveUnshielded_0(context, partialProofData, color_0, amount_0) {
    const tmp_0 = this._left_0(color_0);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { swap: { n: 0 } },
                                       { idx: { cached: true,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(6n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_17.toValue(tmp_0),
                                                                                              alignment: _descriptor_17.alignment() }).encode() } },
                                       { dup: { n: 1 } },
                                       { dup: { n: 1 } },
                                       'member',
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_1.toValue(amount_0),
                                                                                              alignment: _descriptor_1.alignment() }).encode() } },
                                       { swap: { n: 0 } },
                                       'neg',
                                       { branch: { skip: 4 } },
                                       { dup: { n: 2 } },
                                       { dup: { n: 2 } },
                                       { idx: { cached: true,
                                                pushPath: false,
                                                path: [ { tag: 'stack' }] } },
                                       'add',
                                       { ins: { cached: true, n: 2 } },
                                       { swap: { n: 0 } }]);
    return [];
  }
  _persistentHash_0(value_0) {
    const result_0 = __compactRuntime.persistentHash(_descriptor_15, value_0);
    return result_0;
  }
  _persistentHash_1(value_0) {
    const result_0 = __compactRuntime.persistentHash(_descriptor_16, value_0);
    return result_0;
  }
  _persistentHash_2(value_0) {
    const result_0 = __compactRuntime.persistentHash(_descriptor_13, value_0);
    return result_0;
  }
  _persistentHash_3(value_0) {
    const result_0 = __compactRuntime.persistentHash(_descriptor_14, value_0);
    return result_0;
  }
  _ownPublicKey_0(context, partialProofData) {
    const result_0 = __compactRuntime.ownPublicKey(context);
    partialProofData.privateTranscriptOutputs.push({
      value: _descriptor_12.toValue(result_0),
      alignment: _descriptor_12.alignment()
    });
    return result_0;
  }
  __deposit_0(context, partialProofData, color_0, amount_0) {
    this._receiveUnshielded_0(context, partialProofData, color_0, amount_0);
    const bal_0 = this._getTokenBalance_0(context, partialProofData, color_0);
    const tmp_0 = ((t1) => {
                    if (t1 > 340282366920938463463374607431768211455n) {
                      throw new __compactRuntime.CompactError('UnshieldedTreasury.compact line 62 char 48: cast from Field or Uint value to smaller Uint value failed: ' + t1 + ' is greater than 340282366920938463463374607431768211455');
                    }
                    return t1;
                  })(bal_0 + amount_0);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(0n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(color_0),
                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_1.toValue(tmp_0),
                                                                                              alignment: _descriptor_1.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    return [];
  }
  __send_0(context, partialProofData, recipient_0, color_0, amount_0) {
    let t_0;
    __compactRuntime.assert((t_0 = this._getTokenBalance_0(context,
                                                           partialProofData,
                                                           color_0),
                             t_0 >= amount_0),
                            'UnshieldedTreasury: insufficient balance');
    const bal_0 = this._getTokenBalance_0(context, partialProofData, color_0);
    const tmp_0 = (__compactRuntime.assert(bal_0 >= amount_0,
                                           'result of subtraction would be negative'),
                   bal_0 - amount_0);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(0n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(color_0),
                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_1.toValue(tmp_0),
                                                                                              alignment: _descriptor_1.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    this._sendUnshielded_0(context,
                           partialProofData,
                           color_0,
                           amount_0,
                           recipient_0);
    return [];
  }
  _getTokenBalance_0(context, partialProofData, color_0) {
    if (!_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                   partialProofData,
                                                                   [
                                                                    { dup: { n: 0 } },
                                                                    { idx: { cached: false,
                                                                             pushPath: false,
                                                                             path: [
                                                                                    { tag: 'value',
                                                                                      value: { value: _descriptor_5.toValue(0n),
                                                                                               alignment: _descriptor_5.alignment() } }] } },
                                                                    { push: { storage: false,
                                                                              value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(color_0),
                                                                                                                           alignment: _descriptor_0.alignment() }).encode() } },
                                                                    'member',
                                                                    { popeq: { cached: true,
                                                                               result: undefined } }]).value))
    {
      return 0n;
    } else {
      return _descriptor_1.fromValue(__compactRuntime.queryLedgerState(context,
                                                                       partialProofData,
                                                                       [
                                                                        { dup: { n: 0 } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_5.toValue(0n),
                                                                                                   alignment: _descriptor_5.alignment() } }] } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_0.toValue(color_0),
                                                                                                   alignment: _descriptor_0.alignment() } }] } },
                                                                        { popeq: { cached: false,
                                                                                   result: undefined } }]).value);
    }
  }
  _pvDepositUnshielded_0(context, partialProofData, color_0, amount_0) {
    this.__deposit_0(context, partialProofData, color_0, amount_0); return [];
  }
  _pvProposeWithdrawal_0(context,
                         partialProofData,
                         commitment_0,
                         payload_0,
                         salt_0)
  {
    this._assertSigner_0(context, partialProofData, salt_0);
    __compactRuntime.assert(!this._equal_3(commitment_0, new Uint8Array(32)),
                            'PrivateVault: zero proposal commitment');
    const id_0 = _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                           partialProofData,
                                                                           [
                                                                            { dup: { n: 0 } },
                                                                            { idx: { cached: false,
                                                                                     pushPath: false,
                                                                                     path: [
                                                                                            { tag: 'value',
                                                                                              value: { value: _descriptor_5.toValue(3n),
                                                                                                       alignment: _descriptor_5.alignment() } }] } },
                                                                            { popeq: { cached: true,
                                                                                       result: undefined } }]).value);
    const tmp_0 = 1n;
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(3n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { addi: { immediate: parseInt(__compactRuntime.valueToBigInt(
                                                              { value: _descriptor_8.toValue(tmp_0),
                                                                alignment: _descriptor_8.alignment() }
                                                                .value
                                                            )) } },
                                       { ins: { cached: true, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(4n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(commitment_0),
                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(5n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_6.toValue(payload_0),
                                                                                              alignment: _descriptor_6.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(6n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_3.toValue(1),
                                                                                              alignment: _descriptor_3.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(7n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newMap(
                                                          new __compactRuntime.StateMap()
                                                        ).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    const tmp_1 = 0n;
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(8n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(tmp_1),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    return id_0;
  }
  _pvApprove_0(context, partialProofData, id_0, salt_0) {
    this._assertProposalActive_0(context, partialProofData, id_0);
    this._assertSigner_0(context, partialProofData, salt_0);
    const tag_0 = this._approvalTag_0(context, partialProofData, id_0, salt_0);
    __compactRuntime.assert(!_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                       partialProofData,
                                                                                       [
                                                                                        { dup: { n: 0 } },
                                                                                        { idx: { cached: false,
                                                                                                 pushPath: false,
                                                                                                 path: [
                                                                                                        { tag: 'value',
                                                                                                          value: { value: _descriptor_5.toValue(7n),
                                                                                                                   alignment: _descriptor_5.alignment() } },
                                                                                                        { tag: 'value',
                                                                                                          value: { value: _descriptor_2.toValue(id_0),
                                                                                                                   alignment: _descriptor_2.alignment() } }] } },
                                                                                        { push: { storage: false,
                                                                                                  value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(tag_0),
                                                                                                                                               alignment: _descriptor_0.alignment() }).encode() } },
                                                                                        'member',
                                                                                        { popeq: { cached: true,
                                                                                                   result: undefined } }]).value),
                            'PrivateVault: already approved');
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(7n),
                                                                  alignment: _descriptor_5.alignment() } },
                                                       { tag: 'value',
                                                         value: { value: _descriptor_2.toValue(id_0),
                                                                  alignment: _descriptor_2.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(tag_0),
                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newNull().encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 2 } }]);
    const newCount_0 = ((t1) => {
                         if (t1 > 255n) {
                           throw new __compactRuntime.CompactError('PrivateVault.compact line 149 char 20: cast from Field or Uint value to smaller Uint value failed: ' + t1 + ' is greater than 255');
                         }
                         return t1;
                       })(_descriptor_5.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                    partialProofData,
                                                                                    [
                                                                                     { dup: { n: 0 } },
                                                                                     { idx: { cached: false,
                                                                                              pushPath: false,
                                                                                              path: [
                                                                                                     { tag: 'value',
                                                                                                       value: { value: _descriptor_5.toValue(8n),
                                                                                                                alignment: _descriptor_5.alignment() } }] } },
                                                                                     { idx: { cached: false,
                                                                                              pushPath: false,
                                                                                              path: [
                                                                                                     { tag: 'value',
                                                                                                       value: { value: _descriptor_2.toValue(id_0),
                                                                                                                alignment: _descriptor_2.alignment() } }] } },
                                                                                     { popeq: { cached: false,
                                                                                                result: undefined } }]).value)
                          +
                          1n);
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(8n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(newCount_0),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    return [];
  }
  _pvRevokeApproval_0(context, partialProofData, id_0, salt_0) {
    this._assertProposalActive_0(context, partialProofData, id_0);
    this._assertSigner_0(context, partialProofData, salt_0);
    const tag_0 = this._approvalTag_0(context, partialProofData, id_0, salt_0);
    __compactRuntime.assert(_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                      partialProofData,
                                                                                      [
                                                                                       { dup: { n: 0 } },
                                                                                       { idx: { cached: false,
                                                                                                pushPath: false,
                                                                                                path: [
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_5.toValue(7n),
                                                                                                                  alignment: _descriptor_5.alignment() } },
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_2.toValue(id_0),
                                                                                                                  alignment: _descriptor_2.alignment() } }] } },
                                                                                       { push: { storage: false,
                                                                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(tag_0),
                                                                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                                                                       'member',
                                                                                       { popeq: { cached: true,
                                                                                                  result: undefined } }]).value),
                            'PrivateVault: not approved');
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(7n),
                                                                  alignment: _descriptor_5.alignment() } },
                                                       { tag: 'value',
                                                         value: { value: _descriptor_2.toValue(id_0),
                                                                  alignment: _descriptor_2.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(tag_0),
                                                                                              alignment: _descriptor_0.alignment() }).encode() } },
                                       { rem: { cached: false } },
                                       { ins: { cached: true, n: 2 } }]);
    let t_0;
    const newCount_0 = (t_0 = _descriptor_5.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                        partialProofData,
                                                                                        [
                                                                                         { dup: { n: 0 } },
                                                                                         { idx: { cached: false,
                                                                                                  pushPath: false,
                                                                                                  path: [
                                                                                                         { tag: 'value',
                                                                                                           value: { value: _descriptor_5.toValue(8n),
                                                                                                                    alignment: _descriptor_5.alignment() } }] } },
                                                                                         { idx: { cached: false,
                                                                                                  pushPath: false,
                                                                                                  path: [
                                                                                                         { tag: 'value',
                                                                                                           value: { value: _descriptor_2.toValue(id_0),
                                                                                                                    alignment: _descriptor_2.alignment() } }] } },
                                                                                         { popeq: { cached: false,
                                                                                                    result: undefined } }]).value),
                        (__compactRuntime.assert(t_0 >= 1n,
                                                 'result of subtraction would be negative'),
                         t_0 - 1n));
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(8n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_5.toValue(newCount_0),
                                                                                              alignment: _descriptor_5.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    return [];
  }
  _pvExecute_0(context,
               partialProofData,
               id_0,
               recipientIsContract_0,
               recipient_0,
               color_0,
               amount_0,
               nonce_0,
               threshold_0,
               thresholdSalt_0)
  {
    this._assertProposalActive_0(context, partialProofData, id_0);
    const expected_0 = this._proposalCommitment_0(recipientIsContract_0,
                                                  recipient_0,
                                                  color_0,
                                                  amount_0,
                                                  nonce_0);
    __compactRuntime.assert(this._equal_4(_descriptor_0.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                                    partialProofData,
                                                                                                    [
                                                                                                     { dup: { n: 0 } },
                                                                                                     { idx: { cached: false,
                                                                                                              pushPath: false,
                                                                                                              path: [
                                                                                                                     { tag: 'value',
                                                                                                                       value: { value: _descriptor_5.toValue(4n),
                                                                                                                                alignment: _descriptor_5.alignment() } }] } },
                                                                                                     { idx: { cached: false,
                                                                                                              pushPath: false,
                                                                                                              path: [
                                                                                                                     { tag: 'value',
                                                                                                                       value: { value: _descriptor_2.toValue(id_0),
                                                                                                                                alignment: _descriptor_2.alignment() } }] } },
                                                                                                     { popeq: { cached: false,
                                                                                                                result: undefined } }]).value),
                                          expected_0),
                            'PrivateVault: preimage does not match proposal');
    __compactRuntime.assert(threshold_0 >= 1n,
                            'PrivateVault: threshold below one');
    __compactRuntime.assert(this._equal_5(this._thresholdCommitment_0(threshold_0,
                                                                      thresholdSalt_0),
                                          _descriptor_0.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                                    partialProofData,
                                                                                                    [
                                                                                                     { dup: { n: 0 } },
                                                                                                     { idx: { cached: false,
                                                                                                              pushPath: false,
                                                                                                              path: [
                                                                                                                     { tag: 'value',
                                                                                                                       value: { value: _descriptor_5.toValue(2n),
                                                                                                                                alignment: _descriptor_5.alignment() } }] } },
                                                                                                     { popeq: { cached: false,
                                                                                                                result: undefined } }]).value)),
                            'PrivateVault: threshold preimage mismatch');
    let t_0;
    __compactRuntime.assert((t_0 = _descriptor_5.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                             partialProofData,
                                                                                             [
                                                                                              { dup: { n: 0 } },
                                                                                              { idx: { cached: false,
                                                                                                       pushPath: false,
                                                                                                       path: [
                                                                                                              { tag: 'value',
                                                                                                                value: { value: _descriptor_5.toValue(8n),
                                                                                                                         alignment: _descriptor_5.alignment() } }] } },
                                                                                              { idx: { cached: false,
                                                                                                       pushPath: false,
                                                                                                       path: [
                                                                                                              { tag: 'value',
                                                                                                                value: { value: _descriptor_2.toValue(id_0),
                                                                                                                         alignment: _descriptor_2.alignment() } }] } },
                                                                                              { popeq: { cached: false,
                                                                                                         result: undefined } }]).value),
                             t_0 >= threshold_0),
                            'PrivateVault: approvals below threshold');
    if (recipientIsContract_0) {
      this.__send_0(context,
                    partialProofData,
                    this._left_1({ bytes: recipient_0 }),
                    color_0,
                    amount_0);
    } else {
      this.__send_0(context,
                    partialProofData,
                    this._right_0({ bytes: recipient_0 }),
                    color_0,
                    amount_0);
    }
    __compactRuntime.queryLedgerState(context,
                                      partialProofData,
                                      [
                                       { idx: { cached: false,
                                                pushPath: true,
                                                path: [
                                                       { tag: 'value',
                                                         value: { value: _descriptor_5.toValue(6n),
                                                                  alignment: _descriptor_5.alignment() } }] } },
                                       { push: { storage: false,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                       { push: { storage: true,
                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_3.toValue(2),
                                                                                              alignment: _descriptor_3.alignment() }).encode() } },
                                       { ins: { cached: false, n: 1 } },
                                       { ins: { cached: true, n: 1 } }]);
    return [];
  }
  _assertSigner_0(context, partialProofData, salt_0) {
    const me_0 = this._signerCommitment_0(this._ownPublicKey_0(context,
                                                               partialProofData).bytes,
                                          salt_0);
    const roster_0 = _descriptor_7.fromValue(__compactRuntime.queryLedgerState(context,
                                                                               partialProofData,
                                                                               [
                                                                                { dup: { n: 0 } },
                                                                                { idx: { cached: false,
                                                                                         pushPath: false,
                                                                                         path: [
                                                                                                { tag: 'value',
                                                                                                  value: { value: _descriptor_5.toValue(1n),
                                                                                                           alignment: _descriptor_5.alignment() } }] } },
                                                                                { popeq: { cached: false,
                                                                                           result: undefined } }]).value);
    const found_0 = this._equal_6(roster_0[0], me_0)
                    ||
                    this._equal_7(roster_0[1], me_0)
                    ||
                    this._equal_8(roster_0[2], me_0)
                    ||
                    this._equal_9(roster_0[3], me_0)
                    ||
                    this._equal_10(roster_0[4], me_0);
    __compactRuntime.assert(found_0, 'PrivateVault: not a signer');
    return [];
  }
  _approvalTag_0(context, partialProofData, id_0, salt_0) {
    return this._persistentHash_0({ pk:
                                      this._ownPublicKey_0(context,
                                                           partialProofData).bytes,
                                    salt: salt_0,
                                    id: id_0,
                                    domain:
                                      new Uint8Array([112, 118, 97, 117, 108, 116, 58, 97, 112, 112, 114, 111, 118, 97, 108, 58, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]) });
  }
  _signerCommitment_0(pk_0, salt_0) {
    return this._persistentHash_1({ pk: pk_0,
                                    salt: salt_0,
                                    domain:
                                      new Uint8Array([112, 118, 97, 117, 108, 116, 58, 115, 105, 103, 110, 101, 114, 58, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]) });
  }
  _thresholdCommitment_0(threshold_0, salt_0) {
    return this._persistentHash_2({ threshold: threshold_0,
                                    salt: salt_0,
                                    domain:
                                      new Uint8Array([112, 118, 97, 117, 108, 116, 58, 116, 104, 114, 101, 115, 104, 111, 108, 100, 58, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]) });
  }
  _proposalCommitment_0(recipientIsContract_0,
                        recipient_0,
                        color_0,
                        amount_0,
                        nonce_0)
  {
    return this._persistentHash_3({ recipientIsContract: recipientIsContract_0,
                                    recipient: recipient_0,
                                    color: color_0,
                                    amount: amount_0,
                                    nonce: nonce_0,
                                    domain:
                                      new Uint8Array([112, 118, 97, 117, 108, 116, 58, 112, 114, 111, 112, 111, 115, 97, 108, 58, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]) });
  }
  _assertProposalActive_0(context, partialProofData, id_0) {
    __compactRuntime.assert(_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                      partialProofData,
                                                                                      [
                                                                                       { dup: { n: 0 } },
                                                                                       { idx: { cached: false,
                                                                                                pushPath: false,
                                                                                                path: [
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_5.toValue(6n),
                                                                                                                  alignment: _descriptor_5.alignment() } }] } },
                                                                                       { push: { storage: false,
                                                                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                                                                       'member',
                                                                                       { popeq: { cached: true,
                                                                                                  result: undefined } }]).value),
                            'PrivateVault: proposal not found');
    __compactRuntime.assert(_descriptor_3.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                      partialProofData,
                                                                                      [
                                                                                       { dup: { n: 0 } },
                                                                                       { idx: { cached: false,
                                                                                                pushPath: false,
                                                                                                path: [
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_5.toValue(6n),
                                                                                                                  alignment: _descriptor_5.alignment() } }] } },
                                                                                       { idx: { cached: false,
                                                                                                pushPath: false,
                                                                                                path: [
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_2.toValue(id_0),
                                                                                                                  alignment: _descriptor_2.alignment() } }] } },
                                                                                       { popeq: { cached: false,
                                                                                                  result: undefined } }]).value)
                            ===
                            1,
                            'PrivateVault: proposal not active');
    return [];
  }
  _getProposalCount_0(context, partialProofData) {
    return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                     partialProofData,
                                                                     [
                                                                      { dup: { n: 0 } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_5.toValue(3n),
                                                                                                 alignment: _descriptor_5.alignment() } }] } },
                                                                      { popeq: { cached: true,
                                                                                 result: undefined } }]).value);
  }
  _getProposalCommitment_0(context, partialProofData, id_0) {
    __compactRuntime.assert(_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                      partialProofData,
                                                                                      [
                                                                                       { dup: { n: 0 } },
                                                                                       { idx: { cached: false,
                                                                                                pushPath: false,
                                                                                                path: [
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_5.toValue(4n),
                                                                                                                  alignment: _descriptor_5.alignment() } }] } },
                                                                                       { push: { storage: false,
                                                                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                                                                       'member',
                                                                                       { popeq: { cached: true,
                                                                                                  result: undefined } }]).value),
                            'PrivateVault: proposal not found');
    return _descriptor_0.fromValue(__compactRuntime.queryLedgerState(context,
                                                                     partialProofData,
                                                                     [
                                                                      { dup: { n: 0 } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_5.toValue(4n),
                                                                                                 alignment: _descriptor_5.alignment() } }] } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_2.toValue(id_0),
                                                                                                 alignment: _descriptor_2.alignment() } }] } },
                                                                      { popeq: { cached: false,
                                                                                 result: undefined } }]).value);
  }
  _getProposalPayload_0(context, partialProofData, id_0) {
    __compactRuntime.assert(_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                      partialProofData,
                                                                                      [
                                                                                       { dup: { n: 0 } },
                                                                                       { idx: { cached: false,
                                                                                                pushPath: false,
                                                                                                path: [
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_5.toValue(5n),
                                                                                                                  alignment: _descriptor_5.alignment() } }] } },
                                                                                       { push: { storage: false,
                                                                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                                                                       'member',
                                                                                       { popeq: { cached: true,
                                                                                                  result: undefined } }]).value),
                            'PrivateVault: proposal not found');
    return _descriptor_6.fromValue(__compactRuntime.queryLedgerState(context,
                                                                     partialProofData,
                                                                     [
                                                                      { dup: { n: 0 } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_5.toValue(5n),
                                                                                                 alignment: _descriptor_5.alignment() } }] } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_2.toValue(id_0),
                                                                                                 alignment: _descriptor_2.alignment() } }] } },
                                                                      { popeq: { cached: false,
                                                                                 result: undefined } }]).value);
  }
  _getProposalStatus_0(context, partialProofData, id_0) {
    __compactRuntime.assert(_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                                      partialProofData,
                                                                                      [
                                                                                       { dup: { n: 0 } },
                                                                                       { idx: { cached: false,
                                                                                                pushPath: false,
                                                                                                path: [
                                                                                                       { tag: 'value',
                                                                                                         value: { value: _descriptor_5.toValue(6n),
                                                                                                                  alignment: _descriptor_5.alignment() } }] } },
                                                                                       { push: { storage: false,
                                                                                                 value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                                                                              alignment: _descriptor_2.alignment() }).encode() } },
                                                                                       'member',
                                                                                       { popeq: { cached: true,
                                                                                                  result: undefined } }]).value),
                            'PrivateVault: proposal not found');
    return _descriptor_3.fromValue(__compactRuntime.queryLedgerState(context,
                                                                     partialProofData,
                                                                     [
                                                                      { dup: { n: 0 } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_5.toValue(6n),
                                                                                                 alignment: _descriptor_5.alignment() } }] } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_2.toValue(id_0),
                                                                                                 alignment: _descriptor_2.alignment() } }] } },
                                                                      { popeq: { cached: false,
                                                                                 result: undefined } }]).value);
  }
  _getApprovalCount_0(context, partialProofData, id_0) {
    if (!_descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                   partialProofData,
                                                                   [
                                                                    { dup: { n: 0 } },
                                                                    { idx: { cached: false,
                                                                             pushPath: false,
                                                                             path: [
                                                                                    { tag: 'value',
                                                                                      value: { value: _descriptor_5.toValue(8n),
                                                                                               alignment: _descriptor_5.alignment() } }] } },
                                                                    { push: { storage: false,
                                                                              value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(id_0),
                                                                                                                           alignment: _descriptor_2.alignment() }).encode() } },
                                                                    'member',
                                                                    { popeq: { cached: true,
                                                                               result: undefined } }]).value))
    {
      return 0n;
    } else {
      return _descriptor_5.fromValue(__compactRuntime.queryLedgerState(context,
                                                                       partialProofData,
                                                                       [
                                                                        { dup: { n: 0 } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_5.toValue(8n),
                                                                                                   alignment: _descriptor_5.alignment() } }] } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_2.toValue(id_0),
                                                                                                   alignment: _descriptor_2.alignment() } }] } },
                                                                        { popeq: { cached: false,
                                                                                   result: undefined } }]).value);
    }
  }
  _getUnshieldedBalance_0(context, partialProofData, color_0) {
    return this._getTokenBalance_0(context, partialProofData, color_0);
  }
  _getThresholdCommitment_0(context, partialProofData) {
    return _descriptor_0.fromValue(__compactRuntime.queryLedgerState(context,
                                                                     partialProofData,
                                                                     [
                                                                      { dup: { n: 0 } },
                                                                      { idx: { cached: false,
                                                                               pushPath: false,
                                                                               path: [
                                                                                      { tag: 'value',
                                                                                        value: { value: _descriptor_5.toValue(2n),
                                                                                                 alignment: _descriptor_5.alignment() } }] } },
                                                                      { popeq: { cached: false,
                                                                                 result: undefined } }]).value);
  }
  _equal_0(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _folder_0(context, partialProofData, f, x, a0) {
    for (let i = 0; i < 5; i++) { x = f(context, partialProofData, x, a0[i]); }
    return x;
  }
  _equal_1(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_2(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_3(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_4(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_5(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_6(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_7(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_8(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_9(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
  _equal_10(x0, y0) {
    if (!x0.every((x, i) => y0[i] === x)) { return false; }
    return true;
  }
}
export function ledger(stateOrChargedState) {
  const state = stateOrChargedState instanceof __compactRuntime.StateValue ? stateOrChargedState : stateOrChargedState.state;
  const chargedState = stateOrChargedState instanceof __compactRuntime.StateValue ? new __compactRuntime.ChargedState(stateOrChargedState) : stateOrChargedState;
  const context = {
    currentQueryContext: new __compactRuntime.QueryContext(chargedState, __compactRuntime.dummyContractAddress()),
    costModel: __compactRuntime.CostModel.initialCostModel()
  };
  const partialProofData = {
    input: { value: [], alignment: [] },
    output: undefined,
    publicTranscript: [],
    privateTranscriptOutputs: []
  };
  return {
    get _signerRoster() {
      return _descriptor_7.fromValue(__compactRuntime.queryLedgerState(context,
                                                                       partialProofData,
                                                                       [
                                                                        { dup: { n: 0 } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_5.toValue(1n),
                                                                                                   alignment: _descriptor_5.alignment() } }] } },
                                                                        { popeq: { cached: false,
                                                                                   result: undefined } }]).value);
    },
    get _thresholdCommitment() {
      return _descriptor_0.fromValue(__compactRuntime.queryLedgerState(context,
                                                                       partialProofData,
                                                                       [
                                                                        { dup: { n: 0 } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_5.toValue(2n),
                                                                                                   alignment: _descriptor_5.alignment() } }] } },
                                                                        { popeq: { cached: false,
                                                                                   result: undefined } }]).value);
    },
    get _nextProposalId() {
      return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                       partialProofData,
                                                                       [
                                                                        { dup: { n: 0 } },
                                                                        { idx: { cached: false,
                                                                                 pushPath: false,
                                                                                 path: [
                                                                                        { tag: 'value',
                                                                                          value: { value: _descriptor_5.toValue(3n),
                                                                                                   alignment: _descriptor_5.alignment() } }] } },
                                                                        { popeq: { cached: true,
                                                                                   result: undefined } }]).value);
    },
    _proposalCommitment: {
      isEmpty(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`isEmpty: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(4n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(0n),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'eq',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      size(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`size: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(4n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      member(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`member: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('member',
                                     'argument 1',
                                     'PrivateVault.compact line 82 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(4n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(key_0),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'member',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      lookup(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`lookup: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('lookup',
                                     'argument 1',
                                     'PrivateVault.compact line 82 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_0.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(4n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_2.toValue(key_0),
                                                                                                     alignment: _descriptor_2.alignment() } }] } },
                                                                          { popeq: { cached: false,
                                                                                     result: undefined } }]).value);
      },
      [Symbol.iterator](...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`iter: expected 0 arguments, received ${args_0.length}`);
        }
        const self_0 = state.asArray()[4];
        return self_0.asMap().keys().map(  (key) => {    const value = self_0.asMap().get(key).asCell();    return [      _descriptor_2.fromValue(key.value),      _descriptor_0.fromValue(value.value)    ];  })[Symbol.iterator]();
      }
    },
    _proposalPayload: {
      isEmpty(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`isEmpty: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(5n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(0n),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'eq',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      size(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`size: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(5n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      member(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`member: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('member',
                                     'argument 1',
                                     'PrivateVault.compact line 85 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(5n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(key_0),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'member',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      lookup(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`lookup: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('lookup',
                                     'argument 1',
                                     'PrivateVault.compact line 85 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_6.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(5n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_2.toValue(key_0),
                                                                                                     alignment: _descriptor_2.alignment() } }] } },
                                                                          { popeq: { cached: false,
                                                                                     result: undefined } }]).value);
      },
      [Symbol.iterator](...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`iter: expected 0 arguments, received ${args_0.length}`);
        }
        const self_0 = state.asArray()[5];
        return self_0.asMap().keys().map(  (key) => {    const value = self_0.asMap().get(key).asCell();    return [      _descriptor_2.fromValue(key.value),      _descriptor_6.fromValue(value.value)    ];  })[Symbol.iterator]();
      }
    },
    _proposalStatus: {
      isEmpty(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`isEmpty: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(6n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(0n),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'eq',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      size(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`size: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(6n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      member(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`member: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('member',
                                     'argument 1',
                                     'PrivateVault.compact line 86 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(6n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(key_0),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'member',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      lookup(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`lookup: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('lookup',
                                     'argument 1',
                                     'PrivateVault.compact line 86 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_3.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(6n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_2.toValue(key_0),
                                                                                                     alignment: _descriptor_2.alignment() } }] } },
                                                                          { popeq: { cached: false,
                                                                                     result: undefined } }]).value);
      },
      [Symbol.iterator](...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`iter: expected 0 arguments, received ${args_0.length}`);
        }
        const self_0 = state.asArray()[6];
        return self_0.asMap().keys().map(  (key) => {    const value = self_0.asMap().get(key).asCell();    return [      _descriptor_2.fromValue(key.value),      _descriptor_3.fromValue(value.value)    ];  })[Symbol.iterator]();
      }
    },
    _approvalTags: {
      isEmpty(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`isEmpty: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(7n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(0n),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'eq',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      size(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`size: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(7n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      member(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`member: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('member',
                                     'argument 1',
                                     'PrivateVault.compact line 88 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(7n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(key_0),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'member',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      lookup(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`lookup: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('lookup',
                                     'argument 1',
                                     'PrivateVault.compact line 88 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        if (state.asArray()[7].asMap().get({ value: _descriptor_2.toValue(key_0),
                                             alignment: _descriptor_2.alignment() }) === undefined) {
          throw new __compactRuntime.CompactError(`Map value undefined for ${key_0}`);
        }
        return {
          isEmpty(...args_1) {
            if (args_1.length !== 0) {
              throw new __compactRuntime.CompactError(`isEmpty: expected 0 arguments, received ${args_1.length}`);
            }
            return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                             partialProofData,
                                                                             [
                                                                              { dup: { n: 0 } },
                                                                              { idx: { cached: false,
                                                                                       pushPath: false,
                                                                                       path: [
                                                                                              { tag: 'value',
                                                                                                value: { value: _descriptor_5.toValue(7n),
                                                                                                         alignment: _descriptor_5.alignment() } },
                                                                                              { tag: 'value',
                                                                                                value: { value: _descriptor_2.toValue(key_0),
                                                                                                         alignment: _descriptor_2.alignment() } }] } },
                                                                              'size',
                                                                              { push: { storage: false,
                                                                                        value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(0n),
                                                                                                                                     alignment: _descriptor_2.alignment() }).encode() } },
                                                                              'eq',
                                                                              { popeq: { cached: true,
                                                                                         result: undefined } }]).value);
          },
          size(...args_1) {
            if (args_1.length !== 0) {
              throw new __compactRuntime.CompactError(`size: expected 0 arguments, received ${args_1.length}`);
            }
            return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                             partialProofData,
                                                                             [
                                                                              { dup: { n: 0 } },
                                                                              { idx: { cached: false,
                                                                                       pushPath: false,
                                                                                       path: [
                                                                                              { tag: 'value',
                                                                                                value: { value: _descriptor_5.toValue(7n),
                                                                                                         alignment: _descriptor_5.alignment() } },
                                                                                              { tag: 'value',
                                                                                                value: { value: _descriptor_2.toValue(key_0),
                                                                                                         alignment: _descriptor_2.alignment() } }] } },
                                                                              'size',
                                                                              { popeq: { cached: true,
                                                                                         result: undefined } }]).value);
          },
          member(...args_1) {
            if (args_1.length !== 1) {
              throw new __compactRuntime.CompactError(`member: expected 1 argument, received ${args_1.length}`);
            }
            const elem_0 = args_1[0];
            if (!(elem_0.buffer instanceof ArrayBuffer && elem_0.BYTES_PER_ELEMENT === 1 && elem_0.length === 32)) {
              __compactRuntime.typeError('member',
                                         'argument 1',
                                         'PrivateVault.compact line 88 char 44',
                                         'Bytes<32>',
                                         elem_0)
            }
            return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                             partialProofData,
                                                                             [
                                                                              { dup: { n: 0 } },
                                                                              { idx: { cached: false,
                                                                                       pushPath: false,
                                                                                       path: [
                                                                                              { tag: 'value',
                                                                                                value: { value: _descriptor_5.toValue(7n),
                                                                                                         alignment: _descriptor_5.alignment() } },
                                                                                              { tag: 'value',
                                                                                                value: { value: _descriptor_2.toValue(key_0),
                                                                                                         alignment: _descriptor_2.alignment() } }] } },
                                                                              { push: { storage: false,
                                                                                        value: __compactRuntime.StateValue.newCell({ value: _descriptor_0.toValue(elem_0),
                                                                                                                                     alignment: _descriptor_0.alignment() }).encode() } },
                                                                              'member',
                                                                              { popeq: { cached: true,
                                                                                         result: undefined } }]).value);
          },
          [Symbol.iterator](...args_1) {
            if (args_1.length !== 0) {
              throw new __compactRuntime.CompactError(`iter: expected 0 arguments, received ${args_1.length}`);
            }
            const self_0 = state.asArray()[7].asMap().get({ value: _descriptor_2.toValue(key_0),
                                                            alignment: _descriptor_2.alignment() });
            return self_0.asMap().keys().map((elem) => _descriptor_0.fromValue(elem.value))[Symbol.iterator]();
          }
        }
      }
    },
    _approvalCount: {
      isEmpty(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`isEmpty: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(8n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(0n),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'eq',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      size(...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`size: expected 0 arguments, received ${args_0.length}`);
        }
        return _descriptor_2.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(8n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          'size',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      member(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`member: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('member',
                                     'argument 1',
                                     'PrivateVault.compact line 89 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_4.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(8n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { push: { storage: false,
                                                                                    value: __compactRuntime.StateValue.newCell({ value: _descriptor_2.toValue(key_0),
                                                                                                                                 alignment: _descriptor_2.alignment() }).encode() } },
                                                                          'member',
                                                                          { popeq: { cached: true,
                                                                                     result: undefined } }]).value);
      },
      lookup(...args_0) {
        if (args_0.length !== 1) {
          throw new __compactRuntime.CompactError(`lookup: expected 1 argument, received ${args_0.length}`);
        }
        const key_0 = args_0[0];
        if (!(typeof(key_0) === 'bigint' && key_0 >= 0n && key_0 <= 18446744073709551615n)) {
          __compactRuntime.typeError('lookup',
                                     'argument 1',
                                     'PrivateVault.compact line 89 char 1',
                                     'Uint<0..18446744073709551616>',
                                     key_0)
        }
        return _descriptor_5.fromValue(__compactRuntime.queryLedgerState(context,
                                                                         partialProofData,
                                                                         [
                                                                          { dup: { n: 0 } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_5.toValue(8n),
                                                                                                     alignment: _descriptor_5.alignment() } }] } },
                                                                          { idx: { cached: false,
                                                                                   pushPath: false,
                                                                                   path: [
                                                                                          { tag: 'value',
                                                                                            value: { value: _descriptor_2.toValue(key_0),
                                                                                                     alignment: _descriptor_2.alignment() } }] } },
                                                                          { popeq: { cached: false,
                                                                                     result: undefined } }]).value);
      },
      [Symbol.iterator](...args_0) {
        if (args_0.length !== 0) {
          throw new __compactRuntime.CompactError(`iter: expected 0 arguments, received ${args_0.length}`);
        }
        const self_0 = state.asArray()[8];
        return self_0.asMap().keys().map(  (key) => {    const value = self_0.asMap().get(key).asCell();    return [      _descriptor_2.fromValue(key.value),      _descriptor_5.fromValue(value.value)    ];  })[Symbol.iterator]();
      }
    }
  };
}
const _emptyContext = {
  currentQueryContext: new __compactRuntime.QueryContext(new __compactRuntime.ContractState().data, __compactRuntime.dummyContractAddress())
};
const _dummyContract = new Contract({ });
export const pureCircuits = {
  signerCommitment: (...args_0) => {
    if (args_0.length !== 2) {
      throw new __compactRuntime.CompactError(`signerCommitment: expected 2 arguments (as invoked from Typescript), received ${args_0.length}`);
    }
    const pk_0 = args_0[0];
    const salt_0 = args_0[1];
    if (!(pk_0.buffer instanceof ArrayBuffer && pk_0.BYTES_PER_ELEMENT === 1 && pk_0.length === 32)) {
      __compactRuntime.typeError('signerCommitment',
                                 'argument 1',
                                 'PrivateVault.compact line 247 char 1',
                                 'Bytes<32>',
                                 pk_0)
    }
    if (!(salt_0.buffer instanceof ArrayBuffer && salt_0.BYTES_PER_ELEMENT === 1 && salt_0.length === 32)) {
      __compactRuntime.typeError('signerCommitment',
                                 'argument 2',
                                 'PrivateVault.compact line 247 char 1',
                                 'Bytes<32>',
                                 salt_0)
    }
    return _dummyContract._signerCommitment_0(pk_0, salt_0);
  },
  thresholdCommitment: (...args_0) => {
    if (args_0.length !== 2) {
      throw new __compactRuntime.CompactError(`thresholdCommitment: expected 2 arguments (as invoked from Typescript), received ${args_0.length}`);
    }
    const threshold_0 = args_0[0];
    const salt_0 = args_0[1];
    if (!(typeof(threshold_0) === 'bigint' && threshold_0 >= 0n && threshold_0 <= 255n)) {
      __compactRuntime.typeError('thresholdCommitment',
                                 'argument 1',
                                 'PrivateVault.compact line 255 char 1',
                                 'Uint<0..256>',
                                 threshold_0)
    }
    if (!(salt_0.buffer instanceof ArrayBuffer && salt_0.BYTES_PER_ELEMENT === 1 && salt_0.length === 32)) {
      __compactRuntime.typeError('thresholdCommitment',
                                 'argument 2',
                                 'PrivateVault.compact line 255 char 1',
                                 'Bytes<32>',
                                 salt_0)
    }
    return _dummyContract._thresholdCommitment_0(threshold_0, salt_0);
  },
  proposalCommitment: (...args_0) => {
    if (args_0.length !== 5) {
      throw new __compactRuntime.CompactError(`proposalCommitment: expected 5 arguments (as invoked from Typescript), received ${args_0.length}`);
    }
    const recipientIsContract_0 = args_0[0];
    const recipient_0 = args_0[1];
    const color_0 = args_0[2];
    const amount_0 = args_0[3];
    const nonce_0 = args_0[4];
    if (!(typeof(recipientIsContract_0) === 'boolean')) {
      __compactRuntime.typeError('proposalCommitment',
                                 'argument 1',
                                 'PrivateVault.compact line 263 char 1',
                                 'Boolean',
                                 recipientIsContract_0)
    }
    if (!(recipient_0.buffer instanceof ArrayBuffer && recipient_0.BYTES_PER_ELEMENT === 1 && recipient_0.length === 32)) {
      __compactRuntime.typeError('proposalCommitment',
                                 'argument 2',
                                 'PrivateVault.compact line 263 char 1',
                                 'Bytes<32>',
                                 recipient_0)
    }
    if (!(color_0.buffer instanceof ArrayBuffer && color_0.BYTES_PER_ELEMENT === 1 && color_0.length === 32)) {
      __compactRuntime.typeError('proposalCommitment',
                                 'argument 3',
                                 'PrivateVault.compact line 263 char 1',
                                 'Bytes<32>',
                                 color_0)
    }
    if (!(typeof(amount_0) === 'bigint' && amount_0 >= 0n && amount_0 <= 340282366920938463463374607431768211455n)) {
      __compactRuntime.typeError('proposalCommitment',
                                 'argument 4',
                                 'PrivateVault.compact line 263 char 1',
                                 'Uint<0..340282366920938463463374607431768211456>',
                                 amount_0)
    }
    if (!(nonce_0.buffer instanceof ArrayBuffer && nonce_0.BYTES_PER_ELEMENT === 1 && nonce_0.length === 32)) {
      __compactRuntime.typeError('proposalCommitment',
                                 'argument 5',
                                 'PrivateVault.compact line 263 char 1',
                                 'Bytes<32>',
                                 nonce_0)
    }
    return _dummyContract._proposalCommitment_0(recipientIsContract_0,
                                                recipient_0,
                                                color_0,
                                                amount_0,
                                                nonce_0);
  }
};
export const contractReferenceLocations =
  { tag: 'publicLedgerArray', indices: { } };
//# sourceMappingURL=index.js.map

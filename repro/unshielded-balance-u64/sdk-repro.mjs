// Reproduces: unshieldedBalanceLte type-checks a Uint<128> argument, but the runtime
// comparison decodes u64, so a value above u64::MAX fails at circuit execution.
//
// Run: npm install && node sdk-repro.mjs

import {
  createConstructorContext,
  createCircuitContext,
  dummyContractAddress,
} from '@midnight-ntwrk/compact-runtime';
import { Contract } from './src/managed/repro/contract/index.js';

const coinPublicKey = '00'.repeat(32);

const contract = new Contract({});
const ctorCtx = createConstructorContext(undefined, coinPublicKey);
const init = contract.initialState(ctorCtx);
const circuitCtx = createCircuitContext(
  dummyContractAddress(),
  coinPublicKey,
  init.currentContractState,
  init.currentPrivateState,
);

const color = new Uint8Array(32).fill(1);
contract.impureCircuits.checkOverflow(circuitCtx, color);

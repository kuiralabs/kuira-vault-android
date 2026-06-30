// Test-utils for the Vault simulator suite. Trimmed from OpenZeppelin's
// test-utils to depend ONLY on @midnight-ntwrk/compact-runtime — our signers are
// coin public keys and our recipients are user addresses, so we never need the
// ledger-v8 contract-address encoder (keeps the harness off any 8.1.0 dep).

import { encodeCoinPublicKey } from '@midnight-ntwrk/compact-runtime';

type Bytes = { bytes: Uint8Array };
type Either<A, B> = { is_left: boolean; left: A; right: B };

/** ASCII -> zero-left-padded hex of `len` chars (default 64 = 32 bytes). */
export const toHexPadded = (str: string, len = 64): string =>
  Buffer.from(str, 'ascii').toString('hex').padStart(len, '0');

/** A coin public key derived from a label — matches the simulator's `.as(label)`. */
export const encodeToPK = (label: string): Bytes => ({
  bytes: encodeCoinPublicKey(toHexPadded(label)),
});

const zeroBytes = (): Bytes => ({ bytes: new Uint8Array(32) });

/**
 * A signer identity as the contract stores it:
 * `Either<ZswapCoinPublicKey, ContractAddress>` with the coin-key (left) arm set.
 * The right arm is zeroed; the contract canonicalizes it.
 */
export const signer = (label: string): Either<Bytes, Bytes> => ({
  is_left: true,
  left: encodeToPK(label),
  right: zeroBytes(),
});

/** A 32-byte user-address value from a label. */
export const userAddressBytes = (label: string): Uint8Array =>
  Uint8Array.from(Buffer.from(toHexPadded(label), 'hex'));

/** ProposalManager.RecipientKind enum order. */
export const RecipientKind = {
  ShieldedUser: 0,
  UnshieldedUser: 1,
  Contract: 2,
} as const;

/** A ProposalManager `Recipient` struct for an unshielded user. */
export const unshieldedRecipient = (label: string): { kind: number; address: Uint8Array } => ({
  kind: RecipientKind.UnshieldedUser,
  address: userAddressBytes(label),
});

/** A 32-byte token color. */
export const color = (fill = 1): Uint8Array => new Uint8Array(32).fill(fill);

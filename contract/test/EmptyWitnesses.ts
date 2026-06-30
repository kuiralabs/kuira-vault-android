// The Vault's unshielded path is witness-free (identity is the ownPublicKey()
// builtin; the treasury/proposal/signer modules declare no witnesses). So the
// simulator runs with an empty private state and no witness implementations.

export type EmptyPrivateState = Record<string, never>;
export const EmptyPrivateState: EmptyPrivateState = {};
export const emptyWitnesses = () => ({});

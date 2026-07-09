package com.kuiralabs.vault.ui

import java.math.BigInteger

// Private Vault UI state — the selective-disclosure sibling of [VaultUiState].
//
//   NotReady      — SDK not bootstrapped yet.
//   ReadyToStart  — SDK ready, this device holds no membership → create or join.
//   Member        — this device holds membership material; render the (decrypted) vault.
//
// Unlike the public vault there is no "deposit-only non-signer" joined state: you reach Member
// only by creating or accepting an invite, i.e. you ARE a signer. The proposal contents here are
// DECRYPTED locally from the on-chain ciphertext with the vault viewing key — an outsider sees none
// of it. Status + approval counts come from public chain state; identities never do.
sealed interface PrivateVaultUiState {
    data object NotReady : PrivateVaultUiState
    data object ReadyToStart : PrivateVaultUiState
    data class Member(
        val address: String,
        val threshold: Int,
        val signerCount: Int,
        val treasuryBalance: BigInteger,
        val proposals: List<PrivateProposalView>,
        /** True if this device deployed the vault (shows the invites to share). */
        val isCreator: Boolean,
        /** Co-signer invites to share (creator only). */
        val invites: List<String>,
        /** Each invite's owner coin-key hex, index-aligned with [invites] (creator only). */
        val coSignerKeyHexes: List<String> = emptyList(),
        val refreshing: Boolean = false,
    ) : PrivateVaultUiState
}

/** A private withdrawal proposal, decrypted for a member. */
data class PrivateProposalView(
    val id: Long,
    val recipientAddress: String,
    val recipientIsMe: Boolean,
    val amount: BigInteger,
    val approvals: Int,
    val threshold: Int,
    val executed: Boolean,
    /** Whether THIS device approved — tracked locally (tags are unlinkable without the signer's
     *  secret salt, and not queryable). */
    val approvedByMe: Boolean,
    /** False if this device's viewing key couldn't decrypt this proposal (garbage/wrong-key). */
    val readable: Boolean = true,
) {
    val thresholdMet: Boolean get() = approvals >= threshold
}

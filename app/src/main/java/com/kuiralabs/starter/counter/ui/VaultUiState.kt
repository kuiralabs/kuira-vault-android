package com.kuiralabs.starter.counter.ui

import java.math.BigInteger

// Vault (multisig treasury) UI state. Mirrors the counter's three-branch machine:
//
//   NotReady       — SDK not bootstrapped yet (sigil/wallet panels above drive it).
//   ReadyToDeploy  — SDK ready, no Vault deployed on the current network.
//   Deployed       — a Vault is deployed; render treasury + proposals.
//
// The treasury balance and proposal details are tracked app-side (this app performs the
// deposits and creates the proposals, so it knows them); the per-proposal approval count
// is read live from the contract's `_approvalCount` ledger field, since that changes when
// OTHER signer wallets approve. A chain-read of balance/proposal details would need an
// SDK view-call path (follow-up).
sealed interface VaultUiState {
    data object NotReady : VaultUiState
    data object ReadyToDeploy : VaultUiState
    data class Deployed(
        val address: String,
        val threshold: Int,
        val signerCount: Int,
        val treasuryBalance: BigInteger,
        val proposals: List<ProposalView>,
        // Whether THIS wallet is a signer. Non-signers can only deposit (permissionless); propose
        // AND approve are both signer-gated (Signer_assertSigner in the contract).
        val isSigner: Boolean,
        // A chain refresh is in flight (balance/proposals being re-read).
        val refreshing: Boolean = false,
    ) : VaultUiState
}

/** A withdrawal proposal as shown in the UI. */
data class ProposalView(
    val id: Long,
    // The recipient re-encoded as a real Bech32m wallet address (not a raw hash tail), plus
    // whether it is THIS wallet — "1 NIGHT → mn_addr…xyz (your wallet)" reads; a hex tail doesn't.
    val recipientAddress: String,
    val recipientIsMe: Boolean,
    val amount: BigInteger,
    val approvals: Int,
    val threshold: Int,
    val executed: Boolean,
    // Whether THIS wallet already approved (from isApprovedBySigner) — gates the Approve
    // affordance so a signer is never guided into the contract's duplicate-approval assert.
    val approvedByMe: Boolean,
) {
    val thresholdMet: Boolean get() = approvals >= threshold
}

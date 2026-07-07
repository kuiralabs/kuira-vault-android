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
        // Whether THIS wallet is a signer (from isSigner) — if false, it can view but not approve.
        val canApprove: Boolean,
        // A chain refresh is in flight (balance/proposals being re-read).
        val refreshing: Boolean = false,
    ) : VaultUiState
}

/** A withdrawal proposal as shown in the UI. */
data class ProposalView(
    val id: Long,
    val recipientLabel: String,
    val recipientHashHex: String,
    val amount: BigInteger,
    val approvals: Int,
    val threshold: Int,
    val executed: Boolean,
) {
    val thresholdMet: Boolean get() = approvals >= threshold
    val settleable: Boolean get() = thresholdMet && !executed
}

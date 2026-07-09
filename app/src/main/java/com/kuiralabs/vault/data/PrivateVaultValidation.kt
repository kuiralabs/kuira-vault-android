package com.kuiralabs.vault.data

/**
 * Pure create-a-vault input validation, extracted from [com.kuiralabs.vault.ui.PrivateVaultViewModel]
 * so it's unit-testable without faking the SDK. This is where the confirmed bugs lived (co-signer
 * hex checks, threshold bounds, the signer-count cap), so it's worth pinning on its own.
 */
object PrivateVaultValidation {

    sealed interface CreatePlan {
        /** [coSignerKeys] are the parsed co-signers (32 bytes each), EXCLUDING the deployer. */
        data class Valid(val coSignerKeys: List<ByteArray>) : CreatePlan
        data class Invalid(val error: String) : CreatePlan
    }

    /**
     * Validate the co-signer key fields + threshold for a vault whose roster holds at most
     * [rosterMax] signers (deployer + co-signers). A blank field is dropped; a non-blank field that
     * isn't exactly 64 hex chars is rejected (silently substituting would deploy an unusable vault).
     */
    fun planCreate(coSignerHexKeys: List<String>, threshold: Int, rosterMax: Int): CreatePlan {
        val coSigners = mutableListOf<ByteArray>()
        for ((i, raw) in coSignerHexKeys.withIndex()) {
            val entry = raw.trim()
            if (entry.isEmpty()) continue
            if (entry.length != 64 || !entry.all { it in "0123456789abcdefABCDEF" }) {
                return CreatePlan.Invalid(
                    "Co-signer ${i + 2} key must be exactly 64 hex characters (got ${entry.length}) — " +
                        "fix it or leave the field blank.",
                )
            }
            coSigners += hexToBytes(entry)
        }
        val signerCount = 1 + coSigners.size // + the deployer
        if (signerCount > rosterMax) {
            return CreatePlan.Invalid(
                "A vault supports at most $rosterMax signers (you + ${rosterMax - 1} co-signers).",
            )
        }
        if (threshold !in 1..signerCount) {
            return CreatePlan.Invalid(
                "Threshold $threshold needs $threshold real signers but only $signerCount provided — " +
                    "add co-signer keys or lower the threshold.",
            )
        }
        return CreatePlan.Valid(coSigners)
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

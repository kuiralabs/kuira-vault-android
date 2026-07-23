package com.kuiralabs.vault

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuiralabs.vault.ui.MoneyPathResult
import com.midnight.kuira.core.compact.ContractCallException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the one bit of Money Path tab ViewModel logic with a real failure mode: classifying a
 * thrown deposit error. The risk is misclassifying the SDK's typed Layer-1
 * [ContractCallException.UnshieldedValueUnfunded] as a generic failure (or vice-versa) — which the
 * MoneyPathE2ETest, driving VaultContract directly, would never catch. Pure logic; no localnet.
 */
@RunWith(AndroidJUnit4::class)
class MoneyPathResultTest {

    @Test
    fun forError_mapsUnshieldedValueUnfunded_toTypedError() {
        val result = MoneyPathResult.forError(
            ContractCallException.UnshieldedValueUnfunded("no funding offer supplied"),
        )
        assertTrue("Layer-1 error must map to TypedError, got $result", result is MoneyPathResult.TypedError)
        assertEquals("no funding offer supplied", (result as MoneyPathResult.TypedError).detail)
    }

    @Test
    fun forError_mapsOtherThrowable_toFailure() {
        val result = MoneyPathResult.forError(IllegalStateException("node unreachable"))
        assertTrue("a generic error must map to Failure, got $result", result is MoneyPathResult.Failure)
        assertEquals("node unreachable", (result as MoneyPathResult.Failure).detail)
    }

    @Test
    fun forError_failureFallsBackToClassName_whenNoMessage() {
        val result = MoneyPathResult.forError(RuntimeException())
        assertTrue(result is MoneyPathResult.Failure)
        assertEquals("RuntimeException", (result as MoneyPathResult.Failure).detail)
    }
}

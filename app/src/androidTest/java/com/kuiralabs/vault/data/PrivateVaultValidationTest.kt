package com.kuiralabs.vault.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuiralabs.vault.data.PrivateVaultValidation.CreatePlan
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The create-a-vault input validation the ViewModel runs before deploying — extracted to be pure,
 * so it's tested here directly with no SDK fakes. This is where the confirmed bugs lived (the
 * co-signer hex check, threshold bounds, the signer-count cap), which is exactly why it's pinned.
 */
@RunWith(AndroidJUnit4::class)
class PrivateVaultValidationTest {

    private val ROSTER_MAX = 5
    private val KEY = "ab".repeat(32)  // a valid 64-hex co-signer key
    private val KEY2 = "cd".repeat(32)

    private fun plan(keys: List<String>, threshold: Int) =
        PrivateVaultValidation.planCreate(keys, threshold, ROSTER_MAX)

    @Test
    fun soloVault_noCoSigners_threshold1_isValid() {
        val p = plan(listOf("", ""), 1)
        assertTrue(p is CreatePlan.Valid)
        assertTrue("solo vault has no co-signers", (p as CreatePlan.Valid).coSignerKeys.isEmpty())
    }

    @Test
    fun twoCoSigners_threshold2_isValid_andParsesKeys() {
        val p = plan(listOf(KEY, KEY2), 2)
        assertTrue(p is CreatePlan.Valid)
        val keys = (p as CreatePlan.Valid).coSignerKeys
        assertEquals(2, keys.size)
        assertArrayEquals(ByteArray(32) { 0xAB.toByte() }, keys[0])
        assertArrayEquals(ByteArray(32) { 0xCD.toByte() }, keys[1])
    }

    @Test
    fun blankFieldsAreDropped_soCountIsRealSignersOnly() {
        // one real co-signer + one blank => 2 signers (deployer + 1); threshold 2 ok, threshold 3 not.
        assertTrue(plan(listOf(KEY, ""), 2) is CreatePlan.Valid)
        assertTrue(plan(listOf(KEY, ""), 3) is CreatePlan.Invalid)
    }

    @Test
    fun badHexCoSigner_isRejected() {
        assertTrue("63 chars", plan(listOf("ab".repeat(31) + "a", ""), 1) is CreatePlan.Invalid)
        assertTrue("non-hex char", plan(listOf("zz".repeat(32), ""), 1) is CreatePlan.Invalid)
        val err = (plan(listOf("xyz", ""), 1) as CreatePlan.Invalid).error
        assertTrue("names the offending field", err.contains("Co-signer 2"))
    }

    @Test
    fun thresholdAboveSignerCount_isRejected() {
        // deployer only (both blank) => 1 signer; threshold 2 impossible.
        assertTrue(plan(listOf("", ""), 2) is CreatePlan.Invalid)
    }

    @Test
    fun thresholdZero_isRejected() {
        assertTrue(plan(listOf(KEY, ""), 0) is CreatePlan.Invalid)
    }

    @Test
    fun tooManySigners_overRosterMax_isRejected() {
        // 5 co-signers + deployer = 6 > ROSTER_MAX(5).
        val fiveKeys = List(5) { "ab".repeat(32) }
        val p = plan(fiveKeys, 2)
        assertTrue(p is CreatePlan.Invalid)
        assertTrue((p as CreatePlan.Invalid).error.contains("at most $ROSTER_MAX"))
    }
}

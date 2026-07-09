package com.kuiralabs.starter.counter.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midnight.kuira.core.compact.MidnightConfig
import com.midnight.kuira.core.compact.MidnightContract
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

/**
 * The commitment-computation path the Private Vault deploy runs BEFORE any contract exists —
 * [MidnightContract.readLocal] against the real PrivateVault contract. It runs entirely locally
 * (QuickJS: `initialState()` then a pure circuit) with NO chain, NO wallet, NO localnet — so it's a
 * cheap on-device integration test, not an e2e.
 *
 * **The regression it pins (caught on-device, not by a test):** `readLocal` runs `initialState()`,
 * which VALIDATES the constructor arg SHAPES even though a pure circuit never touches state. The
 * PrivateVault constructor takes 2 args, so a handle built with NONE makes `initialState` throw
 * "expected 3 arguments … received 1" — the deploy crashed before submitting. The earlier SDK-level
 * readLocal test used the bboard fixture (a NO-ARG constructor), so it was blind to this. Here the
 * real 2-arg contract is exercised: `withConstructorArgs` proves the fix; `withoutConstructorArgs`
 * reproduces the exact failure.
 */
@RunWith(AndroidJUnit4::class)
class PrivateVaultReadLocalTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // readLocal never hits the chain or the wallet; the URLs only satisfy the config builder.
    private val config = MidnightConfig.Builder(context)
        .indexerUrl("http://10.0.2.2:8088/api/v4")
        .walletUrl("ws://10.0.2.2:8080")
        .networkId("undeployed")
        .build()

    // Shape-valid placeholder constructor args (Vector<5, Bytes<32>> + Bytes<32>), discarded by a
    // pure circuit but required to pass initialState()'s validation — exactly what deploy supplies.
    private fun ctorArgs(): List<Any?> = listOf(
        List(5) { i -> ByteArray(32) { (i + 1).toByte() } },
        ByteArray(32) { 0x7f },
    )

    private fun handle(withCtorArgs: Boolean): MidnightContract =
        MidnightContract.create(config) {
            name = "private-vault"
            contractJs = context.assets.open("private-vault-runtime/private-vault-contract.js")
            if (withCtorArgs) constructorArgs = ctorArgs()
        }

    private fun hexOf(json: String): String = json.trim().trim('"')

    private val pk = ByteArray(32) { (it + 1).toByte() }
    private val salt = ByteArray(32) { 0x11 }

    @Test
    fun signerCommitment_computesLocally_withConstructorArgs() = runBlocking {
        val hex = hexOf(handle(withCtorArgs = true).readLocal("signerCommitment", pk, salt))
        assertEquals("expected a 32-byte commitment, got: $hex", 64, hex.length)
        assertTrue("commitment must not be all zeros", hex.any { it != '0' })
    }

    @Test
    fun signerCommitment_isDeterministic_andInputSensitive() = runBlocking {
        val h = handle(withCtorArgs = true)
        val a = hexOf(h.readLocal("signerCommitment", pk, salt))
        assertEquals(a, hexOf(h.readLocal("signerCommitment", pk, salt)))
        val otherSalt = ByteArray(32) { 0x22 }
        assertNotEquals(a, hexOf(h.readLocal("signerCommitment", pk, otherSalt)))
    }

    @Test
    fun thresholdAndProposalCommitments_alsoCompute() = runBlocking {
        val h = handle(withCtorArgs = true)
        val th = hexOf(h.readLocal("thresholdCommitment", BigInteger.valueOf(2), salt))
        assertEquals(64, th.length)
        // proposalCommitment marshals mixed types: Boolean + 3×Bytes<32> + Uint<128>.
        val prop = hexOf(h.readLocal(
            "proposalCommitment",
            false, ByteArray(32) { 0x0a }, ByteArray(32) { 0x0b }, BigInteger.valueOf(400), ByteArray(32) { 0x0c },
        ))
        assertEquals(64, prop.length)
        assertNotEquals(th, prop)
    }

    @Test
    fun withoutConstructorArgs_reproducesTheExpectedArgumentsFailure() = runBlocking<Unit> {
        // The exact on-device bug: no ctor args -> initialState() rejects the arg count.
        assertThrows(Exception::class.java) {
            runBlocking { handle(withCtorArgs = false).readLocal("signerCommitment", pk, salt) }
        }
    }
}

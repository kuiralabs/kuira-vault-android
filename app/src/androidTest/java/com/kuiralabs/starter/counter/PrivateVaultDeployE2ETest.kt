package com.kuiralabs.starter.counter

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuiralabs.starter.counter.data.PrivateVaultContract
import com.kuiralabs.starter.counter.data.PrivateVaultCrypto
import com.midnight.kuira.core.compact.ContractCallException
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.core.crypto.address.Bech32m
import com.midnight.kuira.core.crypto.bip39.BIP39
import com.midnight.kuira.core.crypto.proving.ProvingMode
import com.midnight.kuira.core.network.MidnightNetwork
import com.midnight.kuira.sdk.MidnightSdk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

/**
 * On-chain e2e for the PRIVATE Vault — the capstone that ties the whole selective-disclosure stack
 * together on a real node: two distinct signer wallets run create → join(invite) → deposit →
 * propose → approve(to threshold) → execute, and then an OUTSIDER (no viewing key) proves the
 * privacy actually holds against the repo's own read tooling.
 *
 * Two gates, per the plan's "function + adversarial" model:
 *  - FUNCTION: the ceremony finalizes and the member reads (decrypted) match chain truth.
 *  - PRIVACY: the outsider CANNOT recover the recipient, amount, or a signer identity from the
 *    public ledger; only a viewing-key holder can. This is the disclosure matrix, proven on-chain.
 *
 * Funding crosses device→host via a logcat marker; tests SKIP (assumeTrue), never fail, when
 * localnet/funding is absent. First proposal id is 0 (the contract's Counter).
 */
@RunWith(AndroidJUnit4::class)
class PrivateVaultDeployE2ETest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val openSdks = mutableListOf<MidnightSdk>()

    @After
    fun tearDown() {
        openSdks.forEach { runCatching { it.close() } }
        openSdks.clear()
    }

    @Test
    fun private_full_flow_2of2_plus_adversarial_read() = runBlocking<Unit> {
        val alice = fundedSdk(nightWhole = 50)   // creator + signer 1
        // Bob is BUILT (his coin public key is needed for the roster) but NOT funded/synced yet —
        // funding him later keeps peak memory low during Alice's heavy deploy proof (contract
        // circuits prove LOCALLY; the private vault's are large). Matches the working interactive case.
        val bob = buildSdk()                     // co-signer 2 (funded after deploy, joins via invite)

        // ── Create a 2-of-2 private vault over Alice + Bob ──
        val deploy = PrivateVaultContract.deploy(
            context, alice,
            signerCoinPublicKeys = listOf(alice.coinPublicKey, bob.coinPublicKey),
            threshold = 2,
        ) { stage -> Log.i(TAG, "create stage: $stage") }
        val address = deploy.address
        assertTrue("deploy must return a 64-hex address, got '$address'",
            address.length == 64 && address.all { it in "0123456789abcdefABCDEF" })
        assertEquals("one co-signer invite (for Bob)", 1, deploy.coSignerInvites.size)
        Log.i(TAG, "Private vault deployed at $address")

        // ── Deposit 5 NIGHT (public transfer in Tier 1) ──
        assertTrue("deposit must finalize", deposit(alice, address, BigInteger.valueOf(5_000_000L)))

        // Now fund Bob (peak memory during the deploy proof is past).
        fundExisting(bob, nightWhole = 20)

        // ── Bob JOINS by decoding his invite — no chain identity of his revealed ──
        val bobInvite = PrivateVaultCrypto.decodeInvite(deploy.coSignerInvites[0])
        assertEquals("invite address", address, bobInvite.vaultAddress)
        assertEquals("invite threshold", 2, bobInvite.threshold)
        assertEquals("invite signer count", 2, bobInvite.signerCount)

        // ── Alice proposes a private withdrawal of 2 NIGHT to herself ──
        val recipientHash = Bech32m.decode(alice.walletAddress).second
        var proposeSubmitted = false
        PrivateVaultContract.proposeWithdrawal(
            context, alice, address,
            recipientAddressHash = recipientHash, color = ByteArray(32), amount = BigInteger.valueOf(2_000_000L),
            viewingKey = deploy.viewingKey, memberSalt = deploy.creatorSalt,
        ) { stage -> if (stage.toString().contains("Submitting")) proposeSubmitted = true }
        assertTrue("propose must reach submit", proposeSubmitted)

        val reader = PrivateVaultContract.buildReadHandle(context, alice, address)

        // ── Approve to threshold: Alice (creator salt) + Bob (invite salt) ──
        approveAs(alice, address, ID, deploy.creatorSalt)
        approveAs(bob, address, ID, bobInvite.memberSalt)
        assertEquals("approvals == 2", 2, jsonInt(reader.read("getApprovalCount", ID.toBig())))

        // Member snapshot (production read path) must DECRYPT to the exact proposal.
        val snap = PrivateVaultContract.loadSnapshot(reader, deploy.viewingKey, ByteArray(32))
        assertEquals("snapshot balance", BigInteger.valueOf(5_000_000L), snap.balance)
        assertEquals("one proposal", 1, snap.proposals.size)
        val mp = snap.proposals[0]
        assertTrue("proposal decrypted", mp.readable)
        assertEquals("decrypted recipient matches", recipientHash.toHex(), mp.recipientHashHex)
        assertEquals("decrypted amount matches", BigInteger.valueOf(2_000_000L), mp.amount)
        assertEquals("status ACTIVE pre-execute", PrivateVaultContract.STATUS_ACTIVE, mp.status)

        // ══ ADVERSARIAL: an outsider (no viewing key) can read PUBLIC state but recovers nothing ══
        val commitmentHex = jsonHex(reader.read("getProposalCommitment", ID.toBig()))
        assertEquals("commitment is 32 bytes", 64, commitmentHex.length)
        assertTrue("commitment must NOT contain the recipient hash",
            !commitmentHex.contains(recipientHash.toHex(), ignoreCase = true))
        // 2_000_000 = 0x1E8480 — the amount must not surface in the commitment either.
        assertTrue("commitment must NOT contain the amount bytes", !commitmentHex.contains("1e8480", ignoreCase = true))

        val payloadHex = jsonHex(reader.read("getProposalPayload", ID.toBig()))
        val payload = hexToBytes(payloadHex)
        // The ciphertext is on-chain and readable — but WITHOUT the viewing key it decrypts to nothing.
        val attackerKey = ByteArray(32) { 0x66 }
        assertThrows("outsider must NOT decrypt the payload", Exception::class.java) {
            PrivateVaultCrypto.decryptPreimage(attackerKey, payload)
        }
        // And no signer public key appears in the public roster.
        val rosterDump = reader.ledger().getRawOrNull("_signerRoster").toString()
        assertTrue("roster must NOT contain Alice's public key",
            !rosterDump.contains(alice.coinPublicKey.toHex(), ignoreCase = true))
        assertTrue("roster must NOT contain Bob's public key",
            !rosterDump.contains(bob.coinPublicKey.toHex(), ignoreCase = true))
        Log.i(TAG, "Adversarial read: outsider recovered NOTHING (commitment/ciphertext/roster all opaque)")

        // ── Execute (any member with the viewing key + threshold salt) ──
        var executeSubmitted = false
        PrivateVaultContract.execute(
            context, alice, address, proposalId = ID,
            viewingKey = deploy.viewingKey, threshold = 2, thresholdSalt = deploy.thresholdSalt,
        ) { stage -> if (stage.toString().contains("Submitting")) executeSubmitted = true }
        assertTrue("execute + withdrawal money path must finalize", executeSubmitted)

        assertEquals("status EXECUTED after execute", PrivateVaultContract.STATUS_EXECUTED,
            jsonInt(reader.read("getProposalStatus", ID.toBig())))
        assertEquals("treasury debited by the withdrawal", BigInteger.valueOf(3_000_000L),
            PrivateVaultContract.getUnshieldedBalance(reader, ByteArray(32)))
        Log.i(TAG, "Private vault flow complete — 2 NIGHT withdrawn, chain truth verified, privacy held")
    }

    private suspend fun approveAs(s: MidnightSdk, address: String, id: Long, salt: ByteArray) {
        var submitted = false
        PrivateVaultContract.approve(context, s, address, id, salt) { stage ->
            if (stage.toString().contains("Submitting")) submitted = true
        }
        assertTrue("approve must reach submit", submitted)
    }

    private suspend fun deposit(s: MidnightSdk, address: String, amount: BigInteger): Boolean {
        var reachedSubmit = false
        val deadline = System.currentTimeMillis() + 90_000
        while (System.currentTimeMillis() < deadline) {
            try {
                PrivateVaultContract.depositUnshielded(context, s, address, ByteArray(32), amount) { stage ->
                    val name = stage.toString()
                    if (name.contains("Submitting") || name.contains("Finalization")) reachedSubmit = true
                }
                break
            } catch (e: ContractCallException.StateFetchFailed) {
                Log.i(TAG, "contract not indexed yet; retrying deposit in 3s")
                delay(3_000)
            }
        }
        return reachedSubmit
    }

    /** Build a fresh REMOTE-proving wallet SDK — no funding yet (coin public key is available). */
    private fun buildSdk(): MidnightSdk {
        val seed = BIP39.mnemonicToSeed(BIP39.generateMnemonic(24), "")
        val s = MidnightSdk.Builder(context)
            .network(MidnightNetwork.UNDEPLOYED).seed(seed).provingMode(ProvingMode.REMOTE).build()
        openSdks += s
        seed.fill(0)
        ProvingKeyManager(context).installWalletKeysFromAssets()
        return s
    }

    /** Airdrop [nightWhole] NIGHT + dust to an already-built [s] (host services the logcat marker). */
    private suspend fun fundExisting(s: MidnightSdk, nightWhole: Int) {
        val address = s.walletAddress
        Log.i(FUND_TAG, "$FUND_MARKER addr=$address night=$nightWhole small=1 dust=true")

        val reachable = withTimeoutOrNull(15_000L) { s.wallet.balance() }
        assumeTrue("Localnet unreachable (indexer http://10.0.2.2:8088 healthy?)", reachable != null)

        val minNight = BigInteger.valueOf(nightWhole.toLong()).multiply(NIGHT_UNIT)
        val funded = runCatching { s.wallet.waitForFunding(minNight = minNight, timeoutMs = 5 * 60_000L) }.getOrNull()
        assumeTrue("Host must fund $address (>= $nightWhole NIGHT) — run via fund-vault-e2e.sh.", funded != null)

        if (!funded!!.dustRegistered) {
            runCatching { s.registerForDustGeneration() }
            val deadline = System.currentTimeMillis() + 3 * 60_000L
            while (System.currentTimeMillis() < deadline && !s.wallet.balance().dustRegistered) {
                delay(3_000L); runCatching { s.wallet.refresh() }
            }
            assumeTrue("Dust never registered for $address", s.wallet.balance().dustRegistered)
        }
    }

    private suspend fun fundedSdk(nightWhole: Int): MidnightSdk = buildSdk().also { fundExisting(it, nightWhole) }

    private fun Long.toBig(): BigInteger = BigInteger.valueOf(this)
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun jsonScalar(json: String): String = org.json.JSONTokener(json).nextValue().toString()
    private fun jsonInt(json: String): Int = jsonScalar(json).toInt()
    // A Bytes value from read() arrives either as a hex string (top-level) or a JSON int-array
    // (struct-nested) — decode both to hex (mirrors the app's VaultContract.bytesHex).
    private fun jsonHex(json: String): String = when (val v = org.json.JSONTokener(json).nextValue()) {
        is String -> v
        is org.json.JSONArray -> (0 until v.length()).joinToString("") { "%02x".format(v.getInt(it) and 0xFF) }
        else -> v.toString()
    }

    private companion object {
        const val TAG = "PrivateVaultE2E"
        const val FUND_TAG = "KuiraE2EFund"
        const val FUND_MARKER = "KUIRA_FUND_REQ"
        const val ID = 0L   // the contract's Counter starts at 0
        val NIGHT_UNIT: BigInteger = BigInteger.valueOf(1_000_000L)
    }
}

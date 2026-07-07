package com.kuiralabs.starter.counter

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuiralabs.starter.counter.data.VaultContract
import com.midnight.kuira.core.compact.ContractCallException
import com.midnight.kuira.core.crypto.address.Bech32m
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.core.crypto.bip39.BIP39
import com.midnight.kuira.core.crypto.proving.ProvingMode
import com.midnight.kuira.core.network.MidnightNetwork
import com.midnight.kuira.sdk.MidnightSdk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

/**
 * On-chain e2e for the Vault multisig — proves the SDK can deploy a contract with
 * CONSTRUCTOR ARGUMENTS (the alpha05 fix) on localnet, and that the OZ-composed
 * Vault (SignerManager + ProposalManager + UnshieldedTreasury) runs on our 8.0.3
 * engine. Closes #28.
 *
 * Funding crosses device→host via a logcat marker ([FUND_MARKER]); the host
 * harness `fund-vault-e2e.sh` airdrops NIGHT, dust registers on-device. Tests SKIP
 * (assumeTrue), never fail, when localnet/funding is absent.
 */
@RunWith(AndroidJUnit4::class)
class VaultDeployE2ETest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val openSdks = mutableListOf<MidnightSdk>()

    @After
    fun tearDown() {
        openSdks.forEach { runCatching { it.close() } }
        openSdks.clear()
    }

    @Test
    fun deploy_succeeds() = runBlocking {
        val s = fundedSdk(nightWhole = 50)

        val address = deployVault(s)

        Log.i(TAG, "Vault deployed at $address")
        assertTrue(
            "deploy must return a 64-hex contract address, got '$address'",
            address.length == 64 && address.all { it in "0123456789abcdefABCDEF" },
        )
    }

    @Test
    fun deposit_credits() = runBlocking {
        val s = fundedSdk(nightWhole = 50)

        val address = deployVault(s)
        Log.i(TAG, "Vault deployed at $address; depositing NIGHT into the treasury")

        // Deposit native NIGHT into the treasury. Exercises receiveUnshielded — funding a CONTRACT
        // with a real wallet UTXO (the UTXO->contract money path, #30).
        val reachedSubmit = deposit(s, address, BigInteger.valueOf(5_000_000L))

        assertTrue(
            "deposit must reach on-chain submission/finalization",
            reachedSubmit,
        )
    }

    /**
     * Full 2-of-3 multisig governance on a real node: three distinct signer wallets deploy a Vault,
     * fund it, then propose → approve (to threshold) → execute a withdrawal. Each circuit's asserts
     * run client-side during proving, so reaching finalization proves the on-chain invariants held
     * (a non-signer/duplicate approval, or execute below threshold, would throw before submit); the
     * node additionally rejects a withdrawal whose offer output doesn't match the contract's claimed
     * spend, so execute finalizing proves the withdrawal money path end-to-end.
     */
    @Test
    fun governance_full_flow_2of3() = runBlocking<Unit> {
        val a = fundedSdk(nightWhole = 50)
        val b = fundedSdk(nightWhole = 20)
        val c = fundedSdk(nightWhole = 20)

        // Deploy a 2-of-3 Vault whose signers are the three wallets' real coin public keys.
        val address = deployVaultWith(
            deployer = a,
            signerCoinPublicKeys = listOf(a.coinPublicKey, b.coinPublicKey, c.coinPublicKey),
            threshold = 2,
        )
        Log.i(TAG, "2-of-3 Vault deployed at $address")

        // Fund the treasury so there is something to withdraw. deposit() retries the state fetch
        // until the indexer exposes the freshly-deployed contract, so reads after it are safe.
        assertTrue("deposit must finalize", deposit(a, address, BigInteger.valueOf(5_000_000L)))

        // A read-only handle any device could build from just the address (no signing key). Its
        // reads verify the view-call read path against on-chain truth at every stage.
        val reader = VaultContract.buildReadHandle(context, a, address)
        assertEquals("read: threshold", 2, VaultContract.getThreshold(reader))
        assertEquals("read: signer count", 3, VaultContract.getSignerCount(reader))
        assertEquals(
            "read: treasury balance reflects the deposit",
            BigInteger.valueOf(5_000_000L),
            VaultContract.getUnshieldedBalance(reader, ByteArray(32)),
        )

        // Signer A proposes a withdrawal (first proposal on a fresh Vault has id 1).
        val recipientHash = Bech32m.decode(a.walletAddress).second
        var proposeSubmitted = false
        VaultContract.proposeWithdrawal(
            context, a, address,
            recipientAddressHash = recipientHash,
            color = ByteArray(32),
            amount = BigInteger.valueOf(2_000_000L),
        ) { stage -> if (stage.toString().contains("Submitting")) proposeSubmitted = true }
        assertTrue("proposeWithdrawal (signer-gated) must finalize", proposeSubmitted)
        Log.i(TAG, "Proposal 1 created by signer A")

        // read: the proposal's recipient + amount match what A proposed (chain truth, not local).
        val onChain = VaultContract.getProposal(reader, FIRST_PROPOSAL_ID)
        assertEquals("read: proposal amount", BigInteger.valueOf(2_000_000L), onChain.amountBase)
        assertEquals("read: proposal recipient", recipientHash.toHex(), onChain.recipientHashHex)
        val statusBeforeExecute = onChain.status

        // Two distinct signers approve → meets the threshold of 2.
        var approveA = false
        VaultContract.approve(context, a, address, proposalId = FIRST_PROPOSAL_ID) { stage ->
            if (stage.toString().contains("Submitting")) approveA = true
        }
        var approveB = false
        VaultContract.approve(context, b, address, proposalId = FIRST_PROPOSAL_ID) { stage ->
            if (stage.toString().contains("Submitting")) approveB = true
        }
        assertTrue("signer A approval must finalize", approveA)
        assertTrue("signer B approval must finalize (distinct signer, threshold met)", approveB)
        assertEquals("read: approval count == 2", 2, VaultContract.getApprovalCount(reader, FIRST_PROPOSAL_ID))
        Log.i(TAG, "Proposal 1 approved by signers A + B (2/2 threshold)")

        // Permissionless execute (wallet C — not a required approver): the threshold is the
        // authorization, not the executor's identity. Treasury__send → sendUnshielded creates the
        // recipient UTXO for A; the SDK-built withdrawal offer names the matching output.
        var executeSubmitted = false
        VaultContract.execute(
            context, c, address,
            proposalId = FIRST_PROPOSAL_ID,
            recipientAddressHash = recipientHash,
            color = ByteArray(32),
            amount = BigInteger.valueOf(2_000_000L),
        ) { stage -> if (stage.toString().contains("Submitting")) executeSubmitted = true }
        assertTrue("execute (threshold met) + withdrawal money path must finalize", executeSubmitted)

        // read: status changed (Active → Executed) and the treasury was debited.
        assertNotEquals(
            "read: proposal status changes after execute",
            statusBeforeExecute,
            VaultContract.getProposalStatus(reader, FIRST_PROPOSAL_ID),
        )
        assertEquals(
            "read: treasury debited by the withdrawal",
            BigInteger.valueOf(3_000_000L),
            VaultContract.getUnshieldedBalance(reader, ByteArray(32)),
        )
        Log.i(TAG, "Proposal 1 executed by C — 2 NIGHT withdrawn to A; reads reflect chain truth")
    }

    /** Deploy a fresh 2-of-3 Vault: the wallet's own coin public key + two placeholders. */
    private suspend fun deployVault(s: MidnightSdk): String =
        deployVaultWith(s, listOf(s.coinPublicKey, ByteArray(32) { 0x11 }, ByteArray(32) { 0x22 }), threshold = 2)

    /** Deploy a Vault with explicit [signerCoinPublicKeys] and [threshold], via [deployer]. */
    private suspend fun deployVaultWith(
        deployer: MidnightSdk,
        signerCoinPublicKeys: List<ByteArray>,
        threshold: Int,
    ): String = VaultContract.deploy(
        context = context,
        sdk = deployer,
        signerCoinPublicKeys = signerCoinPublicKeys,
        threshold = threshold,
    ) { stage -> Log.i(TAG, "deploy stage: $stage") }

    /**
     * Deposit [amount] base-unit NIGHT into the treasury at [address]. deploy() returns a
     * node-finalized address but the indexer exposes the contract a beat later, so retry the
     * cheap (pre-proving) state fetch until indexed. Returns true if the deposit reached submit.
     */
    private suspend fun deposit(s: MidnightSdk, address: String, amount: BigInteger): Boolean {
        var reachedSubmit = false
        val deadline = System.currentTimeMillis() + 90_000
        while (System.currentTimeMillis() < deadline) {
            try {
                VaultContract.depositUnshielded(
                    context = context, sdk = s, address = address,
                    color = ByteArray(32), amount = amount,
                ) { stage ->
                    Log.i(TAG, "deposit stage: $stage")
                    val name = stage.toString()
                    if (name.contains("Submitting") || name.contains("Finalization")) reachedSubmit = true
                }
                break
            } catch (e: ContractCallException.StateFetchFailed) {
                Log.i(TAG, "contract not indexed yet; retrying deposit in 3s: ${e.message}")
                delay(3_000)
            }
        }
        return reachedSubmit
    }

    /**
     * Build a fresh-wallet REMOTE-proving SDK, request host funding via the logcat
     * marker, and block until NIGHT + dust are visible. Mirrors the SDK's
     * IsolatedWalletE2E (which lives in the SDK's own androidTest source and can't be
     * imported here).
     */
    private suspend fun fundedSdk(nightWhole: Int): MidnightSdk {
        val seed = BIP39.mnemonicToSeed(BIP39.generateMnemonic(24), "")
        val s = MidnightSdk.Builder(context)
            .network(MidnightNetwork.UNDEPLOYED)
            .seed(seed)
            .provingMode(ProvingMode.REMOTE)
            .build()
        openSdks += s
        seed.fill(0)
        val address = s.walletAddress

        // The dust fee transaction is proved LOCALLY even in REMOTE mode, so the
        // bundled wallet proving keys (dust/zswap, shipped in the APK via
        // bundleWalletKeys) must be installed on-device before any submit.
        ProvingKeyManager(context).installWalletKeysFromAssets()

        Log.i(FUND_TAG, "$FUND_MARKER addr=$address night=$nightWhole small=1 dust=true")

        val reachable = withTimeoutOrNull(15_000L) { s.wallet.balance() }
        assumeTrue("Localnet unreachable (indexer http://10.0.2.2:8088 healthy?)", reachable != null)

        val minNight = BigInteger.valueOf(nightWhole.toLong()).multiply(NIGHT_UNIT)
        val funded = runCatching {
            s.wallet.waitForFunding(minNight = minNight, timeoutMs = 5 * 60_000L)
        }.getOrNull()
        assumeTrue(
            "Host must fund $address (>= $nightWhole NIGHT) — run via fund-vault-e2e.sh.",
            funded != null,
        )

        if (!funded!!.dustRegistered) {
            runCatching { s.registerForDustGeneration() }
            val deadline = System.currentTimeMillis() + 3 * 60_000L
            while (System.currentTimeMillis() < deadline && !s.wallet.balance().dustRegistered) {
                delay(3_000L)
                runCatching { s.wallet.refresh() }
            }
            assumeTrue("Dust never registered for $address", s.wallet.balance().dustRegistered)
        }
        return s
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "VaultE2E"
        const val FUND_TAG = "KuiraE2EFund"
        const val FUND_MARKER = "KUIRA_FUND_REQ"
        const val FIRST_PROPOSAL_ID = 1L
        val NIGHT_UNIT: BigInteger = BigInteger.valueOf(1_000_000L)
    }
}

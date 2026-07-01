package com.kuiralabs.starter.counter

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuiralabs.starter.counter.data.VaultContract
import com.midnight.kuira.core.compact.ContractCallException
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.core.crypto.bip39.BIP39
import com.midnight.kuira.core.crypto.proving.ProvingMode
import com.midnight.kuira.core.network.MidnightNetwork
import com.midnight.kuira.sdk.MidnightSdk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
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
    private var sdk: MidnightSdk? = null

    @After
    fun tearDown() {
        sdk?.let { runCatching { it.close() } }
        sdk = null
    }

    @Test
    fun deploy_succeeds() = runBlocking {
        val s = fundedSdk(nightWhole = 50)
        sdk = s

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
        sdk = s

        val address = deployVault(s)
        Log.i(TAG, "Vault deployed at $address; depositing NIGHT into the treasury")

        // Deposit native NIGHT into the treasury. The native unshielded token color is 32 zero bytes
        // (UtxoSpend.NATIVE_TOKEN_TYPE). This exercises receiveUnshielded — the Key::Stack op #15
        // fixed — funding a CONTRACT with a real wallet UTXO (the UTXO->contract path, #30).
        val nativeColor = ByteArray(32) // all-zeros = NATIVE_TOKEN_TYPE (NIGHT)
        val amount = BigInteger.valueOf(5_000_000L) // 5 NIGHT (6 decimals)

        // deploy() returns a NODE-finalized address, but the indexer exposes the new contract a
        // beat later — a deposit immediately after can race "Contract not found". The state fetch
        // fails fast (before any proving), so retry cheaply until the contract is indexed.
        var reachedSubmit = false
        var lastError: Exception? = null
        val deadline = System.currentTimeMillis() + 90_000
        while (System.currentTimeMillis() < deadline) {
            try {
                VaultContract.depositUnshielded(
                    context = context,
                    sdk = s,
                    address = address,
                    color = nativeColor,
                    amount = amount,
                ) { stage ->
                    Log.i(TAG, "deposit stage: $stage")
                    val name = stage.toString()
                    if (name.contains("Submitting") || name.contains("Finalization")) reachedSubmit = true
                }
                break
            } catch (e: ContractCallException.StateFetchFailed) {
                lastError = e
                Log.i(TAG, "contract not indexed yet; retrying deposit in 3s: ${e.message}")
                delay(3_000)
            }
        }

        Log.i(TAG, "Deposited $amount base-unit NIGHT into Vault treasury at $address")
        // Reaching here means the deposit tx FINALIZED on-chain: receiveUnshielded claimed the
        // wallet UTXO and the treasury balance insert ran (an invalid/unbalanced deposit would
        // have thrown before this point — the SDK waits for finalization).
        assertTrue(
            "deposit must reach on-chain submission/finalization (last error: ${lastError?.message})",
            reachedSubmit,
        )
    }

    /** Deploy a fresh 2-of-3 Vault: the wallet's own coin public key + two placeholders. */
    private suspend fun deployVault(s: MidnightSdk): String {
        val signers = listOf(
            s.coinPublicKey,
            ByteArray(32) { 0x11 },
            ByteArray(32) { 0x22 },
        )
        return VaultContract.deploy(
            context = context,
            sdk = s,
            signerCoinPublicKeys = signers,
            threshold = 2,
        ) { stage -> Log.i(TAG, "deploy stage: $stage") }
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

    private companion object {
        const val TAG = "VaultE2E"
        const val FUND_TAG = "KuiraE2EFund"
        const val FUND_MARKER = "KUIRA_FUND_REQ"
        val NIGHT_UNIT: BigInteger = BigInteger.valueOf(1_000_000L)
    }
}

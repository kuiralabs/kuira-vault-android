package com.kuiralabs.vault

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuiralabs.vault.data.VaultContract
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
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

/**
 * On-chain e2e for the unshielded money-path fix (kuira-sdk-android #4), exercised through the
 * Vault's Money Path tab code path ([VaultContract.depositUnshieldedAutoFund] +
 * [VaultContract.depositUnshielded]). Deploys a fresh Vault, then drives all three #4 behaviours:
 *
 *  - **auto-fund ON**  → the GENERATED `depositUnshielded(color, amount)` call attaches no offer and
 *    the SDK auto-funds it (Layer 2). The exact call that failed pre-#4 with node error 138; here it
 *    finalizes and credits the treasury.
 *  - **auto-fund OFF** → same generated call with no offer throws the clear, typed
 *    [ContractCallException.UnshieldedValueUnfunded] (Layer 1).
 *  - **explicit offer** → [VaultContract.depositUnshielded] supplies its own funding offer; it takes
 *    precedence over auto-fund and credits the treasury (the sponsoring / custom-offer path).
 *
 * Same funding contract as [VaultDeployE2ETest]: a fresh REMOTE-proving wallet per test, funded by
 * the host via the [FUND_MARKER] logcat marker (run through the vault e2e funding servicer). Tests
 * SKIP (assumeTrue), never fail, when localnet / funding is absent.
 */
@RunWith(AndroidJUnit4::class)
class MoneyPathE2ETest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val openSdks = mutableListOf<MidnightSdk>()

    @After
    fun tearDown() {
        // close() is suspend as of alpha06 (DustLocalState teardown-race fix).
        runBlocking { openSdks.forEach { runCatching { it.close() } } }
        openSdks.clear()
    }

    /** #4 Layer 2 — the generated typed call with no offer auto-funds and credits the treasury. */
    @Test
    fun autoFundDeposit_creditsTreasury_viaGeneratedCall() = runBlocking {
        val s = fundedSdk(nightWhole = 50)
        val address = deployVault(s)
        Log.i(TAG, "Vault at $address — auto-fund deposit via the generated depositUnshielded(...)")

        val amount = BigInteger.valueOf(5_000_000L)
        val reachedSubmit = depositAutoFund(s, address, amount, autoFund = true)
        assertTrue("auto-funded deposit must reach on-chain submission/finalization", reachedSubmit)

        val reader = VaultContract.buildReadHandle(context, s, address)
        assertEquals(
            "auto-funded deposit must credit the treasury",
            amount,
            VaultContract.getUnshieldedBalance(reader, NATIVE_COLOR),
        )
    }

    /** #4 Layer 1 — no offer + auto-fund OFF surfaces the clear, typed UnshieldedValueUnfunded. */
    @Test
    fun autoFundOff_throwsUnshieldedValueUnfunded() = runBlocking<Unit> {
        val s = fundedSdk(nightWhole = 50)
        val address = deployVault(s)
        Log.i(TAG, "Vault at $address — expecting the typed Layer-1 error with auto-fund OFF")

        val error = runCatching {
            depositAutoFund(s, address, BigInteger.valueOf(5_000_000L), autoFund = false)
        }.exceptionOrNull()

        assertTrue(
            "auto-fund OFF + no offer must throw ContractCallException.UnshieldedValueUnfunded, got: $error",
            error is ContractCallException.UnshieldedValueUnfunded,
        )

        // The rejected deposit must have NO on-chain effect — the treasury stays empty. (The
        // contract is indexed by now: the Layer-1 error is thrown DURING circuit execution, which
        // only runs after the state fetch succeeds.)
        val reader = VaultContract.buildReadHandle(context, s, address)
        assertEquals(
            "a rejected (Layer-1) deposit must not credit the treasury",
            BigInteger.ZERO,
            VaultContract.getUnshieldedBalance(reader, NATIVE_COLOR),
        )
    }

    /**
     * #4 precedence — an explicit offer takes precedence over auto-fund. Proven by turning
     * auto-fund OFF (so a no-offer deposit throws — see [autoFundOff_throwsUnshieldedValueUnfunded])
     * and confirming an EXPLICIT offer STILL funds the deposit. Crediting therefore proves the
     * caller's offer was used, not auto-fund — which is exactly what precedence means.
     */
    @Test
    fun explicitOffer_creditsWithAutoFundOff_precedence() = runBlocking {
        val s = fundedSdk(nightWhole = 50)
        val address = deployVault(s)

        // Disable auto-fund on THIS session's SDK — with no offer, a deposit would now Layer-1 throw.
        s.config.configureUnshieldedAutoFund(false, null)
        Log.i(TAG, "Vault at $address — explicit-offer deposit with auto-fund OFF (precedence proof)")

        val amount = BigInteger.valueOf(5_000_000L)
        val reachedSubmit = depositExplicit(s, address, amount)
        assertTrue("explicit-offer deposit must reach on-chain submission/finalization", reachedSubmit)

        val reader = VaultContract.buildReadHandle(context, s, address)
        assertEquals(
            "explicit offer must credit the treasury even with auto-fund off (precedence)",
            amount,
            VaultContract.getUnshieldedBalance(reader, NATIVE_COLOR),
        )
    }

    /**
     * #4 withdrawal Layer 1 — auto-fund is DEPOSIT-only, so a withdrawal (sendUnshielded) with no
     * offer has no auto-fill and must surface the typed error. Deploys a threshold-1 Vault (the sole
     * funded wallet approves on its own), funds it, proposes + approves a withdrawal, then executes
     * via the generated execute(id) (no withdrawal offer) → UnshieldedValueUnfunded, treasury intact.
     */
    @Test
    fun withdrawWithoutOffer_throwsUnshieldedValueUnfunded() = runBlocking<Unit> {
        val s = fundedSdk(nightWhole = 50)
        val address = deployVault(s, threshold = 1)
        Log.i(TAG, "Vault at $address — withdrawal with NO offer must Layer-1 throw")

        // Fund the treasury so there is value to (attempt to) withdraw.
        val deposited = BigInteger.valueOf(5_000_000L)
        assertTrue("deposit must finalize", depositExplicit(s, address, deposited))

        // Propose + approve to the threshold of 1 (deployer is the sole real signer).
        val recipientHash = Bech32m.decode(s.walletAddress).second
        VaultContract.proposeWithdrawal(
            context, s, address,
            recipientAddressHash = recipientHash, color = NATIVE_COLOR,
            amount = BigInteger.valueOf(2_000_000L),
        ) { }
        VaultContract.approve(context, s, address, proposalId = FIRST_PROPOSAL_ID) { }

        // Execute via the generated execute(id) — NO withdrawal offer.
        val error = runCatching {
            VaultContract.executeWithoutWithdrawalOffer(context, s, address, proposalId = FIRST_PROPOSAL_ID) { }
        }.exceptionOrNull()
        assertTrue(
            "a withdrawal with no offer must throw ContractCallException.UnshieldedValueUnfunded, got: $error",
            error is ContractCallException.UnshieldedValueUnfunded,
        )

        // The rejected withdrawal must NOT debit the treasury — it stays at the deposited amount.
        val reader = VaultContract.buildReadHandle(context, s, address)
        assertEquals(
            "a rejected (Layer-1) withdrawal must not debit the treasury",
            deposited,
            VaultContract.getUnshieldedBalance(reader, NATIVE_COLOR),
        )
    }

    // ── Harness (mirrors VaultDeployE2ETest — its helpers are private to that class) ──

    /** Deploy a fresh Vault: the wallet's own coin public key + two placeholders, at [threshold]. */
    private suspend fun deployVault(s: MidnightSdk, threshold: Int = 2): String = VaultContract.deploy(
        context = context,
        sdk = s,
        signerCoinPublicKeys = listOf(s.coinPublicKey, ByteArray(32) { 0x11 }, ByteArray(32) { 0x22 }),
        threshold = threshold,
    ) { stage -> Log.i(TAG, "deploy stage: $stage") }

    /**
     * Deposit via [VaultContract.depositUnshieldedAutoFund] (the generated call, no manual offer).
     * Retries the cheap pre-proving state fetch until the freshly-deployed contract is indexed; any
     * other exception (notably the Layer-1 UnshieldedValueUnfunded when [autoFund] is off) propagates.
     */
    private suspend fun depositAutoFund(
        s: MidnightSdk,
        address: String,
        amount: BigInteger,
        autoFund: Boolean,
    ): Boolean {
        var reachedSubmit = false
        val deadline = System.currentTimeMillis() + 90_000
        while (System.currentTimeMillis() < deadline) {
            try {
                VaultContract.depositUnshieldedAutoFund(
                    context = context, sdk = s, address = address,
                    color = NATIVE_COLOR, amount = amount, autoFund = autoFund,
                ) { stage ->
                    val name = stage.toString()
                    if (name.contains("Submitting") || name.contains("Finalization")) reachedSubmit = true
                }
                break
            } catch (e: ContractCallException.StateFetchFailed) {
                Log.i(TAG, "contract not indexed yet; retrying in 3s: ${e.message}")
                delay(3_000)
            }
        }
        return reachedSubmit
    }

    /** Deposit via [VaultContract.depositUnshielded] (the explicit-offer path), same retry. */
    private suspend fun depositExplicit(s: MidnightSdk, address: String, amount: BigInteger): Boolean {
        var reachedSubmit = false
        val deadline = System.currentTimeMillis() + 90_000
        while (System.currentTimeMillis() < deadline) {
            try {
                VaultContract.depositUnshielded(
                    context = context, sdk = s, address = address,
                    color = NATIVE_COLOR, amount = amount,
                ) { stage ->
                    val name = stage.toString()
                    if (name.contains("Submitting") || name.contains("Finalization")) reachedSubmit = true
                }
                break
            } catch (e: ContractCallException.StateFetchFailed) {
                Log.i(TAG, "contract not indexed yet; retrying in 3s: ${e.message}")
                delay(3_000)
            }
        }
        return reachedSubmit
    }

    /**
     * Build a fresh-wallet REMOTE-proving SDK, request host funding via the [FUND_MARKER] logcat
     * marker, and block until NIGHT + dust are visible. Mirrors the SDK's IsolatedWalletE2E /
     * VaultDeployE2ETest.fundedSdk (which are private to their own classes).
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

        // The dust fee tx is proved LOCALLY even in REMOTE mode, so the bundled wallet proving keys
        // (shipped in the APK via bundleWalletKeys) must be installed before any submit.
        ProvingKeyManager(context).installWalletKeysFromAssets()

        Log.i(FUND_TAG, "$FUND_MARKER addr=$address night=$nightWhole small=1 dust=true")

        val reachable = withTimeoutOrNull(15_000L) { s.wallet.balance() }
        assumeTrue("Localnet unreachable (indexer http://10.0.2.2:8088 healthy?)", reachable != null)

        val minNight = BigInteger.valueOf(nightWhole.toLong()).multiply(NIGHT_UNIT)
        val funded = runCatching {
            s.wallet.waitForFunding(minNight = minNight, timeoutMs = 5 * 60_000L)
        }.getOrNull()
        assumeTrue(
            "Host must fund $address (>= $nightWhole NIGHT) — run the vault e2e funding servicer.",
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
        const val TAG = "MoneyPathE2E"
        const val FUND_TAG = "KuiraE2EFund"
        const val FUND_MARKER = "KUIRA_FUND_REQ"
        const val FIRST_PROPOSAL_ID = 1L
        val NIGHT_UNIT: BigInteger = BigInteger.valueOf(1_000_000L)

        /** 32 zero bytes = native NIGHT. */
        val NATIVE_COLOR = ByteArray(32)
    }
}

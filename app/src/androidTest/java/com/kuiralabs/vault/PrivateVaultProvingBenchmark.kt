package com.kuiralabs.vault

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuiralabs.vault.data.PrivateVaultContract
import com.kuiralabs.vault.data.PrivateVaultCrypto
import com.midnight.kuira.core.compact.ContractCallException
import com.midnight.kuira.core.compact.ContractCallStage
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
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigInteger

/**
 * Device-proving benchmark for the Private Vault's contract circuits — the measurement the plan
 * calls for ("budget a device-proving benchmark before committing to circuit shape") before the
 * Tier 2 / Tier 3 SDK proving lift (#58).
 *
 * WHY: contract circuits ALWAYS prove locally (`MidnightConfig` hardcodes the LocalProofProvider;
 * `ProvingMode.REMOTE` only offloads WALLET txs), and a private vault CAN'T offload to a remote
 * prover without shipping its witness (the secret inputs) to a third party. So the local prover's
 * peak memory + wall time per circuit is what decides whether the heavier Tier 2/3 circuits fit on
 * a real device, or whether #58 (route proofs by cost AND privacy) is mandatory first.
 *
 * WHAT: runs the same on-chain ceremony as [PrivateVaultDeployE2ETest] but wraps each proving call
 * in a sampler that records wall time, the Proving-stage duration, and the process peak RSS
 * (/proc/self/status VmRSS, polled off-thread). pvExecute (k=16, + the unshielded withdrawal money
 * path) is the heaviest Tier-1 circuit and the headline number.
 *
 * Rides the same fund harness (device→host logcat marker); SKIPs (assumeTrue) when localnet/funding
 * is absent. Results log to logcat under "BENCH" and to <externalFiles>/proving-benchmark.txt.
 *
 * NOTE: peak RSS is process-wide and the native allocator may retain pages between circuits, so a
 * later circuit's peak includes memory earlier ones kept — read the ABSOLUTE peak (esp. pvExecute)
 * as "how close did the prover get to OOM," not as a clean per-circuit delta.
 */
@RunWith(AndroidJUnit4::class)
class PrivateVaultProvingBenchmark {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val openSdks = mutableListOf<MidnightSdk>()
    private val rows = mutableListOf<Row>()

    @After
    fun tearDown() {
        openSdks.forEach { runCatching { it.close() } }
        openSdks.clear()
    }

    private data class Row(val circuit: String, val wallMs: Long, val provingMs: Long, val peakRssMb: Long)

    @Test
    fun benchmark_private_vault_proving_costs() = runBlocking<Unit> {
        val alice = fundedSdk(nightWhole = 50)
        val bob = buildSdk()

        // ── pvDeploy (constructor publish; proves little — a baseline) ──
        val deploy = measure("deploy") { onProgress ->
            PrivateVaultContract.deploy(
                context, alice,
                signerCoinPublicKeys = listOf(alice.coinPublicKey, bob.coinPublicKey),
                threshold = 2, onProgress = onProgress,
            )
        }
        val address = deploy.address
        Log.i(BTAG, "vault deployed at $address")

        // ── pvDepositUnshielded ──
        measure("pvDepositUnshielded") { onProgress -> depositWithRetry(alice, address, N5, onProgress) }

        fundExisting(bob, nightWhole = 20)
        val bobInvite = PrivateVaultCrypto.decodeInvite(deploy.coSignerInvites[0])

        // ── pvProposeWithdrawal ──
        val recipientHash = Bech32m.decode(alice.walletAddress).second
        measure("pvProposeWithdrawal") { onProgress ->
            PrivateVaultContract.proposeWithdrawal(
                context, alice, address,
                recipientAddressHash = recipientHash, color = ByteArray(32), amount = N2,
                viewingKey = deploy.viewingKey, memberSalt = deploy.creatorSalt, onProgress = onProgress,
            )
        }

        // ── pvApprove (×2 — one per signer; both measured) ──
        measure("pvApprove#1") { onProgress ->
            PrivateVaultContract.approve(context, alice, address, ID, deploy.creatorSalt, onProgress)
        }
        measure("pvApprove#2") { onProgress ->
            PrivateVaultContract.approve(context, bob, address, ID, bobInvite.memberSalt, onProgress)
        }

        // ── pvExecute (heaviest: k=16 + unshielded withdrawal money path) ──
        measure("pvExecute") { onProgress ->
            PrivateVaultContract.execute(
                context, alice, address, proposalId = ID,
                viewingKey = deploy.viewingKey, threshold = 2, thresholdSalt = deploy.thresholdSalt,
                onProgress = onProgress,
            )
        }

        report()
    }

    /** Run [block] (which is handed the onProgress callback) while sampling RSS + timing Proving. */
    private suspend fun <T> measure(name: String, block: suspend (suspend (ContractCallStage) -> Unit) -> T): T {
        val sampler = RssSampler().also { it.start() }
        var provingStart = 0L
        var provingEnd = 0L
        val t0 = System.currentTimeMillis()
        val result = block { stage ->
            when (stage) {
                is ContractCallStage.Proving -> if (provingStart == 0L) provingStart = System.currentTimeMillis()
                is ContractCallStage.Balancing -> if (provingStart != 0L && provingEnd == 0L) provingEnd = System.currentTimeMillis()
                else -> {}
            }
        }
        val wall = System.currentTimeMillis() - t0
        val peakKb = sampler.stop()
        val provingMs = if (provingStart != 0L && provingEnd >= provingStart) provingEnd - provingStart else -1L
        val row = Row(name, wall, provingMs, peakKb / 1024)
        rows += row
        Log.i(BTAG, "BENCH row circuit=${row.circuit} wallMs=${row.wallMs} provingMs=${row.provingMs} peakRssMb=${row.peakRssMb}")
        return result
    }

    private fun report() {
        val cores = Runtime.getRuntime().availableProcessors()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalRamMb = mi.totalMem / (1024 * 1024)
        val jvmMaxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val globalPeak = rows.maxOfOrNull { it.peakRssMb } ?: 0L

        val sb = StringBuilder()
        sb.appendLine("Private Vault — device proving benchmark")
        sb.appendLine("device: totalRAM=${totalRamMb}MB  jvmHeapLimit=${jvmMaxMb}MB  cores=$cores (prover uses cores-1)")
        sb.appendLine("proving mode: contract circuits LOCAL (wallet txs REMOTE)")
        sb.appendLine()
        sb.appendLine("%-22s %10s %12s %14s".format("circuit", "wall(ms)", "proving(ms)", "peakRSS(MB)"))
        sb.appendLine("-".repeat(60))
        rows.forEach { sb.appendLine("%-22s %10d %12d %14d".format(it.circuit, it.wallMs, it.provingMs, it.peakRssMb)) }
        sb.appendLine("-".repeat(60))
        sb.appendLine("global peak RSS across the ceremony: ${globalPeak}MB")
        val report = sb.toString()

        Log.i(BTAG, "\n$report")
        runCatching {
            val out = File(context.getExternalFilesDir(null), "proving-benchmark.txt")
            out.writeText(report)
            Log.i(BTAG, "benchmark written to ${out.absolutePath}")
        }
    }

    // ── RSS sampler: polls /proc/self/status VmRSS off-thread, tracks the max ──
    private class RssSampler {
        @Volatile private var running = false
        @Volatile private var peakKb = 0L
        private var thread: Thread? = null

        fun start() {
            peakKb = 0L; running = true
            thread = Thread {
                while (running) {
                    val rss = readVmRssKb()
                    if (rss > peakKb) peakKb = rss
                    try { Thread.sleep(40) } catch (_: InterruptedException) { break }
                }
            }.apply { isDaemon = true; start() }
        }

        fun stop(): Long {
            running = false
            thread?.interrupt()
            thread?.join(1_000)
            return peakKb
        }

        private fun readVmRssKb(): Long = runCatching {
            File("/proc/self/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("VmRSS:") }
                    ?.removePrefix("VmRSS:")?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.toLong() ?: 0L
            }
        }.getOrDefault(0L)
    }

    private suspend fun depositWithRetry(
        s: MidnightSdk, address: String, amount: BigInteger, onProgress: suspend (ContractCallStage) -> Unit,
    ) {
        val deadline = System.currentTimeMillis() + 90_000
        while (true) {
            try {
                PrivateVaultContract.depositUnshielded(context, s, address, ByteArray(32), amount, onProgress)
                return
            } catch (e: ContractCallException.StateFetchFailed) {
                if (System.currentTimeMillis() > deadline) throw e
                Log.i(BTAG, "contract not indexed yet; retrying deposit in 3s")
                delay(3_000)
            }
        }
    }

    private fun buildSdk(): MidnightSdk {
        val seed = BIP39.mnemonicToSeed(BIP39.generateMnemonic(24), "")
        val s = MidnightSdk.Builder(context)
            .network(MidnightNetwork.UNDEPLOYED).seed(seed).provingMode(ProvingMode.REMOTE).build()
        openSdks += s
        seed.fill(0)
        ProvingKeyManager(context).installWalletKeysFromAssets()
        return s
    }

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

    private companion object {
        const val BTAG = "PrivateVaultBench"
        const val FUND_TAG = "KuiraE2EFund"
        const val FUND_MARKER = "KUIRA_FUND_REQ"
        const val ID = 0L
        val NIGHT_UNIT: BigInteger = BigInteger.valueOf(1_000_000L)
        val N5: BigInteger = BigInteger.valueOf(5_000_000L)
        val N2: BigInteger = BigInteger.valueOf(2_000_000L)
    }
}

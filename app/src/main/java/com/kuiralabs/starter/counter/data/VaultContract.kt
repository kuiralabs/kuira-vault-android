package com.kuiralabs.starter.counter.data

import android.content.Context
import com.midnight.kuira.core.compact.CompactEnum
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.sdk.MidnightSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.math.BigInteger

// Thin wrapper around MidnightContract for the Vault — the unshielded multisig
// treasury. Loads the synced assets and gives the caller a tight surface
// (deploy / depositUnshielded).
//
// The io.github.kuiralabs.contract plugin syncs the compiled contract into the
// app's assets: the runtime JS as runtime/vault-contract.js (alias "vault") and
// each circuit's keys as keys/<circuit>.{prover,verifier,bzkir}.
internal object VaultContract {

    private const val NAME = "vault"
    private const val CONTRACT_JS_ASSET = "runtime/$NAME-contract.js"
    private const val KEYS_DIR = "keys"

    // Embed every circuit's verifier key at deploy so all circuits are callable
    // on-chain. (Calls don't pass verifier keys — they're fetched from chain.)
    private fun loadAllVerifierKeys(context: Context): Map<String, ByteArray> {
        val files = context.assets.list(KEYS_DIR).orEmpty().filter { it.endsWith(".verifier") }
        return files.associate { file ->
            file.removeSuffix(".verifier") to
                context.assets.open("$KEYS_DIR/$file").use { it.readBytes() }
        }
    }

    private fun installProvingKeys(context: Context) {
        ProvingKeyManager(context).installCircuitKeysFromAssets()
    }

    private fun buildHandle(
        context: Context,
        sdk: MidnightSdk,
        address: String?,
        forWrite: Boolean,
        constructorArgs: List<Any?> = emptyList(),
    ): MidnightContract = MidnightContract.create(sdk.config) {
        name = NAME
        contractJs = context.assets.open(CONTRACT_JS_ASSET)
        if (address != null) this.address = address
        if (forWrite) {
            coinPublicKey = sdk.coinPublicKey
            circuitVerifierKeys = loadAllVerifierKeys(context)
        }
        if (constructorArgs.isNotEmpty()) this.constructorArgs = constructorArgs
    }

    // The Either<ZswapCoinPublicKey, ContractAddress> signer struct, coin-key (left) arm.
    // Only left-arm (coin public key) signers can authenticate today (getCaller =
    // ownPublicKey); the right arm is a zeroed placeholder the contract canonicalizes.
    private fun signerStruct(coinPublicKey: ByteArray): Map<String, Any?> = mapOf(
        "is_left" to true,
        "left" to mapOf("bytes" to coinPublicKey),
        "right" to mapOf("bytes" to ByteArray(32)),
    )

    // Shape-valid placeholder constructor args for CIRCUIT CALLS. The call path runs initialState()
    // to build a throwaway state, then swaps in the on-chain state — so the values are discarded and
    // only the shape must pass validation. The 3 signers must be DISTINCT (SignerManager rejects
    // duplicates during initialize), hence a different fill byte per signer; threshold in 1..255.
    private fun callConstructorArgs(): List<Any?> = listOf(
        List(3) { i -> signerStruct(ByteArray(32) { (i + 1).toByte() }) },
        BigInteger.ONE,
    )

    /**
     * Deploy a fresh Vault with [signerCoinPublicKeys] (exactly 3) and [threshold].
     * Returns the contract address.
     */
    suspend fun deploy(
        context: Context,
        sdk: MidnightSdk,
        signerCoinPublicKeys: List<ByteArray>,
        threshold: Int,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ): String {
        require(signerCoinPublicKeys.size == 3) { "Vault constructor takes exactly 3 signers" }
        installProvingKeys(context)
        val signersArg = signerCoinPublicKeys.map { signerStruct(it) }
        val handle = buildHandle(
            context = context,
            sdk = sdk,
            address = null,
            forWrite = true,
            constructorArgs = listOf(signersArg, BigInteger.valueOf(threshold.toLong())),
        )
        return handle.deploy(onProgress = onProgress).contractAddress
    }

    /**
     * Deposit [amount] of unshielded token [color] into the treasury. Permissionless;
     * the tokens are claimed by the contract via receiveUnshielded in this transaction.
     */
    suspend fun depositUnshielded(
        context: Context,
        sdk: MidnightSdk,
        address: String,
        color: ByteArray,
        amount: BigInteger,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        installProvingKeys(context)
        val handle = buildHandle(
            context, sdk, address = address, forWrite = true,
            constructorArgs = callConstructorArgs(),
        )
        // Fund the value the treasury claims via receiveUnshielded: the SDK selects wallet
        // NIGHT UTXOs covering `amount` (+ change) and the native assembler adds + signs the
        // guaranteed unshielded offer. Without this the unshielded segment underflows → node
        // error 138 (BalanceCheckOverspend).
        val fundingJson = sdk.buildUnshieldedFundingJson(amount, tokenType = color.toHex())
        handle.call(
            "depositUnshielded", color, amount,
            onProgress = onProgress,
            unshieldedFundingJson = fundingJson,
        )
    }

    /**
     * Propose an unshielded withdrawal of [amount] of token [color] to [recipientAddressHash]
     * (a 32-byte UserAddress). Signer-gated — the calling wallet's key must be a Vault signer.
     * The first proposal on a fresh Vault has id 1.
     */
    suspend fun proposeWithdrawal(
        context: Context,
        sdk: MidnightSdk,
        address: String,
        recipientAddressHash: ByteArray,
        color: ByteArray,
        amount: BigInteger,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        installProvingKeys(context)
        val handle = buildHandle(
            context, sdk, address = address, forWrite = true,
            constructorArgs = callConstructorArgs(),
        )
        // Recipient { kind: RecipientKind (enum, JS number), address: Bytes<32> }.
        val recipient = mapOf(
            "kind" to CompactEnum(RECIPIENT_KIND_UNSHIELDED_USER),
            "address" to recipientAddressHash,
        )
        handle.call("proposeWithdrawal", recipient, color, amount, onProgress = onProgress)
    }

    /**
     * Approve proposal [proposalId] as the calling wallet (must be a signer and not have already
     * approved). Reaching finalization proves the signer-gating + approval accounting passed.
     */
    suspend fun approve(
        context: Context,
        sdk: MidnightSdk,
        address: String,
        proposalId: Long,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        installProvingKeys(context)
        val handle = buildHandle(
            context, sdk, address = address, forWrite = true,
            constructorArgs = callConstructorArgs(),
        )
        handle.call("approve", BigInteger.valueOf(proposalId), onProgress = onProgress)
    }

    /**
     * Execute approved proposal [proposalId], sending [amount] of [color] to
     * [recipientAddressHash]. Permissionless once the threshold is met (any caller). The
     * recipient/color/amount must match the proposal — the contract's Treasury__send claims that
     * exact spend, and the SDK-built withdrawal offer names the matching recipient output.
     * Reaching finalization proves the threshold check + the withdrawal money path.
     */
    suspend fun execute(
        context: Context,
        sdk: MidnightSdk,
        address: String,
        proposalId: Long,
        recipientAddressHash: ByteArray,
        color: ByteArray,
        amount: BigInteger,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        installProvingKeys(context)
        val handle = buildHandle(
            context, sdk, address = address, forWrite = true,
            constructorArgs = callConstructorArgs(),
        )
        // The contract SENDS value out (Treasury__send -> sendUnshielded); the withdrawal offer
        // provides the matching recipient output (no inputs/signature — the contract has the funds).
        val withdrawalJson = sdk.buildUnshieldedWithdrawalJson(
            recipientAddressHash = recipientAddressHash,
            amount = amount,
            tokenType = color.toHex(),
        )
        handle.call(
            "execute", BigInteger.valueOf(proposalId),
            onProgress = onProgress,
            unshieldedWithdrawalJson = withdrawalJson,
        )
    }

    /**
     * Read-only handle for observing the Vault's ledger state and calling view circuits (no signing
     * keys needed). Includes the constructor args: a view-call read runs initialState() (then swaps
     * in the on-chain state), so a constructor-args contract needs shape-valid args to pass its
     * validation — even for a read.
     */
    fun buildReadHandle(context: Context, sdk: MidnightSdk, address: String): MidnightContract =
        buildHandle(context, sdk, address = address, forWrite = false, constructorArgs = callConstructorArgs())

    /**
     * Live per-proposal approval count from the contract's `_approvalCount` ledger map. This is
     * the one governance value read from chain (it changes when OTHER signer wallets approve);
     * proposal details + treasury balance are tracked app-side. Emits on each on-chain change.
     */
    fun observeApprovalCounts(handle: MidnightContract): Flow<Map<Long, Int>> =
        handle.observeLedger().map { ledger -> decodeApprovalCounts(ledger.getRawOrNull(LEDGER_APPROVAL_COUNT)) }

    private fun decodeApprovalCounts(raw: Any?): Map<Long, Int> {
        val map = raw as? Map<*, *> ?: return emptyMap()
        return map.entries.mapNotNull { (k, v) ->
            val id = asLong(k) ?: return@mapNotNull null
            val count = asLong(v)?.toInt() ?: return@mapNotNull null
            id to count
        }.toMap()
    }

    private fun asLong(x: Any?): Long? = when (x) {
        is Long -> x
        is Int -> x.toLong()
        is BigInteger -> x.toLong()
        is String -> x.toLongOrNull()
        else -> null
    }

    // ── On-chain reads (view-call read path) ──
    // These read the composed OZ module state (treasury balance, proposal details, threshold)
    // that isn't exposed as a raw ledger field — so ANY device (not just the deployer) sees
    // chain truth. read() returns JSON: a scalar Uint arrives as a quoted decimal string, Bytes
    // as hex, a struct as a JSON object.

    /** A proposal as stored on-chain (recipient kind/address, token, amount, status). */
    data class OnChainProposal(
        val recipientKind: Int,
        val recipientHashHex: String,
        val colorHex: String,
        val amountBase: BigInteger,
        val status: Int,
    )

    suspend fun getThreshold(handle: MidnightContract): Int =
        jsonScalar(handle.read("getThreshold")).toInt()

    suspend fun getSignerCount(handle: MidnightContract): Int =
        jsonScalar(handle.read("getSignerCount")).toInt()

    suspend fun getUnshieldedBalance(handle: MidnightContract, color: ByteArray): BigInteger =
        BigInteger(jsonScalar(handle.read("getUnshieldedBalance", color)))

    suspend fun getApprovalCount(handle: MidnightContract, proposalId: Long): Int =
        jsonScalar(handle.read("getApprovalCount", BigInteger.valueOf(proposalId))).toInt()

    suspend fun getProposalStatus(handle: MidnightContract, proposalId: Long): Int =
        jsonScalar(handle.read("getProposalStatus", BigInteger.valueOf(proposalId))).toInt()

    suspend fun getProposal(handle: MidnightContract, proposalId: Long): OnChainProposal {
        val o = JSONObject(handle.read("getProposal", BigInteger.valueOf(proposalId)))
        val to = o.getJSONObject("to")
        return OnChainProposal(
            recipientKind = to.getInt("kind"),
            recipientHashHex = bytesHex(to, "address"),
            colorHex = bytesHex(o, "color"),
            amountBase = BigInteger(o.getString("amount")),
            status = o.getInt("status"),
        )
    }

    /** Parse a JSON scalar (a quoted Uint decimal string, a number, or a boolean) to its text form. */
    private fun jsonScalar(json: String): String = org.json.JSONTokener(json).nextValue().toString()

    /**
     * Decode a Compact Bytes field to hex. read() emits a top-level Bytes as a hex string, but a
     * Bytes nested in a struct arrives as a JSON array of byte values — accept both.
     */
    private fun bytesHex(o: JSONObject, key: String): String = when (val v = o.get(key)) {
        is String -> v
        is org.json.JSONArray -> (0 until v.length()).joinToString("") { "%02x".format(v.getInt(it) and 0xFF) }
        else -> v.toString()
    }

    private const val RECIPIENT_KIND_UNSHIELDED_USER = 1
    private const val LEDGER_APPROVAL_COUNT = "_approvalCount"

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}

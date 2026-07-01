package com.kuiralabs.starter.counter.data

import android.content.Context
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.sdk.MidnightSdk
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
        handle.call("depositUnshielded", color, amount, onProgress = onProgress)
    }
}

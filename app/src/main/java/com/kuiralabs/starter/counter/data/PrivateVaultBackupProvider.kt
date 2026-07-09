package com.kuiralabs.starter.counter.data

import android.util.Log
import com.midnight.kuira.dapp.backup.AppDataBackupProvider
import javax.inject.Inject

/**
 * Folds the Private Vault's device-local membership + approval state into the Sigil cloud backup,
 * so a reinstall or device-hop recovers your vaults instead of stranding them (the viewing key +
 * salts are otherwise unrecoverable — the on-chain state is opaque). The SDK discovers this via its
 * `Optional<AppDataBackupProvider>` injection (bound in [com.kuiralabs.starter.counter.di.VaultBackupBindings]).
 * Bytes are PRF-encrypted under the passkey before Block Store — same protection as the seed.
 */
class PrivateVaultBackupProvider @Inject constructor(
    private val store: PrivateVaultStore,
) : AppDataBackupProvider {

    override suspend fun snapshot(): ByteArray? = runCatching { store.snapshotBytes() }
        .onFailure { Log.w(TAG, "private-vault snapshot failed", it) }
        .getOrNull()

    override suspend fun restore(bytes: ByteArray) {
        // A malformed blob must never fail the whole sigil restore.
        runCatching { store.restoreFromBytes(bytes) }
            .onFailure { Log.w(TAG, "private-vault restore failed (ignored)", it) }
    }

    private companion object { const val TAG = "PrivateVaultBackup" }
}

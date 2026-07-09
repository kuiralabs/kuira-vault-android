package com.kuiralabs.vault.di

import com.kuiralabs.vault.data.PrivateVaultBackupProvider
import com.midnight.kuira.dapp.backup.AppDataBackupProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the Private Vault's [AppDataBackupProvider] so the SDK's `Optional<AppDataBackupProvider>`
 * (consumed in `SigilPanelViewModel`) becomes present — a sigil backup then includes vault
 * membership + approval state, and a sigil restore repopulates it. Without this binding the SDK
 * sees an empty Optional and the vault state is device-local only. Mirrors Kicks's binding.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VaultBackupBindings {

    @Binds
    @Singleton
    abstract fun bindAppDataBackupProvider(
        impl: PrivateVaultBackupProvider,
    ): AppDataBackupProvider
}

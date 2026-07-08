package com.kuiralabs.starter.counter.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.midnight.kuira.core.network.MidnightNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which Vault this device is pointed at, per network. Only the address is essential — everything
 * else (threshold, balance, proposals) is read from chain, so a device that CONNECTS to a Vault it
 * didn't deploy sees the same truth. The threshold is cached for an instant first paint before the
 * chain read lands. Encrypted at rest (EncryptedSharedPreferences, consistent with the SDK's stores).
 */
@Singleton
class VaultStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kuira-starter-vault-prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun addrKey(network: MidnightNetwork) = "vault.addr.${network.name}"
    private fun threshKey(network: MidnightNetwork) = "vault.thresh.${network.name}"

    fun getAddress(network: MidnightNetwork): String? = prefs.getString(addrKey(network), null)
    fun getThresholdHint(network: MidnightNetwork): Int = prefs.getInt(threshKey(network), 0)

    fun save(network: MidnightNetwork, address: String, thresholdHint: Int) {
        prefs.edit()
            .putString(addrKey(network), address)
            .putInt(threshKey(network), thresholdHint)
            .apply()
    }

    fun clear(network: MidnightNetwork) {
        prefs.edit().remove(addrKey(network)).remove(threshKey(network)).apply()
    }
}

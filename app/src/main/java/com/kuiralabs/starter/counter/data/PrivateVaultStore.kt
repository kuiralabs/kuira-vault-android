package com.kuiralabs.starter.counter.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.midnight.kuira.core.network.MidnightNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This device's MEMBER material for a private vault, per network. Unlike the public [VaultStore]
 * (address only — everything else is public chain-read), the private vault's material is SECRET:
 * the viewing key decrypts proposals, the member salt proves membership, the threshold salt lets
 * this device execute. Held in the same protected tier as wallet material
 * (EncryptedSharedPreferences), in a SEPARATE prefs file from the public vault.
 *
 * Also tracks which proposal ids THIS device approved — the contract's approval tags are unlinkable
 * (H(pk, salt, id)) and not exported for a membership check, so "did I approve?" is answered
 * locally rather than steering a signer into the contract's duplicate-approval assert.
 */
@Singleton
class PrivateVaultStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kuira-private-vault-prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /** The member material this device holds for a private vault on one network. */
    data class Membership(
        val address: String,
        val viewingKey: ByteArray,
        val threshold: Int,
        /** Real signer count (members know it; the on-chain roster stays padded/hidden). */
        val signerCount: Int,
        val thresholdSalt: ByteArray,
        val memberSalt: ByteArray,
        /** True if this device DEPLOYED the vault (holds the invites to share); false if it JOINED. */
        val isCreator: Boolean,
        /** Encoded invites to re-share (creator only). */
        val invites: List<String>,
    )

    private fun k(network: MidnightNetwork, field: String) = "pvault.$field.${network.name}"

    fun get(network: MidnightNetwork): Membership? {
        val address = prefs.getString(k(network, "addr"), null) ?: return null
        return Membership(
            address = address,
            viewingKey = b64(prefs.getString(k(network, "vk"), null) ?: return null),
            threshold = prefs.getInt(k(network, "th"), 0),
            signerCount = prefs.getInt(k(network, "sc"), 0),
            thresholdSalt = b64(prefs.getString(k(network, "ts"), null) ?: return null),
            memberSalt = b64(prefs.getString(k(network, "ms"), null) ?: return null),
            isCreator = prefs.getBoolean(k(network, "creator"), false),
            invites = prefs.getStringSet(k(network, "invites"), emptySet())?.toList().orEmpty(),
        )
    }

    fun save(network: MidnightNetwork, m: Membership) {
        prefs.edit()
            .putString(k(network, "addr"), m.address)
            .putString(k(network, "vk"), b64(m.viewingKey))
            .putInt(k(network, "th"), m.threshold)
            .putInt(k(network, "sc"), m.signerCount)
            .putString(k(network, "ts"), b64(m.thresholdSalt))
            .putString(k(network, "ms"), b64(m.memberSalt))
            .putBoolean(k(network, "creator"), m.isCreator)
            .putStringSet(k(network, "invites"), m.invites.toSet())
            .apply()
    }

    // "Did I approve?" is tracked LOCALLY per (network, VAULT) — the contract's approval tags are
    // unlinkable and not queryable. Keying by address (not just network) stops one vault's approvals
    // leaking into another vault reached on the same network. Known caveat: this is device-local,
    // so a reinstall forgets it (re-approving then trips the contract's duplicate-approve assert).
    fun approvedIds(network: MidnightNetwork, address: String): Set<Long> =
        prefs.getStringSet(approvedKey(network, address), emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet().orEmpty()

    fun markApproved(network: MidnightNetwork, address: String, id: Long, approved: Boolean) {
        val cur = approvedIds(network, address).toMutableSet()
        if (approved) cur.add(id) else cur.remove(id)
        prefs.edit().putStringSet(approvedKey(network, address), cur.map { it.toString() }.toSet()).apply()
    }

    fun clear(network: MidnightNetwork, address: String?) {
        val e = prefs.edit()
            .remove(k(network, "addr")).remove(k(network, "vk")).remove(k(network, "th"))
            .remove(k(network, "sc")).remove(k(network, "ts")).remove(k(network, "ms"))
            .remove(k(network, "creator")).remove(k(network, "invites"))
        if (address != null) e.remove(approvedKey(network, address))
        e.apply()
    }

    private fun approvedKey(network: MidnightNetwork, address: String) = "pvault.approved.${network.name}.$address"

    private fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun b64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}

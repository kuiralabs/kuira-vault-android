package com.kuiralabs.starter.counter.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.midnight.kuira.core.network.MidnightNetwork
import org.json.JSONArray
import org.json.JSONObject
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
        /** Encoded invites to re-share (creator only), in co-signer order (index 0 = co-signer 2). */
        val invites: List<String>,
        /** Each co-signer's own coin-key hex, index-aligned with [invites], so the creator can match
         *  an invite to the person they typed. Creator-only; empty for a joiner. */
        val coSignerKeyHexes: List<String> = emptyList(),
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
            // Ordered JSON (a StringSet loses order → mislabeled "Co-signer 2/3" + misaligned keys).
            invites = jsonList(prefs.getString(k(network, "invites"), null)),
            coSignerKeyHexes = jsonList(prefs.getString(k(network, "cosignerKeys"), null)),
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
            .putString(k(network, "invites"), JSONArray(m.invites).toString())
            .putString(k(network, "cosignerKeys"), JSONArray(m.coSignerKeyHexes).toString())
            .apply()
    }

    /** Parse a stored JSON string array (ordered) → list; empty on null/blank/malformed. */
    private fun jsonList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { JSONArray(json).let { a -> (0 until a.length()).map { a.getString(it) } } }
            .getOrDefault(emptyList())
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
            .remove(k(network, "creator")).remove(k(network, "invites")).remove(k(network, "cosignerKeys"))
        if (address != null) e.remove(approvedKey(network, address))
        e.apply()
    }

    private fun approvedKey(network: MidnightNetwork, address: String) = "pvault.approved.${network.name}.$address"

    // ── Cloud-backup round-trip (rides the Sigil app-data backup) ──
    // A reinstall wipes EncryptedSharedPreferences, so without this a member loses their whole vault
    // (viewing key + salts are unrecoverable — the on-chain state is opaque) and their approval
    // state. [snapshotBytes]/[restoreFromBytes] let a PrivateVaultBackupProvider fold this into the
    // sigil's cloud backup, which is PRF-encrypted under the passkey — same protection as the seed.
    // Versioned so a future schema can decline an unrecognized blob rather than crash.

    fun snapshotBytes(): ByteArray? {
        val nets = JSONObject()
        for (network in MidnightNetwork.values()) {
            val m = get(network) ?: continue
            nets.put(network.name, JSONObject()
                .put("addr", m.address)
                .put("vk", b64(m.viewingKey))
                .put("th", m.threshold)
                .put("sc", m.signerCount)
                .put("ts", b64(m.thresholdSalt))
                .put("ms", b64(m.memberSalt))
                .put("creator", m.isCreator)
                .put("invites", JSONArray(m.invites))
                .put("cknh", JSONArray(m.coSignerKeyHexes))
                .put("approved", JSONArray(approvedIds(network, m.address).sorted())))
        }
        if (nets.length() == 0) return null
        return JSONObject().put("v", SNAPSHOT_VERSION).put("nets", nets)
            .toString().toByteArray(Charsets.UTF_8)
    }

    fun restoreFromBytes(bytes: ByteArray) {
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        if (root.optInt("v") != SNAPSHOT_VERSION) return // decline unknown schema, don't crash
        val nets = root.optJSONObject("nets") ?: return
        for (name in nets.keys()) {
            val network = runCatching { MidnightNetwork.valueOf(name) }.getOrNull() ?: continue
            val o = nets.getJSONObject(name)
            save(network, Membership(
                address = o.getString("addr"),
                viewingKey = b64(o.getString("vk")),
                threshold = o.getInt("th"),
                signerCount = o.getInt("sc"),
                thresholdSalt = b64(o.getString("ts")),
                memberSalt = b64(o.getString("ms")),
                isCreator = o.getBoolean("creator"),
                invites = jsonList(o.optJSONArray("invites")?.toString()),
                coSignerKeyHexes = jsonList(o.optJSONArray("cknh")?.toString()),
            ))
            o.optJSONArray("approved")?.let { arr ->
                for (i in 0 until arr.length()) markApproved(network, o.getString("addr"), arr.getLong(i), true)
            }
        }
    }

    private fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun b64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    private companion object {
        const val SNAPSHOT_VERSION = 1
    }
}

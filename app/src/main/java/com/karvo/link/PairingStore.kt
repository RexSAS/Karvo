package com.karvo.link

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** A linked PC. [secret] is null when it can't be unwrapped any more: the link is broken and can only be removed. */
class LinkedPc(val roomId: String, val secret: ByteArray?, val pcName: String, val linkedAt: Instant)

/** Linked PCs, as a JSON array in their own DataStore file; secrets are sealed by [box]. */
class PairingStore(
    private val dataStore: DataStore<Preferences>,
    private val box: SecretBox,
    private val now: () -> Instant = Instant::now,
) {
    private val key = stringPreferencesKey("pcs")

    private fun read(p: Preferences): List<JSONObject> =
        JSONArray(p[key] ?: "[]").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

    val pcs: Flow<List<LinkedPc>> = dataStore.data.map { p ->
        read(p).map { o ->
            LinkedPc(
                roomId = o.getString("roomId"),
                secret = runCatching { box.open(LinkCrypto.fromB64url(o.getString("sealed"))) }.getOrNull(),
                pcName = o.getString("pcName"),
                linkedAt = Instant.parse(o.getString("linkedAt")),
            )
        }
    }

    private suspend fun update(change: (MutableList<JSONObject>) -> Unit) {
        dataStore.edit { p -> p[key] = JSONArray(read(p).toMutableList().also(change)).toString() }
    }

    suspend fun add(qr: LinkQr) {
        val roomId = LinkCrypto.b64url(qr.roomId)
        update { list ->
            list.removeAll { it.getString("roomId") == roomId }
            list += JSONObject().put("roomId", roomId).put("sealed", LinkCrypto.b64url(box.seal(qr.secret)))
                .put("pcName", qr.pcName).put("linkedAt", now().toString())
        }
    }

    suspend fun rename(roomId: String, pcName: String) =
        update { list -> list.filter { it.getString("roomId") == roomId }.forEach { it.put("pcName", pcName) } }

    suspend fun remove(roomId: String) = update { list -> list.removeAll { it.getString("roomId") == roomId } }
}

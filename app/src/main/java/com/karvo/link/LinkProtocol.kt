package com.karvo.link

import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException

const val PROTOCOL_VERSION = 1

class ProtocolException(message: String, val code: String = "invalid", val peerVersion: Int? = null) : Exception(message)

/** A decoded message; [isNewHello] is false for a hello that repeats the current peer session. */
class Decoded(val type: String, val json: JSONObject, val isNewHello: Boolean = false)

/**
 * One socket's message channel, identical to karvo-web/link/protocol.js. Every message names the receiver's
 * session (s), the sender's session (f) and a sequence number (q), so recorded frames can't be replayed.
 * Not thread-safe: the client decodes and encodes one frame at a time, in order.
 */
class LinkChannel(
    private val sendKey: ByteArray, private val recvKey: ByteArray, private val roomId: ByteArray,
    private val sendDir: Int, private val recvDir: Int,
) {
    val mySession: String = LinkCrypto.b64url(LinkCrypto.randomBytes(16))
    var peerSession: String? = null
        private set
    private var sendQ = 0L
    private var lastQ = 0L
    private val seen = mutableSetOf<String>() // switching back to an earlier peer session would reset q

    private fun frame(o: JSONObject) = LinkCrypto.seal(sendKey, roomId, sendDir, o.toString().toByteArray())

    fun hello(app: String, name: String): ByteArray = frame(JSONObject()
        .put("app", app).put("name", name).put("s", JSONObject.NULL).put("f", mySession)
        .put("q", 0).put("t", "hello").put("v", PROTOCOL_VERSION).put("session", mySession))

    fun encode(type: String, fields: JSONObject = JSONObject()): ByteArray {
        val peer = peerSession ?: throw ProtocolException("peer hello not received yet", "state")
        return frame(fields.put("s", peer).put("f", mySession).put("q", ++sendQ).put("t", type))
    }

    fun decode(bytes: ByteArray): Decoded {
        val msg = try {
            JSONObject(String(LinkCrypto.open(recvKey, roomId, recvDir, bytes)))
        } catch (e: GeneralSecurityException) { throw ProtocolException("undecryptable frame") }
        catch (e: JSONException) { throw ProtocolException("bad JSON") }
        val type = msg.optString("t")
        if (type == "hello") {
            val v = msg.optInt("v", -1)
            if (v != PROTOCOL_VERSION) throw ProtocolException("protocol version mismatch", "version", v)
            val session = msg.optString("session")
            if (session.isEmpty()) throw ProtocolException("hello without session")
            if (session == peerSession) return Decoded(type, msg, isNewHello = false) // re-announcement: no reset
            if (!seen.add(session)) throw ProtocolException("hello from an earlier session")
            peerSession = session; lastQ = 0 // a (re)connected peer starts a new session
            return Decoded(type, msg, isNewHello = true)
        }
        val peer = peerSession ?: throw ProtocolException("message before hello")
        if (msg.optString("s") != mySession || msg.optString("f") != peer) throw ProtocolException("wrong session")
        val q = msg.optLong("q", 0)
        if (q <= lastQ) throw ProtocolException("replayed or out-of-order message")
        lastQ = q
        return Decoded(type, msg)
    }
}

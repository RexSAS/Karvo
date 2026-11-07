package com.karvo.link

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The four 32-byte secrets derived from a pairing (spec "Crypto"). */
class LinkKeys(val pcToPhone: ByteArray, val phoneToPc: ByteArray, val authPc: ByteArray, val authPhone: ByteArray)

/**
 * Link encryption, byte-identical to karvo-web/link/crypto.js (both sides test against link-v1.json):
 * HKDF-SHA256 keys from the QR secret, AES-256-GCM frames = 12-byte nonce + ciphertext + tag, AAD = roomId + direction.
 */
object LinkCrypto {
    private val random = SecureRandom()

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also(random::nextBytes)
    fun b64url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    fun fromB64url(s: String): ByteArray = Base64.getUrlDecoder().decode(s)
    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** RFC 5869 with a single 32-byte output block. */
    private fun hkdf(secret: ByteArray, salt: ByteArray, info: String): ByteArray {
        val prk = Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(salt, "HmacSHA256")); doFinal(secret) }
        return Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(prk, "HmacSHA256")); update(info.toByteArray()); update(1); doFinal() }
    }

    fun deriveKeys(secret: ByteArray, roomId: ByteArray): LinkKeys {
        fun k(s: String) = hkdf(secret, roomId, "karvo-link v1 $s")
        return LinkKeys(k("pc->phone"), k("phone->pc"), k("auth pc"), k("auth phone"))
    }

    private fun aad(roomId: ByteArray, direction: Int) = roomId + direction.toByte()

    // Random nonces, not counters: the same keys are reused across sessions, so a counter would repeat.
    fun seal(key: ByteArray, roomId: ByteArray, direction: Int, plaintext: ByteArray, nonce: ByteArray = randomBytes(12)): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        c.updateAAD(aad(roomId, direction))
        return nonce + c.doFinal(plaintext)
    }

    /** Throws GeneralSecurityException for anything that doesn't decrypt with this key, room and direction. */
    fun open(key: ByteArray, roomId: ByteArray, direction: Int, frame: ByteArray): ByteArray {
        if (frame.size < 28) throw java.security.GeneralSecurityException("frame too short")
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, frame, 0, 12))
        c.updateAAD(aad(roomId, direction))
        return c.doFinal(frame, 12, frame.size - 12)
    }
}

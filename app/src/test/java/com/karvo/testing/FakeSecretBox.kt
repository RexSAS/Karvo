package com.karvo.testing

import com.karvo.link.SecretBox

/** Reversible stand-in for the Keystore box; [broken] simulates a key that is gone (data restored on another phone). */
class FakeSecretBox(var broken: Boolean = false) : SecretBox {
    override fun seal(plain: ByteArray) = plain.map { (it.toInt() xor 0x5a).toByte() }.toByteArray()
    override fun open(sealed: ByteArray): ByteArray { if (broken) error("key gone"); return seal(sealed) }
}

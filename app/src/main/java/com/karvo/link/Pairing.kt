package com.karvo.link

import java.net.URLDecoder

/** What the PC's QR code carries (spec "Pairing"): `karvo:link?v=1&r=<roomId>&k=<secret>&n=<PC name>`. */
class LinkQr(val roomId: ByteArray, val secret: ByteArray, val pcName: String)

/** Throws IllegalArgumentException with message "not_karvo", "version" or "damaged". */
fun parseLinkQr(text: String): LinkQr {
    val prefix = "karvo:link?"
    require(text.startsWith(prefix)) { "not_karvo" }
    val params = text.removePrefix(prefix).split('&').mapNotNull {
        val i = it.indexOf('=')
        if (i < 0) null else it.substring(0, i) to URLDecoder.decode(it.substring(i + 1), "UTF-8")
    }.toMap()
    require(params["v"] == "1") { "version" }
    val roomId = runCatching { LinkCrypto.fromB64url(params["r"].orEmpty()) }.getOrNull()
    val secret = runCatching { LinkCrypto.fromB64url(params["k"].orEmpty()) }.getOrNull()
    require(roomId?.size == 16 && secret?.size == 32) { "damaged" }
    return LinkQr(roomId!!, secret!!, params["n"].orEmpty())
}

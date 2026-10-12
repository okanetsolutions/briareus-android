package com.okanetsolutions.briareus.core

import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The Cloudflare Access service token the server hands a manage token (`GET /preview/access`), so a ▶ Run preview opens
 * without Access's emailed code. It opens every preview, so it goes only to hosts under [hostSuffix], over HTTPS.
 */
data class PreviewAccess(val clientId: String, val clientSecret: String, val hostSuffix: String) {
    /** The headers Access reads, for a request to [url]; empty for any other host. */
    fun headersFor(url: String): Map<String, String> =
        if (applies(url, hostSuffix)) mapOf("CF-Access-Client-Id" to clientId, "CF-Access-Client-Secret" to clientSecret) else emptyMap()

    companion object {
        fun parse(o: JsonObject): PreviewAccess? {
            val suffix = o.nonEmpty("hostSuffix")?.trim('.')?.ifEmpty { null } ?: return null
            return PreviewAccess(o.nonEmpty("clientId") ?: return null, o.nonEmpty("clientSecret") ?: return null, suffix)
        }

        /** Whether [url] is an HTTPS address on a host strictly under [hostSuffix]. The Windows client's rule. */
        fun applies(url: String, hostSuffix: String): Boolean {
            val suffix = hostSuffix.removePrefix(".")
            if (suffix.isEmpty()) return false
            val host = previewUrl(url)?.host?.trimEnd('.') ?: return false
            return host.length > suffix.length + 1 && host.endsWith(".$suffix", ignoreCase = true)
        }
    }
}

/** Parse once with browser-compatible URL normalization before applying credential or navigation policy. */
fun previewUrl(value: String): HttpUrl? = value.toHttpUrlOrNull()?.takeIf {
    it.isHttps && it.username.isEmpty() && it.password.isEmpty()
}

class PreviewOrigin(url: String) {
    private val origin = previewUrl(url)

    fun contains(url: String): Boolean {
        val destination = previewUrl(url) ?: return false
        return origin != null && destination.host == origin.host && destination.port == origin.port
    }
}

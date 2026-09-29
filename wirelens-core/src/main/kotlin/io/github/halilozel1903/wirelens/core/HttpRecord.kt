package io.github.halilozel1903.wirelens.core

import java.net.URI

/** One HTTP header. A list of these keeps the order and repeated names as they were sent. */
public data class Header(val name: String, val value: String)

/**
 * A captured request or response body.
 *
 * @property text The body as text, cut at the configured size. `null` for binary bodies.
 * @property byteCount The full size in bytes, or `-1` when the size is unknown.
 * @property isTruncated Whether [text] holds only the start of the body.
 * @property contentType The `Content-Type` of the body, if any.
 */
public data class CapturedBody(
    val text: String?,
    val byteCount: Long,
    val isTruncated: Boolean = false,
    val contentType: String? = null,
) {
    /** Whether the body is not text (an image, a protobuf, a zip, ...). */
    val isBinary: Boolean get() = text == null && byteCount != 0L

    /** Whether the content type or the text itself says this is JSON. */
    val isJson: Boolean
        get() {
            val type = contentType?.lowercase()
            if (type != null && (type.contains("/json") || type.contains("+json"))) return true
            val start = text?.trimStart()?.firstOrNull() ?: return false
            return start == '{' || start == '['
        }

    public companion object {
        /** A text body with a known size. */
        public fun text(text: String, contentType: String? = "application/json"): CapturedBody =
            CapturedBody(text, text.toByteArray().size.toLong(), contentType = contentType)
    }
}

/**
 * One captured exchange: the request and, once it finished, its response or error.
 * Headers and bodies are already redacted and cut when a record is created.
 */
public data class HttpRecord(
    val id: Long,
    val method: String,
    val url: String,
    val requestHeaders: List<Header> = emptyList(),
    val requestBody: CapturedBody? = null,
    /** Wall clock time the request started, in epoch milliseconds. */
    val startedAtMillis: Long,
    /** Milliseconds until the response headers arrived or the call failed. `null` while running. */
    val tookMillis: Long? = null,
    val statusCode: Int? = null,
    /** The reason phrase the server sent, often empty with HTTP/2. */
    val statusMessage: String = "",
    /** `http/1.1`, `h2`, ... */
    val protocol: String? = null,
    val responseHeaders: List<Header> = emptyList(),
    val responseBody: CapturedBody? = null,
    /** The failure, when the call ended without a response. */
    val error: String? = null,
) {
    /** Whether the call finished, successfully or not. */
    val isComplete: Boolean get() = tookMillis != null || error != null

    val category: StatusCategory get() = StatusCategory.of(statusCode, error != null, isComplete)

    private val uri: URI? by lazy { runCatching { URI(url) }.getOrNull() }

    /** `https` or `http`. */
    val scheme: String get() = uri?.scheme ?: url.substringBefore("://", "")

    val host: String get() = uri?.host ?: url.substringAfter("://").substringBefore('/').substringBefore('?')

    /** The path and query, for example `/v2/products?page=1`. */
    val path: String
        get() {
            val u = uri ?: return url
            val path = u.rawPath.takeUnless { it.isNullOrEmpty() } ?: "/"
            val query = u.rawQuery
            return if (query.isNullOrEmpty()) path else "$path?$query"
        }

    /** Decoded query parameters in order. */
    val queryParameters: List<Header>
        get() {
            val query = uri?.rawQuery ?: return emptyList()
            return query.split('&').filter { it.isNotEmpty() }.map { part ->
                val name = part.substringBefore('=')
                val value = if ('=' in part) part.substringAfter('=') else ""
                Header(decode(name), decode(value))
            }
        }

    public companion object {
        /** Case-insensitive lookup of the first header called [name]. */
        public fun header(name: String, headers: List<Header>): String? =
            headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value

        private fun decode(value: String): String =
            runCatching { java.net.URLDecoder.decode(value, Charsets.UTF_8) }.getOrDefault(value)
    }
}

/** The coarse outcome of a request, used for colors and filtering. */
public enum class StatusCategory(public val label: String) {
    Pending("Pending"),
    Success("2xx"),
    Redirect("3xx"),
    ClientError("4xx"),
    ServerError("5xx"),
    Failed("Failed");

    public companion object {
        public fun of(statusCode: Int?, hasError: Boolean, isComplete: Boolean): StatusCategory = when {
            hasError -> Failed
            statusCode == null -> if (isComplete) Failed else Pending
            statusCode < 300 -> Success
            statusCode < 400 -> Redirect
            statusCode < 500 -> ClientError
            else -> ServerError
        }
    }
}

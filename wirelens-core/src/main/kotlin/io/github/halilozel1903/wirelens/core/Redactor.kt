package io.github.halilozel1903.wirelens.core

/**
 * Hides secrets before a request is stored: header values, query parameters and
 * string values of JSON keys. Names are matched case-insensitively.
 */
public class Redactor(
    headers: Set<String> = DefaultHeaders,
    queryParameters: Set<String> = DefaultQueryParameters,
    jsonKeys: Set<String> = DefaultJsonKeys,
) {
    public val headerNames: Set<String> = headers.mapTo(HashSet()) { it.lowercase() }
    public val queryParameterNames: Set<String> = queryParameters.mapTo(HashSet()) { it.lowercase() }
    public val jsonKeyNames: Set<String> = jsonKeys.mapTo(HashSet()) { it.lowercase() }

    public fun isSensitiveHeader(name: String): Boolean = name.lowercase() in headerNames

    /** [headers] with every sensitive value replaced by [Placeholder]. */
    public fun redactHeaders(headers: List<Header>): List<Header> =
        if (headerNames.isEmpty()) headers
        else headers.map { if (isSensitiveHeader(it.name)) it.copy(value = Placeholder) else it }

    /** [url] with the values of sensitive query parameters replaced. Everything else is untouched. */
    public fun redactUrl(url: String): String {
        if (queryParameterNames.isEmpty()) return url
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url
        val fragmentStart = url.indexOf('#', queryStart).let { if (it < 0) url.length else it }
        val query = url.substring(queryStart + 1, fragmentStart)
        val redacted = query.split('&').joinToString("&") { part ->
            val name = part.substringBefore('=')
            val decoded = runCatching { java.net.URLDecoder.decode(name, Charsets.UTF_8) }.getOrDefault(name)
            if ('=' in part && decoded.lowercase() in queryParameterNames) "$name=$Placeholder" else part
        }
        return url.substring(0, queryStart + 1) + redacted + url.substring(fragmentStart)
    }

    /**
     * [json] with the string values of sensitive keys replaced, at any depth.
     * Formatting, key order and every other value stay exactly as they were.
     */
    public fun redactJson(json: String): String {
        if (jsonKeyNames.isEmpty()) return json
        val tokens = JsonFormatter.tokenize(json)
        var pendingKey = false
        var changed = false
        val out = StringBuilder(json.length)
        for (token in tokens) {
            when (token.kind) {
                JsonToken.Kind.Key -> {
                    pendingKey = token.text.removeSurrounding("\"").lowercase() in jsonKeyNames
                    out.append(token.text)
                }
                JsonToken.Kind.Whitespace -> out.append(token.text)
                JsonToken.Kind.Punctuation -> {
                    if (token.text != ":") pendingKey = false
                    out.append(token.text)
                }
                JsonToken.Kind.String -> {
                    if (pendingKey) {
                        out.append('"').append(Placeholder).append('"')
                        changed = true
                    } else {
                        out.append(token.text)
                    }
                    pendingKey = false
                }
                else -> {
                    pendingKey = false
                    out.append(token.text)
                }
            }
        }
        return if (changed) out.toString() else json
    }

    public companion object {
        /** What a redacted value looks like. */
        public const val Placeholder: String = "••••••"

        public val DefaultHeaders: Set<String> = setOf(
            "Authorization", "Proxy-Authorization", "Cookie", "Set-Cookie", "X-Api-Key", "X-Auth-Token",
        )

        public val DefaultQueryParameters: Set<String> = setOf(
            "token", "access_token", "api_key", "apikey", "key", "signature", "sig", "password",
        )

        public val DefaultJsonKeys: Set<String> = setOf(
            "password", "access_token", "accessToken", "refresh_token", "refreshToken", "id_token",
            "idToken", "client_secret", "clientSecret", "token",
        )

        /** Keeps everything as it is. */
        public val None: Redactor = Redactor(emptySet(), emptySet(), emptySet())
    }
}

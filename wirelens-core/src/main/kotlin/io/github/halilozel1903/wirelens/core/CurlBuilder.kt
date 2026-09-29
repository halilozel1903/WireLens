package io.github.halilozel1903.wirelens.core

/** Builds a `curl` command that repeats a request in the terminal. */
public object CurlBuilder {
    /** Headers curl computes itself; copying them can only make the command wrong. */
    private val skippedHeaders = setOf("content-length", "host", "accept-encoding", "connection")

    /**
     * A curl command for [record]. Redacted values stay redacted.
     * A body that was cut or is binary is left out, with a comment line saying so.
     */
    public fun command(record: HttpRecord, multiline: Boolean = true): String {
        val notes = mutableListOf<String>()
        var body = record.requestBody?.text
        val captured = record.requestBody
        if (captured != null && captured.isTruncated) {
            notes += "# Request body (${WireFormat.bytes(captured.byteCount)}) was cut when captured and is not included."
            body = null
        } else if (captured != null && captured.isBinary) {
            notes += "# Binary request body (${WireFormat.bytes(captured.byteCount)}) is not included."
        }
        val curl = command(record.method, record.url, record.requestHeaders, body, multiline)
        return (notes + curl).joinToString("\n")
    }

    /**
     * ```
     * curl -X POST 'https://api.example.com/users' \
     *   -H 'Content-Type: application/json' \
     *   --data-raw '{"name":"Ada"}'
     * ```
     */
    public fun command(
        method: String,
        url: String,
        headers: List<Header> = emptyList(),
        body: String? = null,
        multiline: Boolean = true,
    ): String {
        val upper = method.uppercase()
        val text = body?.takeIf { it.isNotEmpty() }
        val first = mutableListOf("curl")
        when (upper) {
            "HEAD" -> first += "--head"
            "GET" -> if (text != null) first += "-X GET"
            else -> first += "-X $upper"
        }
        first += shellQuoted(url)
        val parts = mutableListOf(first.joinToString(" "))
        headers
            .filter { it.name.lowercase() !in skippedHeaders }
            .forEach { parts += "-H " + shellQuoted("${it.name}: ${it.value}") }
        if (text != null) parts += "--data-raw " + shellQuoted(text)
        return parts.joinToString(if (multiline) " \\\n  " else " ")
    }

    /** Wraps [value] in single quotes for POSIX shells. */
    public fun shellQuoted(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}

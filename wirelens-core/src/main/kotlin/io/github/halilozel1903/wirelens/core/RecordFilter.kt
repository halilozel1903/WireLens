package io.github.halilozel1903.wirelens.core

/** Search text plus method and status filters for the request list. */
public data class RecordFilter(
    /** Whitespace-separated terms; every term must appear in the method, URL, status code or error. */
    val query: String = "",
    /** Uppercased methods to show. Empty shows all. */
    val methods: Set<String> = emptySet(),
    /** Status categories to show. Empty shows all. */
    val categories: Set<StatusCategory> = emptySet(),
) {
    val terms: List<String> get() = query.split(Regex("\\s+")).filter { it.isNotEmpty() }.map { it.lowercase() }

    val isActive: Boolean get() = terms.isNotEmpty() || methods.isNotEmpty() || categories.isNotEmpty()

    public fun matches(record: HttpRecord): Boolean {
        if (methods.isNotEmpty() && record.method.uppercase() !in methods) return false
        if (categories.isNotEmpty() && record.category !in categories) return false
        val terms = terms
        if (terms.isEmpty()) return true
        val haystack = searchableText(record)
        return terms.all { it in haystack }
    }

    /** The records that match, in their original order. */
    public fun apply(records: List<HttpRecord>): List<HttpRecord> =
        if (isActive) records.filter(::matches) else records

    public fun toggle(method: String): RecordFilter {
        val upper = method.uppercase()
        return copy(methods = if (upper in methods) methods - upper else methods + upper)
    }

    public fun toggle(category: StatusCategory): RecordFilter =
        copy(categories = if (category in categories) categories - category else categories + category)

    public companion object {
        private val methodOrder = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")

        /** The distinct methods in [records], common ones first. */
        public fun methodsIn(records: List<HttpRecord>): List<String> =
            records.map { it.method.uppercase() }.distinct().sortedWith(
                compareBy<String> { methodOrder.indexOf(it).let { i -> if (i < 0) methodOrder.size else i } }.thenBy { it },
            )

        internal fun searchableText(record: HttpRecord): String = buildString {
            append(record.method).append(' ').append(record.url)
            runCatching { java.net.URLDecoder.decode(record.url, Charsets.UTF_8) }.getOrNull()?.let { append(' ').append(it) }
            record.statusCode?.let { append(' ').append(it) }
            record.error?.let { append(' ').append(it) }
        }.lowercase()
    }
}

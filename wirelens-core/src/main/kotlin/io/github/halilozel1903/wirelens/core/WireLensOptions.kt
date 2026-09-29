package io.github.halilozel1903.wirelens.core

/**
 * What the interceptor records.
 *
 * @property maxBodyBytes Request and response bodies are cut at this size. The full size is still shown.
 * @property redactor Hides header values, query parameters and JSON fields before anything is stored.
 * @property ignoredHosts Hosts that are never recorded. `example.com` also covers `api.example.com`.
 */
public data class WireLensOptions(
    val maxBodyBytes: Long = 256L * 1024,
    val redactor: Redactor = Redactor(),
    val ignoredHosts: Set<String> = emptySet(),
) {
    public fun isIgnored(host: String): Boolean {
        val lower = host.lowercase()
        return ignoredHosts.any { val h = it.lowercase(); lower == h || lower.endsWith(".$h") }
    }
}

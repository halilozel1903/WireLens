package io.github.halilozel1903.wirelens.core

import java.util.Locale
import kotlin.math.roundToLong

/** Locale-independent formatting for sizes, durations and status codes. */
public object WireFormat {
    /** `512 B`, `1.5 KB`, `18 KB`, `2.4 MB`, or `?` when unknown. Uses 1024-byte units. */
    public fun bytes(count: Long): String {
        if (count < 0) return "?"
        if (count < 1024) return "$count B"
        var value = count.toDouble()
        var unit = "B"
        for (next in listOf("KB", "MB", "GB", "TB")) {
            value /= 1024
            unit = next
            if (value < 1024) break
        }
        return if (value < 10) String.format(Locale.US, "%.1f %s", value, unit) else "${value.roundToLong()} $unit"
    }

    /** `84 ms`, `1.24 s`, `2m 05s`. */
    public fun duration(millis: Long): String {
        val ms = millis.coerceAtLeast(0)
        if (ms < 1000) return "$ms ms"
        if (ms < 60_000) return String.format(Locale.US, "%.2f s", ms / 1000.0)
        val seconds = (ms / 1000.0).roundToLong()
        return "${seconds / 60}m ${(seconds % 60).toString().padStart(2, '0')}s"
    }

    /** `404 Not Found`. */
    public fun status(code: Int): String = HttpStatus.reasonPhrase(code).let { if (it.isEmpty()) "$code" else "$code $it" }

    /** `09:41:07`, from epoch milliseconds in the default time zone. */
    public fun time(epochMillis: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): String {
        val calendar = java.util.Calendar.getInstance(zone).apply { timeInMillis = epochMillis }
        return String.format(
            Locale.US, "%02d:%02d:%02d",
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            calendar.get(java.util.Calendar.SECOND),
        )
    }
}

/** Reason phrases for common status codes. */
public object HttpStatus {
    private val phrases = mapOf(
        100 to "Continue", 101 to "Switching Protocols",
        200 to "OK", 201 to "Created", 202 to "Accepted", 204 to "No Content", 206 to "Partial Content",
        301 to "Moved Permanently", 302 to "Found", 303 to "See Other", 304 to "Not Modified",
        307 to "Temporary Redirect", 308 to "Permanent Redirect",
        400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden", 404 to "Not Found",
        405 to "Method Not Allowed", 408 to "Request Timeout", 409 to "Conflict", 410 to "Gone",
        413 to "Payload Too Large", 415 to "Unsupported Media Type", 422 to "Unprocessable Content",
        429 to "Too Many Requests",
        500 to "Internal Server Error", 501 to "Not Implemented", 502 to "Bad Gateway",
        503 to "Service Unavailable", 504 to "Gateway Timeout",
    )

    public fun reasonPhrase(code: Int): String = phrases[code] ?: ""
}

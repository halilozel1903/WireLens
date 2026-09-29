package io.github.halilozel1903.wirelens.core

/** Totals for the strip at the top of the request list. */
public data class TrafficSummary(
    val requests: Int,
    /** 4xx, 5xx and failed calls. */
    val errors: Int,
    /** Response bytes with a known size. */
    val receivedBytes: Long,
    /** Average time of finished calls, `null` when none finished. */
    val averageMillis: Long?,
) {
    public companion object {
        public fun of(records: List<HttpRecord>): TrafficSummary {
            val finished = records.mapNotNull { it.tookMillis }
            return TrafficSummary(
                requests = records.size,
                errors = records.count {
                    it.category == StatusCategory.ClientError || it.category == StatusCategory.ServerError || it.category == StatusCategory.Failed
                },
                receivedBytes = records.sumOf { (it.responseBody?.byteCount ?: 0L).coerceAtLeast(0L) },
                averageMillis = if (finished.isEmpty()) null else finished.sum() / finished.size,
            )
        }
    }
}

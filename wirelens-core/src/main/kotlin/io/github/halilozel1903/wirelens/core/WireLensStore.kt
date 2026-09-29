package io.github.halilozel1903.wirelens.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/**
 * Keeps the captured records in memory, newest first. Thread safe.
 *
 * @param maxRecords How many records are kept; the oldest go first.
 */
public class WireLensStore(
    records: List<HttpRecord> = emptyList(),
    maxRecords: Int = DefaultMaxRecords,
) {
    private val nextId = AtomicLong((records.maxOfOrNull { it.id } ?: 0) + 1)
    private val state = MutableStateFlow(records.take(maxRecords))

    @Volatile
    public var maxRecords: Int = maxRecords
        set(value) {
            field = value.coerceAtLeast(1)
            state.update { it.take(field) }
        }

    /** Newest first. */
    public val records: StateFlow<List<HttpRecord>> = state.asStateFlow()

    /** A fresh id for a new record. */
    public fun newId(): Long = nextId.getAndIncrement()

    /** Adds [record] at the top, or replaces the record with the same id. */
    public fun put(record: HttpRecord) {
        nextId.accumulateAndGet(record.id + 1) { a, b -> maxOf(a, b) }
        state.update { current ->
            val index = current.indexOfFirst { it.id == record.id }
            if (index >= 0) current.toMutableList().also { it[index] = record }
            else (listOf(record) + current).take(maxRecords)
        }
    }

    public fun get(id: Long): HttpRecord? = state.value.firstOrNull { it.id == id }

    public fun clear() {
        state.value = emptyList()
    }

    public companion object {
        public const val DefaultMaxRecords: Int = 500
    }
}

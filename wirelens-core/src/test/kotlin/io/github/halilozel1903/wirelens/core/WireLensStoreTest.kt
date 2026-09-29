package io.github.halilozel1903.wirelens.core

import kotlin.test.Test
import kotlin.test.assertEquals

class WireLensStoreTest {
    private fun record(id: Long) = HttpRecord(id, "GET", "https://x.io/$id", startedAtMillis = id)

    @Test
    fun newestFirstAndReplacesById() {
        val store = WireLensStore()
        store.put(record(1))
        store.put(record(2))
        store.put(record(1).copy(statusCode = 200))
        assertEquals(listOf(2L, 1L), store.records.value.map { it.id })
        assertEquals(200, store.get(1)?.statusCode)
    }

    @Test
    fun keepsAtMostMaxRecords() {
        val store = WireLensStore(maxRecords = 2)
        (1L..4L).forEach { store.put(record(it)) }
        assertEquals(listOf(4L, 3L), store.records.value.map { it.id })
        store.clear()
        assertEquals(emptyList(), store.records.value)
    }
}

class SampleTrafficTest {
    @Test
    fun coversEveryCategoryNewestFirst() {
        val records = SampleTraffic.records(nowMillis = 1_000_000)
        assertEquals(StatusCategory.entries.toSet(), records.map { it.category }.toSet())
        assertEquals(records.sortedByDescending { it.startedAtMillis }, records)
        assertEquals(records.size, records.map { it.id }.toSet().size)
    }
}

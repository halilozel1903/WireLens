package io.github.halilozel1903.wirelens.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordFilterTest {
    private val records = listOf(
        HttpRecord(1, "GET", "https://api.shop.io/v2/products?page=1", startedAtMillis = 0, tookMillis = 80, statusCode = 200),
        HttpRecord(2, "POST", "https://api.shop.io/v2/cart/items", startedAtMillis = 0, tookMillis = 120, statusCode = 500),
        HttpRecord(3, "DELETE", "https://api.shop.io/v2/cart/items/7", startedAtMillis = 0, tookMillis = 40, statusCode = 404),
        HttpRecord(4, "GET", "https://cdn.shop.io/img.png", startedAtMillis = 0, tookMillis = 900, error = "SocketTimeoutException: timeout"),
        HttpRecord(5, "PATCH", "https://api.shop.io/v2/me", startedAtMillis = 0),
    )

    @Test
    fun searchTermsMustAllMatch() {
        assertEquals(listOf(2L, 3L), RecordFilter("cart items").apply(records).map { it.id })
        assertEquals(listOf(2L), RecordFilter("cart 500").apply(records).map { it.id })
        assertEquals(listOf(4L), RecordFilter("TIMEOUT").apply(records).map { it.id })
    }

    @Test
    fun methodAndCategoryFilters() {
        val filter = RecordFilter().toggle("get").toggle(StatusCategory.Failed)
        assertEquals(listOf(4L), filter.apply(records).map { it.id })
        assertEquals(listOf(2L), RecordFilter(categories = setOf(StatusCategory.ServerError)).apply(records).map { it.id })
        assertEquals(listOf(5L), RecordFilter(categories = setOf(StatusCategory.Pending)).apply(records).map { it.id })
    }

    @Test
    fun methodsAreSortedCommonFirst() {
        assertEquals(listOf("GET", "POST", "PATCH", "DELETE"), RecordFilter.methodsIn(records))
    }

    @Test
    fun summaryCountsErrorsAndAverages() {
        val summary = TrafficSummary.of(records)
        assertEquals(5, summary.requests)
        assertEquals(3, summary.errors)
        assertEquals(285L, summary.averageMillis)
    }
}

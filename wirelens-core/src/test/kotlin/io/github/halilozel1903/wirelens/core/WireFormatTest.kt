package io.github.halilozel1903.wirelens.core

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class WireFormatTest {
    @Test
    fun bytes() {
        assertEquals("512 B", WireFormat.bytes(512))
        assertEquals("1.5 KB", WireFormat.bytes(1536))
        assertEquals("18 KB", WireFormat.bytes(18_841))
        assertEquals("2.4 MB", WireFormat.bytes(2_500_000))
        assertEquals("?", WireFormat.bytes(-1))
    }

    @Test
    fun duration() {
        assertEquals("84 ms", WireFormat.duration(84))
        assertEquals("1.24 s", WireFormat.duration(1240))
        assertEquals("2m 05s", WireFormat.duration(125_000))
    }

    @Test
    fun statusAndTime() {
        assertEquals("404 Not Found", WireFormat.status(404))
        assertEquals("299", WireFormat.status(299))
        assertEquals("09:41:07", WireFormat.time(34_867_000, TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun recordUrlParts() {
        val record = HttpRecord(1, "get", "https://api.shop.io/v2/search?q=red%20shoes&page=2", startedAtMillis = 0)
        assertEquals("api.shop.io", record.host)
        assertEquals("/v2/search?q=red%20shoes&page=2", record.path)
        assertEquals(listOf(Header("q", "red shoes"), Header("page", "2")), record.queryParameters)
        assertEquals(StatusCategory.Pending, record.category)
    }
}

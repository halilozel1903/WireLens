package io.github.halilozel1903.wirelens.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurlBuilderTest {
    @Test
    fun getWithoutBodyHasNoMethodFlag() {
        assertEquals(
            "curl 'https://api.example.com/v2/products' -H 'Accept: application/json'",
            CurlBuilder.command("GET", "https://api.example.com/v2/products", listOf(Header("Accept", "application/json")), multiline = false),
        )
    }

    @Test
    fun postWithBodyAndSkippedHeaders() {
        val command = CurlBuilder.command(
            "post",
            "https://api.example.com/cart",
            listOf(Header("Content-Type", "application/json"), Header("Content-Length", "12"), Header("Accept-Encoding", "gzip")),
            """{"id":8841}""",
        )
        assertEquals(
            "curl -X POST 'https://api.example.com/cart' \\\n  -H 'Content-Type: application/json' \\\n  --data-raw '{\"id\":8841}'",
            command,
        )
    }

    @Test
    fun quotesSingleQuotes() {
        assertEquals("'it'\\''s'", CurlBuilder.shellQuoted("it's"))
    }

    @Test
    fun headUsesHeadFlag() {
        assertEquals("curl --head 'https://x.io'", CurlBuilder.command("HEAD", "https://x.io", multiline = false))
    }

    @Test
    fun truncatedBodyIsLeftOutWithANote() {
        val record = HttpRecord(
            id = 1, method = "PUT", url = "https://x.io/upload", startedAtMillis = 0,
            requestBody = CapturedBody("{\"a\":", 400_000, isTruncated = true),
        )
        val command = CurlBuilder.command(record)
        assertTrue(command.startsWith("# Request body (391 KB) was cut"))
        assertTrue("--data-raw" !in command)
    }
}

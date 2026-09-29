package io.github.halilozel1903.wirelens.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RedactorTest {
    private val redactor = Redactor()

    @Test
    fun redactsSensitiveHeadersIgnoringCase() {
        val headers = listOf(Header("authorization", "Bearer abc"), Header("Accept", "application/json"), Header("Set-Cookie", "sid=1"))
        assertEquals(
            listOf(Header("authorization", Redactor.Placeholder), Header("Accept", "application/json"), Header("Set-Cookie", Redactor.Placeholder)),
            redactor.redactHeaders(headers),
        )
    }

    @Test
    fun redactsQueryParametersOnly() {
        assertEquals(
            "https://api.example.com/v1/items?page=2&api_key=${Redactor.Placeholder}&q=shoes#top",
            redactor.redactUrl("https://api.example.com/v1/items?page=2&api_key=s3cr3t&q=shoes#top"),
        )
        assertEquals("https://example.com/a", redactor.redactUrl("https://example.com/a"))
    }

    @Test
    fun redactsJsonStringValuesAtAnyDepthAndKeepsFormatting() {
        val json = """{"user":"ada","password" : "hunter2","session":{"refresh_token":"r-1","ttl":3600}}"""
        assertEquals(
            """{"user":"ada","password" : "${Redactor.Placeholder}","session":{"refresh_token":"${Redactor.Placeholder}","ttl":3600}}""",
            redactor.redactJson(json),
        )
    }

    @Test
    fun leavesNonStringValuesAndUnrelatedJsonAlone() {
        val json = """{"token":null,"items":[1,2]}"""
        assertSame(json, redactor.redactJson(json))
    }

    @Test
    fun noneKeepsEverything() {
        val headers = listOf(Header("Authorization", "Bearer abc"))
        assertEquals(headers, Redactor.None.redactHeaders(headers))
        assertEquals("https://x.io/?token=1", Redactor.None.redactUrl("https://x.io/?token=1"))
    }
}

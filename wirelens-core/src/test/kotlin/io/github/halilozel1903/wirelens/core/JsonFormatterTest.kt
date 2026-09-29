package io.github.halilozel1903.wirelens.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonFormatterTest {
    @Test
    fun prettyPrintsKeepingOrderAndNumbers() {
        val json = """{"z":1.50,"a":[true,null,{}],"s":"x, y"}"""
        assertEquals(
            """
            {
              "z": 1.50,
              "a": [
                true,
                null,
                {}
              ],
              "s": "x, y"
            }
            """.trimIndent(),
            JsonFormatter.prettyPrint(json),
        )
    }

    @Test
    fun tokensJoinBackToInput() {
        val json = """{ "k" : "a \"q\"", "n": -1e5 }"""
        assertEquals(json, JsonFormatter.tokenize(json).joinToString("") { it.text })
    }

    @Test
    fun marksKeys() {
        val kinds = JsonFormatter.tokenize("""{"k":"v"}""").map { it.kind }
        assertEquals(
            listOf(JsonToken.Kind.Punctuation, JsonToken.Kind.Key, JsonToken.Kind.Punctuation, JsonToken.Kind.String, JsonToken.Kind.Punctuation),
            kinds,
        )
    }

    @Test
    fun detectsJson() {
        assertTrue(JsonFormatter.looksLikeJson(" [1, 2] "))
        assertFalse(JsonFormatter.looksLikeJson("<html></html>"))
        assertFalse(JsonFormatter.looksLikeJson("{oops}"))
        assertEquals("plain text", JsonFormatter.prettyPrint("plain text"))
    }
}

package io.github.halilozel1903.wirelens.core

/** A piece of JSON text, used for pretty printing and syntax highlighting. */
public data class JsonToken(val kind: Kind, val text: String) {
    public enum class Kind {
        /** A string followed by a colon. */
        Key,
        String,
        Number,

        /** `true`, `false` or `null`. */
        Literal,

        /** `{ } [ ] : ,` */
        Punctuation,
        Whitespace,

        /** Anything that is not valid JSON. */
        Other,
    }
}

/** Pretty prints and tokenizes JSON without reordering keys or rewriting numbers. */
public object JsonFormatter {
    /** Whether [text] looks like a complete JSON object or array. */
    public fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 2) return false
        val pair = trimmed.first() to trimmed.last()
        if (pair != ('{' to '}') && pair != ('[' to ']')) return false
        return tokenize(trimmed).none { it.kind == JsonToken.Kind.Other }
    }

    /** [text] re-indented with [indent], or unchanged when it does not look like JSON. */
    public fun prettyPrint(text: String, indent: String = "  "): String =
        if (looksLikeJson(text)) format(text, indent) else text

    /** Re-indents JSON text. Keys keep their order and values keep their exact spelling. */
    public fun format(text: String, indent: String = "  "): String {
        val tokens = tokenize(text).filter { it.kind != JsonToken.Kind.Whitespace }
        val out = StringBuilder(text.length + text.length / 2)
        var level = 0
        var index = 0

        fun newline() {
            out.append('\n')
            repeat(level.coerceAtLeast(0)) { out.append(indent) }
        }

        while (index < tokens.size) {
            val token = tokens[index]
            if (token.kind == JsonToken.Kind.Punctuation) {
                when (token.text) {
                    "{", "[" -> {
                        out.append(token.text)
                        val closing = if (token.text == "{") "}" else "]"
                        val next = tokens.getOrNull(index + 1)
                        if (next != null && next.kind == JsonToken.Kind.Punctuation && next.text == closing) {
                            out.append(closing)
                            index++
                        } else {
                            level++
                            newline()
                        }
                    }
                    "}", "]" -> {
                        level--
                        newline()
                        out.append(token.text)
                    }
                    "," -> {
                        out.append(',')
                        newline()
                    }
                    ":" -> out.append(": ")
                }
            } else {
                out.append(token.text)
            }
            index++
        }
        return out.toString()
    }

    /**
     * Splits JSON text into tokens. Invalid input still produces tokens (kind [JsonToken.Kind.Other]),
     * so it can be highlighted as far as it goes. Joining the texts gives back the input.
     */
    public fun tokenize(text: String): List<JsonToken> {
        val tokens = ArrayList<JsonToken>()
        var index = 0
        val length = text.length
        while (index < length) {
            val c = text[index]
            when {
                c.isWhitespace() -> {
                    var end = index
                    while (end < length && text[end].isWhitespace()) end++
                    tokens += JsonToken(JsonToken.Kind.Whitespace, text.substring(index, end))
                    index = end
                }
                c in "{}[]:," -> {
                    tokens += JsonToken(JsonToken.Kind.Punctuation, c.toString())
                    index++
                }
                c == '"' -> {
                    var end = index + 1
                    var escaped = false
                    while (end < length) {
                        val next = text[end]
                        if (escaped) escaped = false
                        else if (next == '\\') escaped = true
                        else if (next == '"') break
                        end++
                    }
                    end = minOf(end + 1, length)
                    tokens += JsonToken(JsonToken.Kind.String, text.substring(index, end))
                    index = end
                }
                c == '-' || c in '0'..'9' -> {
                    var end = index + 1
                    while (end < length && (text[end] in '0'..'9' || text[end] in ".eE+-")) end++
                    tokens += JsonToken(JsonToken.Kind.Number, text.substring(index, end))
                    index = end
                }
                c.isLetter() -> {
                    var end = index + 1
                    while (end < length && text[end].isLetter()) end++
                    val word = text.substring(index, end)
                    val kind = if (word == "true" || word == "false" || word == "null") JsonToken.Kind.Literal else JsonToken.Kind.Other
                    tokens += JsonToken(kind, word)
                    index = end
                }
                else -> {
                    tokens += JsonToken(JsonToken.Kind.Other, c.toString())
                    index++
                }
            }
        }
        markKeys(tokens)
        return tokens
    }

    private fun markKeys(tokens: MutableList<JsonToken>) {
        for (i in tokens.indices) {
            if (tokens[i].kind != JsonToken.Kind.String) continue
            var next = i + 1
            while (next < tokens.size && tokens[next].kind == JsonToken.Kind.Whitespace) next++
            val token = tokens.getOrNull(next)
            if (token != null && token.kind == JsonToken.Kind.Punctuation && token.text == ":") {
                tokens[i] = tokens[i].copy(kind = JsonToken.Kind.Key)
            }
        }
    }
}

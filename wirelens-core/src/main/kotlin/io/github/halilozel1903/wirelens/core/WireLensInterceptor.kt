package io.github.halilozel1903.wirelens.core

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.GzipSource
import okio.buffer
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset

/**
 * An OkHttp interceptor that records every call into [store].
 *
 * Add it with `addInterceptor` to see requests as your code builds them and responses already
 * decompressed. The response body is only peeked, never consumed, so your code reads it as usual.
 * Streaming responses (`text/event-stream`) and one-shot or duplex request bodies are recorded
 * without their body so the call is never blocked or replayed.
 */
public class WireLensInterceptor(
    private val store: WireLensStore,
    @Volatile public var options: WireLensOptions = WireLensOptions(),
    private val clock: () -> Long = System::currentTimeMillis,
) : Interceptor {
    /** `false` passes calls through without recording them. */
    @Volatile
    public var isEnabled: Boolean = true

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val options = options
        if (!isEnabled || options.isIgnored(request.url.host)) return chain.proceed(request)

        val redactor = options.redactor
        val started = clock()
        val startNanos = System.nanoTime()
        var record = HttpRecord(
            id = store.newId(),
            method = request.method,
            url = redactor.redactUrl(request.url.toString()),
            requestHeaders = redactor.redactHeaders(requestHeaders(request)),
            requestBody = captureRequestBody(request, options),
            startedAtMillis = started,
        )
        store.put(record)

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            record = record.copy(
                tookMillis = elapsed(startNanos),
                error = e.message?.takeIf { it.isNotBlank() }?.let { "${e.javaClass.simpleName}: $it" } ?: e.javaClass.simpleName,
            )
            store.put(record)
            throw e
        }

        record = record.copy(
            tookMillis = elapsed(startNanos),
            statusCode = response.code,
            statusMessage = response.message,
            protocol = response.protocol.toString(),
            responseHeaders = redactor.redactHeaders(response.headers.toList()),
            responseBody = captureResponseBody(response, options),
        )
        store.put(record)
        return response
    }

    private fun elapsed(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000

    private fun requestHeaders(request: Request): List<Header> {
        val headers = request.headers.toList().toMutableList()
        val body = request.body
        if (body != null) {
            if (headers.none { it.name.equals("Content-Type", true) }) {
                body.contentType()?.let { headers += Header("Content-Type", it.toString()) }
            }
            if (headers.none { it.name.equals("Content-Length", true) }) {
                val length = runCatching { body.contentLength() }.getOrDefault(-1L)
                if (length >= 0) headers += Header("Content-Length", length.toString())
            }
        }
        return headers
    }

    private fun captureRequestBody(request: Request, options: WireLensOptions): CapturedBody? {
        val body = request.body ?: return null
        val contentType = body.contentType()
        val length = runCatching { body.contentLength() }.getOrDefault(-1L)
        if (body.isDuplex() || body.isOneShot()) {
            return CapturedBody(null, length, isTruncated = true, contentType = contentType?.toString())
        }
        val buffer = Buffer()
        runCatching { body.writeTo(buffer) }.onFailure { return CapturedBody(null, length, true, contentType?.toString()) }
        val size = buffer.size
        val gzip = request.header("Content-Encoding").equals("gzip", ignoreCase = true)
        val source = if (gzip) gunzip(buffer, options.maxBodyBytes + 1) else buffer
        return toCapturedBody(source, size, contentType, options, cut = source.size > options.maxBodyBytes)
    }

    private fun captureResponseBody(response: Response, options: WireLensOptions): CapturedBody? {
        val body = response.body
        val contentType = body.contentType()
        val declared = body.contentLength()
        if (response.request.method == "HEAD" || response.code == 204 || response.code == 304 || declared == 0L) {
            return if (declared > 0) CapturedBody(null, declared, true, contentType?.toString()) else null
        }
        if (contentType?.subtype == "event-stream") {
            return CapturedBody(null, declared, isTruncated = true, contentType = contentType.toString())
        }
        val peeked = runCatching { response.peekBody(options.maxBodyBytes + 1) }.getOrNull()
            ?: return CapturedBody(null, declared, true, contentType?.toString())
        val buffer = Buffer().apply { write(peeked.bytes()) }
        val gzip = response.header("Content-Encoding").equals("gzip", ignoreCase = true)
        val size = when {
            declared >= 0 -> declared
            buffer.size <= options.maxBodyBytes -> buffer.size
            else -> -1L
        }
        val rawCut = buffer.size > options.maxBodyBytes
        val source = if (gzip) gunzip(buffer, options.maxBodyBytes + 1) else buffer
        return toCapturedBody(source, size, contentType, options, cut = rawCut || source.size > options.maxBodyBytes)
    }

    private fun toCapturedBody(buffer: Buffer, size: Long, contentType: MediaType?, options: WireLensOptions, cut: Boolean): CapturedBody {
        val truncated = cut
        val bytes = buffer.readByteArray(minOf(buffer.size, options.maxBodyBytes))
        val type = contentType?.toString()
        if (bytes.isEmpty()) return CapturedBody("", size.coerceAtLeast(0), false, type)
        if (!isProbablyText(bytes, contentType)) return CapturedBody(null, size, truncated, type)
        val charset = contentType?.charset() ?: Charsets.UTF_8
        var text = String(bytes, charset)
        if (truncated) text = text.trimEnd('�')
        val probe = CapturedBody(text, size, truncated, type)
        if (probe.isJson && !truncated) text = options.redactor.redactJson(text)
        return probe.copy(text = text)
    }

    private fun gunzip(buffer: Buffer, limit: Long): Buffer {
        val out = Buffer()
        val source = GzipSource(buffer).buffer()
        try {
            while (out.size < limit) {
                if (source.read(out, minOf(8192L, limit - out.size)) == -1L) break
            }
        } catch (_: EOFException) {
            // A cut gzip stream still yields everything before the cut.
        } catch (_: IOException) {
        }
        return out
    }

    private fun isProbablyText(bytes: ByteArray, contentType: MediaType?): Boolean {
        val type = contentType?.type?.lowercase()
        val subtype = contentType?.subtype?.lowercase().orEmpty()
        if (type == "image" || type == "audio" || type == "video" || type == "font") return false
        if (subtype == "octet-stream" || subtype == "zip" || subtype == "pdf" || subtype.contains("protobuf")) return false
        if (contentType?.charset() != null || type == "text") return true
        // Decode a prefix and reject control characters, like OkHttp's logging interceptor.
        val sample = String(bytes, 0, minOf(bytes.size, 256), Charset.forName("UTF-8"))
        var checked = 0
        for (c in sample) {
            if (checked++ >= 64) break
            if (c == '�' && checked < sample.length - 3) return false
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) return false
        }
        return true
    }

    private fun Headers.toList(): List<Header> = map { (name, value) -> Header(name, value) }
}

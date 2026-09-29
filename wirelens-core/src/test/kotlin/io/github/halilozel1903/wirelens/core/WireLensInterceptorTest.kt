package io.github.halilozel1903.wirelens.core

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.GzipSink
import okio.buffer
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WireLensInterceptorTest {
    private val server = MockWebServer()
    private val store = WireLensStore()
    private val interceptor = WireLensInterceptor(store)
    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    @BeforeTest
    fun start() = server.start()

    @AfterTest
    fun stop() = server.close()

    @Test
    fun recordsRequestAndResponseAndLeavesBodyReadable() {
        server.enqueue(
            MockResponse.Builder().code(201).addHeader("Content-Type", "application/json").addHeader("Set-Cookie", "sid=42")
                .body("""{"id":8841,"access_token":"abc"}""").build(),
        )
        val request = Request.Builder()
            .url(server.url("/v2/cart?api_key=s3cr3t&page=1"))
            .header("Authorization", "Bearer secret")
            .post("""{"productId":8841,"password":"pw"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val body = client.newCall(request).execute().use { it.body.string() }
        assertEquals("""{"id":8841,"access_token":"abc"}""", body)

        val record = store.records.value.single()
        assertEquals("POST", record.method)
        assertTrue(record.url.endsWith("/v2/cart?api_key=${Redactor.Placeholder}&page=1"))
        assertEquals(Redactor.Placeholder, HttpRecord.header("authorization", record.requestHeaders))
        assertEquals("application/json; charset=utf-8", HttpRecord.header("Content-Type", record.requestHeaders))
        assertEquals("""{"productId":8841,"password":"${Redactor.Placeholder}"}""", record.requestBody?.text)
        assertEquals(201, record.statusCode)
        assertEquals(Redactor.Placeholder, HttpRecord.header("Set-Cookie", record.responseHeaders))
        assertEquals("""{"id":8841,"access_token":"${Redactor.Placeholder}"}""", record.responseBody?.text)
        assertEquals(StatusCategory.Success, record.category)
        assertNotNull(record.tookMillis)

        // The server still received the real secrets.
        val received = server.takeRequest()
        assertEquals("Bearer secret", received.headers["Authorization"])
    }

    @Test
    fun cutsLargeBodiesButKeepsTheirSize() {
        interceptor.options = WireLensOptions(maxBodyBytes = 10)
        server.enqueue(MockResponse.Builder().addHeader("Content-Type", "text/plain").body("0123456789ABCDEF").build())
        val text = client.newCall(Request.Builder().url(server.url("/big")).build()).execute().use { it.body.string() }
        assertEquals("0123456789ABCDEF", text)

        val body = store.records.value.single().responseBody!!
        assertEquals("0123456789", body.text)
        assertEquals(16, body.byteCount)
        assertTrue(body.isTruncated)
    }

    @Test
    fun decodesGzipWhenTheAppAskedForIt() {
        val gzipped = Buffer().also { sink -> GzipSink(sink).buffer().use { it.writeUtf8("""{"ok":true}""") } }
        server.enqueue(
            MockResponse.Builder().addHeader("Content-Type", "application/json").addHeader("Content-Encoding", "gzip").body(gzipped).build(),
        )
        client.newCall(Request.Builder().url(server.url("/gz")).header("Accept-Encoding", "gzip").build()).execute().close()
        assertEquals("""{"ok":true}""", store.records.value.single().responseBody?.text)
    }

    @Test
    fun binaryBodiesHaveNoText() {
        server.enqueue(MockResponse.Builder().addHeader("Content-Type", "image/png").body(Buffer().write(byteArrayOf(-119, 80, 78, 71, 0, 1))).build())
        client.newCall(Request.Builder().url(server.url("/img.png")).build()).execute().close()
        val body = store.records.value.single().responseBody!!
        assertNull(body.text)
        assertTrue(body.isBinary)
        assertEquals(6, body.byteCount)
    }

    @Test
    fun recordsFailures() {
        val failing = client.newBuilder().readTimeout(200, TimeUnit.MILLISECONDS).build()
        server.enqueue(MockResponse.Builder().headersDelay(2, TimeUnit.SECONDS).body("late").build())
        assertFailsWith<IOException> { failing.newCall(Request.Builder().url(server.url("/slow")).build()).execute() }
        val record = store.records.value.single()
        assertEquals(StatusCategory.Failed, record.category)
        assertTrue(record.error!!.contains("SocketTimeoutException"))
    }

    @Test
    fun ignoredHostsAndDisabledAreNotRecorded() {
        interceptor.options = WireLensOptions(ignoredHosts = setOf(server.hostName))
        server.enqueue(MockResponse.Builder().body("a").build())
        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()
        interceptor.options = WireLensOptions()
        interceptor.isEnabled = false
        server.enqueue(MockResponse.Builder().body("b").build())
        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()
        assertEquals(0, store.records.value.size)
    }
}

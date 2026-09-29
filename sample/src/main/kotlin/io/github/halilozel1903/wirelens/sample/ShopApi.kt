package io.github.halilozel1903.wirelens.sample

import io.github.halilozel1903.wirelens.WireLens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Real calls to public test APIs, all through an OkHttpClient with the WireLens interceptor. */
object ShopApi {
    private val client = OkHttpClient.Builder()
        .addInterceptor(WireLens.interceptor)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    suspend fun products() = get("https://dummyjson.com/products?limit=5&select=title,price,rating")

    suspend fun product() = get("https://dummyjson.com/products/1")

    /** dummyjson returns an access and a refresh token; WireLens hides both, and the password. */
    suspend fun signIn() = post("https://dummyjson.com/auth/login", """{"username":"emilys","password":"emilyspass","expiresInMins":30}""")

    suspend fun addToCart() = post("https://dummyjson.com/carts/add", """{"userId":1,"products":[{"id":144,"quantity":2}]}""")

    suspend fun notFound() = get("https://httpbin.org/status/404")

    suspend fun serverError() = get("https://httpbin.org/status/500")

    suspend fun slow() = get("https://httpbin.org/delay/2")

    suspend fun image() = get("https://picsum.photos/id/180/400/300")

    private suspend fun get(url: String) = execute(
        Request.Builder().url(url).header("Accept", "application/json").header("Authorization", "Bearer demo-token-123").build(),
    )

    private suspend fun post(url: String, body: String) = execute(
        Request.Builder().url(url).post(body.toRequestBody(json)).build(),
    )

    private suspend fun execute(request: Request): Result<Int> = withContext(Dispatchers.IO) {
        runCatching { client.newCall(request).execute().use { response -> response.body.bytes(); response.code } }
    }
}

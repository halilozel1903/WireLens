package io.github.halilozel1903.wirelens.core

/**
 * Realistic example records for previews, screenshots and trying the inspector without a network.
 * Timestamps are counted back from [nowMillis].
 */
public object SampleTraffic {
    private const val API = "https://api.novashop.dev"

    public fun records(nowMillis: Long = System.currentTimeMillis()): List<HttpRecord> {
        var id = 0L
        fun at(secondsAgo: Int) = nowMillis - secondsAgo * 1000L
        val json = "application/json; charset=utf-8"
        val common = listOf(Header("Accept", "application/json"), Header("User-Agent", "NovaShop/3.2 (Android 16) okhttp/5.5.0"))
        val auth = common + Header("Authorization", Redactor.Placeholder)
        val server = listOf(Header("Content-Type", json), Header("Server", "cloudflare"), Header("Cache-Control", "private, max-age=60"))

        val product = """{"id":8841,"title":"Aurora Wireless Headphones","brand":"Nova","price":129.90,"currency":"EUR","rating":4.7,"inStock":true,"colors":["Midnight","Pearl","Sage"],"shipping":{"freeOver":50,"days":2},"discount":null}"""
        val products = """{"page":1,"pageSize":20,"total":184,"items":[{"id":8841,"title":"Aurora Wireless Headphones","price":129.90},{"id":8842,"title":"Pulse Smart Watch","price":199.00},{"id":8850,"title":"Echo Mini Speaker","price":49.50}]}"""
        val login = """{"email":"ada@novashop.dev","password":"${Redactor.Placeholder}","device":"Pixel 9"}"""
        val session = """{"userId":"u_2931","access_token":"${Redactor.Placeholder}","refresh_token":"${Redactor.Placeholder}","expiresIn":3600}"""
        val cartAdd = """{"productId":8841,"quantity":1,"color":"Midnight"}"""
        val cart = """{"items":1,"subtotal":129.90,"shipping":0,"total":129.90}"""
        val error500 = """{"error":"checkout_unavailable","message":"Payment provider did not respond","traceId":"7f3a-91c2"}"""
        val error404 = """{"error":"not_found","message":"Coupon SPRING24 does not exist"}"""

        fun body(text: String, type: String = json) = CapturedBody.text(text, type)

        return listOf(
            HttpRecord(++id, "POST", "$API/v1/auth/login", common + Header("Content-Type", json), body(login),
                at(95), 342, 200, "OK", "h2", server + Header("Set-Cookie", Redactor.Placeholder), body(session)),
            HttpRecord(++id, "GET", "$API/v2/products?page=1&sort=popular", auth, null,
                at(88), 118, 200, "OK", "h2", server, body(products)),
            HttpRecord(++id, "GET", "https://cdn.novashop.dev/img/products/8841@2x.webp", common, null,
                at(87), 64, 200, "OK", "h2", listOf(Header("Content-Type", "image/webp"), Header("Content-Length", "48213")),
                CapturedBody(null, 48_213, contentType = "image/webp")),
            HttpRecord(++id, "GET", "$API/v2/products/8841", auth, null,
                at(70), 84, 200, "OK", "h2", server + Header("ETag", "\"a41f-8841\""), body(product)),
            HttpRecord(++id, "POST", "$API/v2/cart/items", auth + Header("Content-Type", json), body(cartAdd),
                at(52), 156, 201, "Created", "h2", server, body(cart)),
            HttpRecord(++id, "GET", "$API/v2/recommendations?for=8841&api_key=${Redactor.Placeholder}", auth, null,
                at(49), 31, 304, "Not Modified", "h2", listOf(Header("ETag", "\"r-19\"")), null),
            HttpRecord(++id, "PATCH", "$API/v2/me/preferences", auth + Header("Content-Type", json), body("""{"newsletter":false,"theme":"dark"}"""),
                at(40), 97, 200, "OK", "h2", server, body("""{"ok":true}""")),
            HttpRecord(++id, "DELETE", "$API/v2/cart/coupons/SPRING24", auth, null,
                at(31), 73, 404, "Not Found", "h2", server, body(error404)),
            HttpRecord(++id, "POST", "$API/v2/checkout", auth + Header("Content-Type", json), body("""{"cartId":"c_5521","payment":"card"}"""),
                at(18), 1840, 500, "Internal Server Error", "h2", server, body(error500)),
            HttpRecord(++id, "GET", "https://analytics.novashop.dev/v1/collect?e=checkout_error", common, null,
                at(12), 10_000, error = "SocketTimeoutException: timeout"),
            HttpRecord(++id, "GET", "$API/v2/orders?status=open", auth, null, at(1)),
        ).reversed()
    }
}

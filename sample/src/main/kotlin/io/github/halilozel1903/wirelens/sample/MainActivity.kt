package io.github.halilozel1903.wirelens.sample

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.halilozel1903.wirelens.WireLens
import io.github.halilozel1903.wirelens.WireLensInspector
import io.github.halilozel1903.wirelens.WireLensOverlay
import io.github.halilozel1903.wirelens.core.SampleTraffic
import io.github.halilozel1903.wirelens.ui.WireLensTab
import io.github.halilozel1903.wirelens.ui.WireLensTheme

/**
 * The demo shop. For README screenshots, `--es scene list|detail|overview|home` fills the
 * inspector with fixed sample traffic and opens that screen, since adb can't tap reliably.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val scene = intent.getStringExtra("scene")
        if (scene != null && savedInstanceState == null) {
            WireLens.clear()
            SampleTraffic.records(nowMillis = SCREENSHOT_TIME).reversed().forEach(WireLens.store::put)
        }
        setContent {
            WireLensTheme {
                when (scene) {
                    "list" -> WireLensInspector()
                    "detail" -> WireLensInspector(initialRecordId = PRODUCT_ID, initialTab = WireLensTab.Response)
                    "overview" -> WireLensInspector(initialRecordId = LOGIN_ID)
                    else -> WireLensOverlay { ShopScreen() }
                }
            }
        }
    }

    private companion object {
        // 29 Sep 2026, 09:41 UTC
        const val SCREENSHOT_TIME = 1_790_674_860_000L
        const val LOGIN_ID = 1L
        const val PRODUCT_ID = 4L
    }
}

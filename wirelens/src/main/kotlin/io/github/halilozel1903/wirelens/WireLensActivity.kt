package io.github.halilozel1903.wirelens

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.halilozel1903.wirelens.ui.WireLensTheme

/** Full screen inspector, opened by [WireLens.open] or a shake. */
public class WireLensActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The headers are always a dark gradient, so the status bar icons stay light.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            WireLensTheme {
                WireLensInspector(onClose = ::finish)
            }
        }
    }
}

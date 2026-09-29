package io.github.halilozel1903.wirelens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.wirelens.core.WireLensStore
import io.github.halilozel1903.wirelens.ui.WireLensBubble

/**
 * Shows a draggable [WireLensBubble] above [content] that opens the inspector.
 *
 * ```kotlin
 * setContent {
 *     WireLensOverlay(enabled = BuildConfig.DEBUG) { App() }
 * }
 * ```
 */
@Composable
public fun WireLensOverlay(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    store: WireLensStore = WireLens.store,
    alignment: Alignment = Alignment.BottomEnd,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Box(modifier.fillMaxSize()) {
        content()
        if (enabled) {
            WireLensBubble(
                store = store,
                onClick = { WireLens.open(context) },
                modifier = Modifier
                    .align(alignment)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
            )
        }
    }
}

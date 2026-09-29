package io.github.halilozel1903.wirelens.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.wirelens.core.RecordFilter
import io.github.halilozel1903.wirelens.core.WireLensStore
import kotlinx.coroutines.delay

/**
 * The whole inspector: the request list and, when [selectedId] is set, the detail of that request.
 * Stateless about navigation so it can live in an Activity, a dialog, a tab or a debug drawer.
 *
 * @param onSelect Called with a record id to open, or `null` to go back to the list.
 * @param onCopy Puts text on the clipboard. The screen shows a short confirmation after it.
 * @param onShare Shares text, for example with an `ACTION_SEND` intent. `null` hides the share button.
 * @param onClose Shows a close button in the list header when set.
 */
@Composable
public fun WireLensScreen(
    store: WireLensStore,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    tab: WireLensTab = WireLensTab.Overview,
    onTabChange: (WireLensTab) -> Unit = {},
    onCopy: (label: String, text: String) -> Unit = { _, _ -> },
    onShare: ((text: String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val records by store.records.collectAsState()
    var filter by remember { mutableStateOf(RecordFilter()) }
    var toast by remember { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { id -> records.firstOrNull { it.id == id } }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1600)
            toast = null
        }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = selected,
            contentKey = { it?.id },
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(220))) togetherWith fadeOut(tween(160))
                } else {
                    fadeIn(tween(220)) togetherWith (slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200)))
                }
            },
            label = "screen",
        ) { record ->
            if (record == null) {
                RequestListScreen(
                    records = records,
                    filter = filter,
                    onFilterChange = { filter = it },
                    onOpen = { onSelect(it.id) },
                    onClear = { store.clear() },
                    onClose = onClose,
                )
            } else {
                RequestDetailScreen(
                    record = record,
                    tab = tab,
                    onTabChange = onTabChange,
                    onBack = { onSelect(null) },
                    onCopy = { label, text ->
                        onCopy(label, text)
                        toast = "$label copied"
                    },
                    onShare = onShare,
                )
            }
        }
        val message = toast
        if (message != null) {
            Text(
                message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1B2E))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

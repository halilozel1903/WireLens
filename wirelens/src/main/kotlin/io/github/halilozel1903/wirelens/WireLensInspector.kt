package io.github.halilozel1903.wirelens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.halilozel1903.wirelens.core.WireLensStore
import io.github.halilozel1903.wirelens.ui.WireLensScreen
import io.github.halilozel1903.wirelens.ui.WireLensTab

/**
 * [WireLensScreen] with its navigation, the system back gesture, the clipboard and the share
 * sheet wired up. Drop it into any screen, tab or dialog of a debug build.
 *
 * @param initialRecordId Opens straight into this request, for example from a notification.
 * @param onClose Shows a close button in the list header when set.
 */
@Composable
public fun WireLensInspector(
    modifier: Modifier = Modifier,
    store: WireLensStore = WireLens.store,
    initialRecordId: Long? = null,
    initialTab: WireLensTab = WireLensTab.Overview,
    onClose: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf(initialRecordId) }
    var tab by rememberSaveable { mutableStateOf(initialTab) }

    BackHandler(enabled = selectedId != null) { selectedId = null }

    WireLensScreen(
        store = store,
        selectedId = selectedId,
        onSelect = {
            if (it != selectedId) tab = WireLensTab.Overview
            selectedId = it
        },
        modifier = modifier,
        tab = tab,
        onTabChange = { tab = it },
        onCopy = { label, text -> context.copyToClipboard(label, text) },
        onShare = { text -> context.shareText(text) },
        onClose = onClose,
    )
}

private fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, "Share request").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

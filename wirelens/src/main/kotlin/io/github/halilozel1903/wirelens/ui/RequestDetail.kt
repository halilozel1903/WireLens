package io.github.halilozel1903.wirelens.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.wirelens.core.CapturedBody
import io.github.halilozel1903.wirelens.core.CurlBuilder
import io.github.halilozel1903.wirelens.core.HttpRecord
import io.github.halilozel1903.wirelens.core.Redactor
import io.github.halilozel1903.wirelens.core.StatusCategory
import io.github.halilozel1903.wirelens.core.WireFormat

/** The tabs of the request detail screen. */
public enum class WireLensTab(internal val title: String) {
    Overview("Overview"),
    Request("Request"),
    Response("Response"),
}

@Composable
internal fun RequestDetailScreen(
    record: HttpRecord,
    tab: WireLensTab,
    onTabChange: (WireLensTab) -> Unit,
    onBack: () -> Unit,
    onCopy: (label: String, text: String) -> Unit,
    onShare: ((text: String) -> Unit)?,
) {
    val curl = CurlBuilder.command(record)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        DetailHeader(record, onBack, onCopyCurl = { onCopy("cURL", curl) }, onShareCurl = onShare?.let { share -> { share(curl) } })
        TabSwitcher(tab, onTabChange, Modifier.padding(horizontal = 16.dp).offset(y = (-24).dp))
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
            modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-8).dp),
        ) { current ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (current) {
                    WireLensTab.Overview -> OverviewTab(record, curl, onCopy)
                    WireLensTab.Request -> RequestTab(record, onCopy)
                    WireLensTab.Response -> ResponseTab(record, onCopy)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun DetailHeader(record: HttpRecord, onBack: () -> Unit, onCopyCurl: () -> Unit, onShareCurl: (() -> Unit)?) {
    val colors = WireLensTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(colors.header),
    ) {
        Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 46.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(WireIcons.Back, "Back", onBack)
                Spacer(Modifier.weight(1f))
                RoundIconButton(WireIcons.Copy, "Copy as cURL", onCopyCurl)
                if (onShareCurl != null) {
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(WireIcons.Share, "Share as cURL", onShareCurl)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MethodPill(record.method, onGradient = true)
                Spacer(Modifier.width(10.dp))
                val code = record.statusCode
                Text(
                    when {
                        code != null -> WireFormat.status(code)
                        record.category == StatusCategory.Pending -> "In progress"
                        else -> "Failed"
                    },
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                record.url,
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                record.tookMillis?.let { GlassChip(WireFormat.duration(it)) }
                record.responseBody?.let { GlassChip(WireFormat.bytes(it.byteCount)) }
                record.protocol?.let { GlassChip(it.uppercase()) }
                GlassChip(WireFormat.time(record.startedAtMillis))
            }
        }
    }
}

@Composable
private fun GlassChip(text: String) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 5.dp),
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun TabSwitcher(selected: WireLensTab, onSelect: (WireLensTab) -> Unit, modifier: Modifier = Modifier) {
    val tabs = WireLensTab.entries
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(5.dp),
    ) {
        val width = maxWidth / tabs.size
        val offset by animateDpAsState(width * selected.ordinal, spring(dampingRatio = 0.8f, stiffness = 500f), label = "indicator")
        Box(
            Modifier
                .offset(x = offset)
                .width(width)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(WireLensTheme.colors.header),
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).clickable { onSelect(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.title,
                        color = if (tab == selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(record: HttpRecord, curl: String, onCopy: (String, String) -> Unit) {
    val colors = WireLensTheme.colors
    val hidden = redactedNames(record)
    if (hidden.isNotEmpty()) {
        SectionCard("Privacy") {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(colors.clientError.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(WireIcons.Lock, null, Modifier.size(18.dp), tint = colors.clientError)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "${hidden.size} ${if (hidden.size == 1) "value was" else "values were"} hidden before storing",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        hidden.joinToString(", "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
    SectionCard("cURL", action = { CopyAction { onCopy("cURL", curl) } }) {
        CodeBlock(curl, json = false, wrap = true)
    }
    SectionCard("Summary") {
        KeyValueRow("Host", record.host, mono = true)
        KeyValueRow("Path", record.path, mono = true)
        KeyValueRow("Method", record.method, valueColor = colors.method(record.method))
        KeyValueRow(
            "Status",
            record.statusCode?.let(WireFormat::status) ?: record.error ?: "Waiting for response",
            valueColor = colors.status(record.category),
        )
        record.protocol?.let { KeyValueRow("Protocol", it) }
        KeyValueRow("Started", WireFormat.time(record.startedAtMillis))
        record.tookMillis?.let { KeyValueRow("Duration", WireFormat.duration(it)) }
        KeyValueRow("Request size", record.requestBody?.byteCount?.let(WireFormat::bytes) ?: "0 B")
        KeyValueRow("Response size", record.responseBody?.byteCount?.let(WireFormat::bytes) ?: "0 B")
        HttpRecord.header("Content-Type", record.responseHeaders)?.let { KeyValueRow("Content type", it, mono = true) }
    }
}

@Composable
private fun RequestTab(record: HttpRecord, onCopy: (String, String) -> Unit) {
    val query = record.queryParameters
    if (query.isNotEmpty()) {
        SectionCard("Query parameters") { HeaderList(query) }
    }
    SectionCard("Headers") { HeaderList(record.requestHeaders) }
    BodyCard("Body", record.requestBody, onCopy)
}

@Composable
private fun ResponseTab(record: HttpRecord, onCopy: (String, String) -> Unit) {
    val error = record.error
    if (error != null) {
        SectionCard("Error") {
            Text(error, color = WireLensTheme.colors.failed, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
    if (record.category == StatusCategory.Pending) {
        SectionCard("Response") {
            Text("Waiting for the response…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        return
    }
    if (record.statusCode != null) {
        BodyCard("Body", record.responseBody, onCopy)
        SectionCard("Headers") { HeaderList(record.responseHeaders) }
    }
}

@Composable
private fun BodyCard(title: String, body: CapturedBody?, onCopy: (String, String) -> Unit) {
    if (body == null) {
        SectionCard(title) { Text("No body", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        return
    }
    val text = body.text
    SectionCard(
        title,
        action = if (!text.isNullOrEmpty()) ({ CopyAction { onCopy(title, text) } }) else null,
    ) {
        when {
            text != null && text.isEmpty() ->
                Text("No body", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            text == null ->
                Text(
                    listOfNotNull("Binary body", WireFormat.bytes(body.byteCount).takeIf { body.byteCount >= 0 }, body.contentType).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            else -> {
                if (body.isTruncated) {
                    Text(
                        "Showing the start of a ${WireFormat.bytes(body.byteCount)} body",
                        color = WireLensTheme.colors.clientError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                CodeBlock(text.orEmpty(), json = body.isJson && !body.isTruncated)
            }
        }
    }
}

@Composable
private fun CopyAction(onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(WireIcons.Copy, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text("Copy", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Names of the headers, query parameters and JSON keys whose values were redacted. */
internal fun redactedNames(record: HttpRecord): List<String> {
    val names = LinkedHashSet<String>()
    (record.requestHeaders + record.responseHeaders).filter { it.value == Redactor.Placeholder }.forEach { names += it.name }
    record.queryParameters.filter { it.value == Redactor.Placeholder }.forEach { names += it.name }
    val keyPattern = Regex("\"([^\"]+)\"\\s*:\\s*\"" + Regex.escape(Redactor.Placeholder) + "\"")
    listOfNotNull(record.requestBody?.text, record.responseBody?.text).forEach { text ->
        keyPattern.findAll(text).forEach { names += it.groupValues[1] }
    }
    return names.toList()
}

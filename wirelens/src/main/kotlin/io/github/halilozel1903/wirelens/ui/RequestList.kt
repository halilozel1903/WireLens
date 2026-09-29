package io.github.halilozel1903.wirelens.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.wirelens.core.HttpRecord
import io.github.halilozel1903.wirelens.core.RecordFilter
import io.github.halilozel1903.wirelens.core.StatusCategory
import io.github.halilozel1903.wirelens.core.TrafficSummary
import io.github.halilozel1903.wirelens.core.WireFormat

@Composable
internal fun RequestListScreen(
    records: List<HttpRecord>,
    filter: RecordFilter,
    onFilterChange: (RecordFilter) -> Unit,
    onOpen: (HttpRecord) -> Unit,
    onClear: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val visible = filter.apply(records)
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            ListHeader(TrafficSummary.of(records), onClear, onClose)
        }
        item(key = "search") {
            SearchField(
                query = filter.query,
                onQueryChange = { onFilterChange(filter.copy(query = it)) },
                modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-26).dp),
            )
        }
        item(key = "filters") {
            FilterRow(records, filter, onFilterChange, Modifier.offset(y = (-12).dp))
        }
        if (visible.isEmpty()) {
            item(key = "empty") { EmptyState(hasRecords = records.isNotEmpty()) }
        }
        items(visible, key = { it.id }) { record ->
            RequestRow(
                record,
                onClick = { onOpen(record) },
                modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 5.dp),
            )
        }
        item(key = "insets") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun ListHeader(summary: TrafficSummary, onClear: () -> Unit, onClose: (() -> Unit)?) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(WireLensTheme.colors.header),
    ) {
        HeaderDecoration(Modifier.matchParentSize())
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(WireIcons.Traffic, null, Modifier.size(24.dp), tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("WireLens", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Network inspector", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                }
                RoundIconButton(WireIcons.Delete, "Clear requests", onClear)
                if (onClose != null) {
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(WireIcons.Close, "Close", onClose)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Requests", summary.requests.toString(), Modifier.weight(1f))
                StatTile("Errors", summary.errors.toString(), Modifier.weight(1f), highlight = summary.errors > 0)
                StatTile("Received", WireFormat.bytes(summary.receivedBytes), Modifier.weight(1f))
                StatTile("Avg time", summary.averageMillis?.let(WireFormat::duration) ?: "–", Modifier.weight(1f))
            }
        }
    }
}

/** Soft rings in the header, like ripples of traffic. */
@Composable
private fun HeaderDecoration(modifier: Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width * 0.92f, size.height * 0.12f)
        for (i in 1..4) {
            drawCircle(Color.White.copy(alpha = 0.10f - i * 0.018f), radius = size.minDimension * 0.22f * i, center = center, style = Stroke(width = 1.5.dp.toPx()))
        }
        drawCircle(Color.White.copy(alpha = 0.06f), radius = size.minDimension * 0.5f, center = Offset(size.width * 0.05f, size.height * 1.05f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (highlight) 0.26f else 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x405B3DF5), spotColor = Color(0x405B3DF5))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(WireIcons.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search URL, method, status…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                WireIcons.Close,
                "Clear search",
                Modifier.size(18.dp).clip(CircleShape).clickable { onQueryChange("") },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterRow(records: List<HttpRecord>, filter: RecordFilter, onFilterChange: (RecordFilter) -> Unit, modifier: Modifier = Modifier) {
    val colors = WireLensTheme.colors
    val categories = listOf(StatusCategory.Success, StatusCategory.Redirect, StatusCategory.ClientError, StatusCategory.ServerError, StatusCategory.Failed)
    val methods = RecordFilter.methodsIn(records)
    LazyRow(
        modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { "c-" + it.name }) { category ->
            FilterChipPill(category.label, colors.status(category), category in filter.categories) { onFilterChange(filter.toggle(category)) }
        }
        items(methods, key = { "m-$it" }) { method ->
            FilterChipPill(method, colors.method(method), method in filter.methods, mono = true) { onFilterChange(filter.toggle(method)) }
        }
    }
}

@Composable
private fun FilterChipPill(text: String, color: Color, selected: Boolean, mono: Boolean = false, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) color else MaterialTheme.colorScheme.surface, label = "chip")
    val content by animateColorAsState(if (selected) Color.White else MaterialTheme.colorScheme.onSurface, label = "chipText")
    Row(
        Modifier
            .clip(CircleShape)
            .background(background)
            .border(BorderStroke(1.dp, if (selected) color else MaterialTheme.colorScheme.outline), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!selected) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = content,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

@Composable
internal fun RequestRow(record: HttpRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WireLensTheme.colors
    val statusColor = colors.status(record.category)
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(if (colors.isDark) 0.dp else 4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1A5B3DF5), spotColor = Color(0x1A5B3DF5))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(statusColor))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MethodPill(record.method)
                Spacer(Modifier.width(8.dp))
                Text(
                    record.path,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                if (record.category == StatusCategory.Pending) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary, strokeCap = StrokeCap.Round)
                } else {
                    StatusBadge(record.statusCode?.toString() ?: "ERR", statusColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.host,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        WireFormat.time(record.startedAtMillis),
                        record.tookMillis?.let(WireFormat::duration),
                        record.responseBody?.byteCount?.let(WireFormat::bytes),
                    ).joinToString("  ·  "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            val error = record.error
            if (error != null) {
                Spacer(Modifier.height(4.dp))
                Text(error, color = colors.failed, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyState(hasRecords: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(WireIcons.Traffic, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasRecords) "No matching requests" else "No requests yet",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasRecords) "Try another search or clear the filters." else "Calls made through an OkHttpClient with WireLens appear here as they happen.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

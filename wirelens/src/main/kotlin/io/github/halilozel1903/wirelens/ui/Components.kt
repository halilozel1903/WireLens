package io.github.halilozel1903.wirelens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.wirelens.core.Header
import io.github.halilozel1903.wirelens.core.JsonFormatter
import io.github.halilozel1903.wirelens.core.JsonToken
import io.github.halilozel1903.wirelens.core.Redactor

/** A colored method label: `GET`, `POST`, ... */
@Composable
internal fun MethodPill(method: String, modifier: Modifier = Modifier, onGradient: Boolean = false) {
    val color = WireLensTheme.colors.method(method)
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (onGradient) Color.White else color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            method.uppercase(),
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

/** A rounded pill with a colored dot and text. */
@Composable
internal fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

/** A white card with a small uppercase title. */
@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(if (WireLensTheme.colors.isDark) 0.dp else 6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x225B3DF5), spotColor = Color(0x225B3DF5))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title.uppercase(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            action?.invoke()
        }
        Spacer(Modifier.padding(top = 10.dp))
        content()
    }
}

@Composable
internal fun KeyValueRow(name: String, value: String, mono: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            name,
            modifier = Modifier.width(112.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
            fontSize = 13.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Header rows. Redacted values get a lock. */
@Composable
internal fun HeaderList(headers: List<Header>, emptyText: String = "No headers") {
    if (headers.isEmpty()) {
        Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        headers.forEach { header ->
            val redacted = header.value == Redactor.Placeholder
            Column {
                Text(
                    header.name,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (redacted) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(WireIcons.Lock, null, Modifier.size(13.dp), tint = WireLensTheme.colors.clientError)
                        Spacer(Modifier.width(4.dp))
                        Text("redacted", color = WireLensTheme.colors.clientError, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Text(header.value, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}

/** A small circular icon button. */
@Composable
internal fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    background: Color = Color.White.copy(alpha = 0.18f),
) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(background).clickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint)
    }
}

/** Monospaced code with line numbers on a dark panel. JSON gets syntax colors. */
@Composable
internal fun CodeBlock(text: String, json: Boolean, modifier: Modifier = Modifier, wrap: Boolean = false, maxLines: Int = 1500) {
    val colors = WireLensTheme.colors
    val shown = remember(text, json) {
        val formatted = if (json) JsonFormatter.prettyPrint(text) else text
        val lines = formatted.lines()
        val clipped = if (lines.size > maxLines) lines.take(maxLines).joinToString("\n") + "\n…" else formatted
        clipped to minOf(lines.size, maxLines)
    }
    val highlighted = remember(shown.first, json, colors) { if (json) highlightJson(shown.first, colors) else AnnotatedString(shown.first) }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.codeBackground)
            .then(if (colors.isDark) Modifier.border(1.dp, Color(0xFF2A2738), RoundedCornerShape(14.dp)) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        if (!wrap) {
            Text(
                (1..shown.second).joinToString("\n"),
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                color = colors.codeGutter,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
        Box(
            Modifier
                .weight(1f)
                .then(if (wrap) Modifier.padding(start = 14.dp) else Modifier.horizontalScroll(rememberScrollState()))
                .padding(end = 14.dp),
        ) {
            Text(
                highlighted,
                color = colors.codeText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                softWrap = wrap,
            )
        }
    }
}

internal fun highlightJson(text: String, colors: WireLensColors): AnnotatedString = buildAnnotatedString {
    for (token in JsonFormatter.tokenize(text)) {
        val color = when (token.kind) {
            JsonToken.Kind.Key -> colors.codeKey
            JsonToken.Kind.String -> colors.codeString
            JsonToken.Kind.Number -> colors.codeNumber
            JsonToken.Kind.Literal -> colors.codeLiteral
            JsonToken.Kind.Punctuation -> colors.codePunctuation
            JsonToken.Kind.Whitespace, JsonToken.Kind.Other -> null
        }
        if (color == null) append(token.text) else withStyle(SpanStyle(color = color)) { append(token.text) }
    }
}

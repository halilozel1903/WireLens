package io.github.halilozel1903.wirelens.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.halilozel1903.wirelens.core.StatusCategory

/** The inspector's own colors, independent from the host app's theme. */
@Immutable
public data class WireLensColors(
    val isDark: Boolean,
    val header: Brush,
    val get: Color = Color(0xFF3B82F6),
    val post: Color = Color(0xFF10B981),
    val put: Color = Color(0xFFF59E0B),
    val patch: Color = Color(0xFFA855F7),
    val delete: Color = Color(0xFFEF4444),
    val otherMethod: Color = Color(0xFF64748B),
    val success: Color = Color(0xFF10B981),
    val redirect: Color = Color(0xFF0EA5E9),
    val clientError: Color = Color(0xFFF59E0B),
    val serverError: Color = Color(0xFFEF4444),
    val failed: Color = Color(0xFFE11D48),
    val pending: Color = Color(0xFF94A3B8),
    val codeBackground: Color = Color(0xFF14121F),
    val codeText: Color = Color(0xFFE4E1F5),
    val codeKey: Color = Color(0xFFC4A5FF),
    val codeString: Color = Color(0xFF9BE5A6),
    val codeNumber: Color = Color(0xFFFFB86B),
    val codeLiteral: Color = Color(0xFFFF7AA8),
    val codePunctuation: Color = Color(0xFF8A86A6),
    val codeGutter: Color = Color(0xFF5A5672),
) {
    public fun method(method: String): Color = when (method.uppercase()) {
        "GET" -> get
        "POST" -> post
        "PUT" -> put
        "PATCH" -> patch
        "DELETE" -> delete
        else -> otherMethod
    }

    public fun status(category: StatusCategory): Color = when (category) {
        StatusCategory.Pending -> pending
        StatusCategory.Success -> success
        StatusCategory.Redirect -> redirect
        StatusCategory.ClientError -> clientError
        StatusCategory.ServerError -> serverError
        StatusCategory.Failed -> failed
    }

    public companion object {
        public val Light: WireLensColors = WireLensColors(
            isDark = false,
            header = Brush.linearGradient(listOf(Color(0xFF5B3DF5), Color(0xFF7C3AED), Color(0xFF0EA5E9))),
        )
        public val Dark: WireLensColors = WireLensColors(
            isDark = true,
            header = Brush.linearGradient(listOf(Color(0xFF3A1FC9), Color(0xFF5B21B6), Color(0xFF0369A1))),
            codeBackground = Color(0xFF08070D),
        )
    }
}

internal val LocalWireLensColors = staticCompositionLocalOf { WireLensColors.Light }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B3DF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E4FF),
    onPrimaryContainer = Color(0xFF1C0B6B),
    secondary = Color(0xFF0EA5E9),
    background = Color(0xFFF5F4FA),
    onBackground = Color(0xFF15131F),
    surface = Color.White,
    onSurface = Color(0xFF15131F),
    surfaceVariant = Color(0xFFEFEDF7),
    onSurfaceVariant = Color(0xFF63607A),
    outline = Color(0xFFD9D6E6),
    outlineVariant = Color(0xFFE8E6F0),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA997FF),
    onPrimary = Color(0xFF1C0B6B),
    primaryContainer = Color(0xFF33268A),
    onPrimaryContainer = Color(0xFFE9E4FF),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF0D0C14),
    onBackground = Color(0xFFEDEBF7),
    surface = Color(0xFF17151F),
    onSurface = Color(0xFFEDEBF7),
    surfaceVariant = Color(0xFF221F2E),
    onSurfaceVariant = Color(0xFFA19DB8),
    outline = Color(0xFF3A3650),
    outlineVariant = Color(0xFF2A2738),
)

/** Material 3 theme with the WireLens palette. Follows the system dark mode by default. */
@Composable
public fun WireLensTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWireLensColors provides if (darkTheme) WireLensColors.Dark else WireLensColors.Light) {
        MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, content = content)
    }
}

/** Accessors for the current WireLens palette. */
public object WireLensTheme {
    public val colors: WireLensColors
        @Composable get() = LocalWireLensColors.current
}

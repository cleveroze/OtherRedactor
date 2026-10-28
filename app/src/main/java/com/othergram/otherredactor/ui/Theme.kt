package com.othergram.otherredactor.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.othergram.otherredactor.AppSettings
import com.othergram.otherredactor.LocalAnimations
import com.othergram.otherredactor.LocalLang
import com.othergram.otherredactor.Palette
import com.othergram.otherredactor.ThemeMode
import kotlin.math.max
import kotlin.math.min

// Colours sampled from the OtherRedactor logo.
val LogoCream = Color(0xFFF3E5CC)
val LogoTan = Color(0xFFC4A386)
val LogoBrown = Color(0xFF8C6D5A)
val LogoDark = Color(0xFF6B4A38)

private val VanillaLight = lightColorScheme(
    primary = Color(0xFF7B5644), onPrimary = Color.White,
    primaryContainer = Color(0xFFF4DBC9), onPrimaryContainer = Color(0xFF2E150A),
    secondary = Color(0xFF74594A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFDCCB), onSecondaryContainer = Color(0xFF2A170D),
    tertiary = Color(0xFF5F6237), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E8B1), onTertiaryContainer = Color(0xFF1B1D00),
    background = Color(0xFFFDF6EC), onBackground = Color(0xFF221A15),
    surface = Color(0xFFFDF6EC), onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF1E1D3), onSurfaceVariant = Color(0xFF52443B),
    surfaceTint = Color(0xFF7B5644),
    outline = Color(0xFF85746A), outlineVariant = Color(0xFFD7C3B6),
    inverseSurface = Color(0xFF382E28), inverseOnSurface = Color(0xFFFAEEE4),
    inversePrimary = Color(0xFFEDBDA6),
    surfaceBright = Color(0xFFFDF6EC), surfaceDim = Color(0xFFE6D7C9),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF9EEE2),
    surfaceContainer = Color(0xFFF4E7D9), surfaceContainerHigh = Color(0xFFEEE1D2),
    surfaceContainerHighest = Color(0xFFE8DACC),
)

private val VanillaDark = darkColorScheme(
    primary = Color(0xFFE9C2A6), onPrimary = Color(0xFF45291A),
    primaryContainer = Color(0xFF5E3F2E), onPrimaryContainer = Color(0xFFFFDCC8),
    secondary = Color(0xFFDCC4B0), onSecondary = Color(0xFF3E2D21),
    secondaryContainer = Color(0xFF574437), onSecondaryContainer = Color(0xFFF8E0CC),
    tertiary = Color(0xFFC9CC97), onTertiary = Color(0xFF31330D),
    tertiaryContainer = Color(0xFF484A22), onTertiaryContainer = Color(0xFFE5E8B1),
    background = Color(0xFF19130F), onBackground = Color(0xFFEFE0D5),
    surface = Color(0xFF19130F), onSurface = Color(0xFFEFE0D5),
    surfaceVariant = Color(0xFF52443B), onSurfaceVariant = Color(0xFFD7C3B6),
    surfaceTint = Color(0xFFE9C2A6),
    outline = Color(0xFF9F8D82), outlineVariant = Color(0xFF52443B),
    inverseSurface = Color(0xFFEFE0D5), inverseOnSurface = Color(0xFF382E28),
    inversePrimary = Color(0xFF7B5644),
    surfaceBright = Color(0xFF41372F), surfaceDim = Color(0xFF19130F),
    surfaceContainerLowest = Color(0xFF130E0A), surfaceContainerLow = Color(0xFF211A16),
    surfaceContainer = Color(0xFF261E19), surfaceContainerHigh = Color(0xFF312823),
    surfaceContainerHighest = Color(0xFF3C332D),
)

fun Palette.seed(): Color = when (this) {
    Palette.VANILLA, Palette.DYNAMIC -> LogoBrown
    Palette.MOCHA -> Color(0xFF6F4E37)
    Palette.SAGE -> Color(0xFF6B8E5A)
    Palette.OCEAN -> Color(0xFF3F7CAC)
    Palette.ROSE -> Color(0xFFB5566E)
    Palette.LAVENDER -> Color(0xFF7E6BB5)
}

private fun Color.toHsl(): FloatArray {
    val r = red; val g = green; val b = blue
    val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
    val l = (mx + mn) / 2f
    if (mx == mn) return floatArrayOf(0f, 0f, l)
    val d = mx - mn
    val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f))
        g -> ((b - r) / d + 2f)
        else -> ((r - g) / d + 4f)
    } * 60f
    return floatArrayOf(h, s, l)
}

/** Hand-rolled tonal scheme from one seed colour, close enough to M3 tones. */
private fun seedScheme(seed: Color, dark: Boolean): ColorScheme {
    val (h, s0, _) = seed.toHsl().let { Triple(it[0], it[1], it[2]) }
    val s = s0.coerceIn(0.25f, 0.75f)
    fun p(l: Float, sat: Float = s) = Color.hsl(h, sat.coerceIn(0f, 1f), l)
    fun sec(l: Float) = Color.hsl(h, s * 0.35f, l)
    val th = (h + 50f) % 360f
    fun ter(l: Float) = Color.hsl(th, s * 0.6f, l)
    fun n(l: Float) = Color.hsl(h, s * 0.18f, l)
    fun nv(l: Float) = Color.hsl(h, s * 0.28f, l)
    return if (!dark) lightColorScheme(
        primary = p(0.38f), onPrimary = Color.White,
        primaryContainer = p(0.88f), onPrimaryContainer = p(0.12f),
        secondary = sec(0.38f), onSecondary = Color.White,
        secondaryContainer = sec(0.88f), onSecondaryContainer = sec(0.12f),
        tertiary = ter(0.36f), onTertiary = Color.White,
        tertiaryContainer = ter(0.87f), onTertiaryContainer = ter(0.12f),
        background = n(0.975f), onBackground = n(0.11f),
        surface = n(0.975f), onSurface = n(0.11f),
        surfaceVariant = nv(0.9f), onSurfaceVariant = nv(0.3f),
        surfaceTint = p(0.38f),
        outline = nv(0.5f), outlineVariant = nv(0.8f),
        inverseSurface = n(0.2f), inverseOnSurface = n(0.95f), inversePrimary = p(0.8f),
        surfaceBright = n(0.975f), surfaceDim = n(0.86f),
        surfaceContainerLowest = Color.White, surfaceContainerLow = n(0.955f),
        surfaceContainer = n(0.935f), surfaceContainerHigh = n(0.915f),
        surfaceContainerHighest = n(0.895f),
    ) else darkColorScheme(
        primary = p(0.78f), onPrimary = p(0.18f),
        primaryContainer = p(0.28f), onPrimaryContainer = p(0.9f),
        secondary = sec(0.78f), onSecondary = sec(0.18f),
        secondaryContainer = sec(0.28f), onSecondaryContainer = sec(0.9f),
        tertiary = ter(0.76f), onTertiary = ter(0.18f),
        tertiaryContainer = ter(0.28f), onTertiaryContainer = ter(0.9f),
        background = n(0.07f), onBackground = n(0.9f),
        surface = n(0.07f), onSurface = n(0.9f),
        surfaceVariant = nv(0.28f), onSurfaceVariant = nv(0.8f),
        surfaceTint = p(0.78f),
        outline = nv(0.58f), outlineVariant = nv(0.28f),
        inverseSurface = n(0.9f), inverseOnSurface = n(0.2f), inversePrimary = p(0.38f),
        surfaceBright = n(0.22f), surfaceDim = n(0.07f),
        surfaceContainerLowest = n(0.045f), surfaceContainerLow = n(0.1f),
        surfaceContainer = n(0.12f), surfaceContainerHigh = n(0.16f),
        surfaceContainerHighest = n(0.2f),
    )
}

private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0908),
    surfaceContainer = Color(0xFF100D0B),
    surfaceContainerHigh = Color(0xFF1A1613),
    surfaceContainerHighest = Color(0xFF24201C),
)

@Immutable
data class SyntaxColors(
    val tag: Color, val attr: Color, val string: Color, val comment: Color,
    val keyword: Color, val number: Color, val punct: Color, val func: Color,
    val prop: Color, val selector: Color, val search: Color, val currentLine: Color,
    val plain: Color,
)

fun syntaxColors(dark: Boolean): SyntaxColors = if (dark) SyntaxColors(
    tag = Color(0xFFE8A87C), attr = Color(0xFFE6C17A), string = Color(0xFFA9C98A),
    comment = Color(0xFF8A7F76), keyword = Color(0xFFD3A6E8), number = Color(0xFFF0A07A),
    punct = Color(0xFFA8998C), func = Color(0xFF8EC5E0), prop = Color(0xFF9BD1C0),
    selector = Color(0xFFE8A87C), search = Color(0x66E6C17A), currentLine = Color(0x14FFFFFF), plain = Color(0xFFEFE0D5),
) else SyntaxColors(
    tag = Color(0xFF9A4A26), attr = Color(0xFF8E6316), string = Color(0xFF4E7A3A),
    comment = Color(0xFF9C8F84), keyword = Color(0xFF7A3E8E), number = Color(0xFFB5562E),
    punct = Color(0xFF8C7B6E), func = Color(0xFF2F6B8A), prop = Color(0xFF2E7366),
    selector = Color(0xFF9A4A26), search = Color(0x66E0B040), currentLine = Color(0x0F5A3A20), plain = Color(0xFF221A15),
)

val LocalSyntaxColors = staticCompositionLocalOf { syntaxColors(false) }
val LocalIsDark = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OtherTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val ctx = LocalContext.current
    var scheme = when (settings.palette) {
        Palette.VANILLA -> if (dark) VanillaDark else VanillaLight
        Palette.DYNAMIC -> if (Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else if (dark) VanillaDark else VanillaLight
        else -> seedScheme(settings.palette.seed(), dark)
    }
    if (dark && settings.amoled) scheme = scheme.amoled()

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )
    CompositionLocalProvider(
        LocalLang provides settings.lang,
        LocalAnimations provides settings.animations,
        LocalSyntaxColors provides syntaxColors(dark),
        LocalIsDark provides dark,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = if (settings.animations) MotionScheme.expressive() else MotionScheme.standard(),
            shapes = shapes,
            content = content,
        )
    }
}

/** True when a colour is perceptually light – used for contrast on swatches. */
fun Color.isLight(): Boolean = luminance() > 0.5f

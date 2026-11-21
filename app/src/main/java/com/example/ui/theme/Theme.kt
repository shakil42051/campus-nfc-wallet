package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = GoldChip,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF422006),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF022C22),
    background = PolishDarkBackground,
    onBackground = TextPrimaryOnDark,
    surface = PolishDarkSurface,
    onSurface = TextPrimaryOnDark,
    surfaceVariant = PolishDarkCard,
    onSurfaceVariant = TextSecondaryOnDark,
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoPrimaryDark,
    secondary = GoldChip,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = PolishLightBackground,
    onBackground = TextPrimaryOnLight,
    surface = PolishLightSurface,
    onSurface = TextPrimaryOnLight,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondaryOnLight,
    outline = Color(0xFF94A3B8),
    outlineVariant = PolishLightCardBorder,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun CampusNfcWalletTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

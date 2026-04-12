package com.sarmaya.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Semantic finance color holder accessible via LocalSarmayaColors */
data class SarmayaFinanceColors(
    val profit: Color = GainGreen,
    val profitContainer: Color = GainGreenLight,
    val onProfitContainer: Color = GainGreenDark,
    val loss: Color = LossRed,
    val lossContainer: Color = LossRedLight,
    val onLossContainer: Color = LossRedDark,
    val warning: Color = WarningAmber,
    val warningContainer: Color = WarningAmberLight,
    val dividend: Color = DividendBlue,
    val dividendContainer: Color = DividendBlueLight,
    val cardSurface: Color = LightCardSurface,
    val neutral: Color = NeutralGray,
    
    // Market States
    val marketPreOpen: Color = MarketPreOpen,
    val marketOpen: Color = MarketOpen,
    val marketSuspended: Color = MarketSuspended,
    val marketClosed: Color = MarketClosed,
    val marketOffline: Color = MarketOffline
)

val LocalSarmayaColors = staticCompositionLocalOf { SarmayaFinanceColors() }

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    secondary = TealSecondary,
    tertiary = TealTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    primaryContainer = Color(0xFF042F2E), // Tailwind Teal 900
    onPrimaryContainer = TealSecondary,
    errorContainer = LossRedDark,
    onErrorContainer = LossRed
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    secondary = TealSecondary,
    tertiary = TealTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    primaryContainer = Color(0xFFCCFBF1), // Tailwind Teal 100
    onPrimaryContainer = Color(0xFF115E59), // Tailwind Teal 800
    errorContainer = LossRedLight,
    onErrorContainer = LossRedDark
)

@Composable
fun SarmayaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val financeColors = if (darkTheme) {
        SarmayaFinanceColors(
            profitContainer = Color(0xFF064E3B), // Tailwind Emerald 900
            onProfitContainer = GainGreen,
            lossContainer = Color(0xFF7F1D1D), // Tailwind Red 900
            onLossContainer = LossRed,
            warningContainer = Color(0xFF78350F), // Tailwind Amber 900
            dividendContainer = Color(0xFF1E3A8A), // Tailwind Blue 900
            cardSurface = DarkCardSurface
        )
    } else {
        SarmayaFinanceColors()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalSarmayaColors provides financeColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

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
    val profit: Color = ProfitGreen,
    val profitContainer: Color = ProfitGreenLight,
    val onProfitContainer: Color = ProfitGreenDark,
    val loss: Color = LossRed,
    val lossContainer: Color = LossRedLight,
    val onLossContainer: Color = LossRedDark,
    val warning: Color = WarningAmber,
    val warningContainer: Color = WarningAmberLight,
    val dividend: Color = DividendBlue,
    val dividendContainer: Color = DividendBlueLight,
    val cardSurface: Color = LightCardSurface,
    val neutral: Color = NeutralGray
)

val LocalSarmayaColors = staticCompositionLocalOf { SarmayaFinanceColors() }

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    tertiary = EmeraldTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    primaryContainer = Color(0xFF0D3D2E),
    onPrimaryContainer = EmeraldSecondary,
    errorContainer = Color(0xFF3D1212),
    onErrorContainer = LossRed
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    tertiary = EmeraldTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
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
            profitContainer = Color(0xFF0D3D1E),
            onProfitContainer = ProfitGreen,
            lossContainer = Color(0xFF3D1212),
            onLossContainer = LossRed,
            warningContainer = Color(0xFF3D2E0A),
            dividendContainer = Color(0xFF1E2A4A),
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

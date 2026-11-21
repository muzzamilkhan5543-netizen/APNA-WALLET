package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppConfigEntity

@Immutable
data class WalletPalette(
    val headerRed: Color = CrimsonRed,
    val headerRedDark: Color = DeepCrimson,
    val accentYellow: Color = VibrantYellow,
    val pageWhite: Color = PureWhite,
    val inputText: Color = Color.Black,
    val cardBorder: Color = BorderLight,
    val inkDark: Color = InkDark,
    val mutedText: Color = MutedGray,
    val success: Color = SuccessGreen,
    val danger: Color = DangerRed
)

val LocalWalletPalette = staticCompositionLocalOf { WalletPalette() }

@Composable
fun ApnaWalletTheme(
    appConfig: AppConfigEntity = AppConfigEntity(),
    content: @Composable () -> Unit
) {
    val primary = Color(appConfig.primaryColorHex)
    val accent = Color(appConfig.accentTextColorHex)
    val pageBg = Color(appConfig.pageBackgroundHex)
    val inputColor = Color(appConfig.inputTextColorHex)

    val darkerPrimary = Color(
        red = (primary.red * 0.78f).coerceIn(0f, 1f),
        green = (primary.green * 0.78f).coerceIn(0f, 1f),
        blue = (primary.blue * 0.78f).coerceIn(0f, 1f),
        alpha = 1f
    )

    val palette = WalletPalette(
        headerRed = primary,
        headerRedDark = darkerPrimary,
        accentYellow = accent,
        pageWhite = pageBg,
        inputText = inputColor
    )

    val colorScheme = lightColorScheme(
        primary = primary,
        onPrimary = accent,
        primaryContainer = darkerPrimary,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = primary,
        background = pageBg,
        onBackground = InkDark,
        surface = pageBg,
        onSurface = InkDark,
        surfaceVariant = SoftSurface,
        onSurfaceVariant = MutedGray,
        outline = BorderLight,
        error = DangerRed,
        onError = PureWhite
    )

    CompositionLocalProvider(LocalWalletPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

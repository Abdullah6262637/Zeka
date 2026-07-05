package com.zeka.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = WhiteAccent,
    onPrimary = PureBlack,
    background = PureBlack,
    onBackground = OffWhite,
    surface = Graphite,
    onSurface = OffWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = OffWhite,
    outline = DividerColor
)

@Composable
fun ZekaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

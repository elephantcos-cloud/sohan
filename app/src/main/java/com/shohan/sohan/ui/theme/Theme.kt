package com.shohan.sohan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.shohan.sohan.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary   = Blue80,
    secondary = Green80,
    error     = Red80
)

private val LightColors = lightColorScheme(
    primary   = Blue40,
    secondary = Green40,
    error     = Red40
)

@Composable
fun SohanTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography  = Typography,
        content     = content
    )
}

package com.yourapp.timemanagement.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.yourapp.timemanagement.domain.ThemeMode
import com.yourapp.timemanagement.domain.UserSettings

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = CalmCoral,
    tertiary = CalmViolet,
    background = Night900,
    onBackground = Color(0xFFE5EEF7),
    surface = Night800,
    onSurface = Color(0xFFE5EEF7),
    surfaceVariant = Night700,
    onSurfaceVariant = Color(0xFFC8D4E5),
    outline = Color(0xFF37506C),
)

private fun amoledScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.Black,
    secondary = CalmCoral,
    tertiary = CalmViolet,
    background = AmoledBlack,
    onBackground = Color(0xFFF4F7FB),
    surface = AmoledSurface,
    onSurface = Color(0xFFF4F7FB),
    surfaceVariant = AmoledVariant,
    onSurfaceVariant = Color(0xFFD6DEE9),
    outline = Color(0xFF2A2A2A),
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = CalmBlue,
    tertiary = CalmCoral,
    background = Mist50,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Mist100,
    onSurfaceVariant = Ink700,
    outline = Mist200,
)

@Composable
fun TimeManagementTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Amoled -> true
    }
    val accent = Color(settings.accentColor)
    val context = LocalContext.current
    val scheme = when {
        settings.themeMode == ThemeMode.Amoled -> amoledScheme(accent)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settings.themeMode == ThemeMode.System ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkScheme(accent)
        else -> lightScheme(accent)
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content,
    )
}

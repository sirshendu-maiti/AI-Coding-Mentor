package com.euphoria.aimentor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Custom App Colors
val MentorPrimary = Color(0xFF6C63FF)
val MentorSecondary = Color(0xFF03DAC6)
val MentorBackground = Color(0xFF0F0F1A)
val MentorSurface = Color(0xFF1C1C2E)
val MentorSurfaceVariant = Color(0xFF252538)
val MentorOnSurface = Color(0xFFE6E1FF)
val MentorError = Color(0xFFFF6B6B)
val MentorSuccess = Color(0xFF4CAF50)
val MentorWarning = Color(0xFFFFA726)
val MentorCode = Color(0xFF1E1E2E)
val MentorCodeText = Color(0xFFCDD6F4)

private val DarkColorScheme = darkColorScheme(
    primary = MentorPrimary,
    secondary = MentorSecondary,
    tertiary = Pink80,
    background = MentorBackground,
    surface = MentorSurface,
    surfaceVariant = MentorSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = MentorOnSurface,
    onSurface = MentorOnSurface,
    error = MentorError
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun AICodingMentorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

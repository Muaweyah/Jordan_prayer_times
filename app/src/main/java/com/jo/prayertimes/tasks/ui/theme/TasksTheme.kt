package com.jo.prayertimes.tasks.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jo.prayertimes.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold)
)

private val md3LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF625B71),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    error = Color(0xFFB3261E)
)

private val md3DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    secondary = Color(0xFFCCC2DC),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5)
)

private val baseTypography = Typography()
private val cairoTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = CairoFontFamily),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = CairoFontFamily),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = CairoFontFamily),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = CairoFontFamily),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = CairoFontFamily),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = CairoFontFamily),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = CairoFontFamily),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = CairoFontFamily),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = CairoFontFamily),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = CairoFontFamily),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = CairoFontFamily),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = CairoFontFamily),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = CairoFontFamily),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = CairoFontFamily),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = CairoFontFamily)
)

@Composable
fun TasksAppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) md3DarkColors else md3LightColors,
        typography = cairoTypography,
        content = content
    )
}

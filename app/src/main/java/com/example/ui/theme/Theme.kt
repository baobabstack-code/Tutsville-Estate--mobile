package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TutsvilleLightColorScheme = lightColorScheme(
  primary = TutsvillePrimary,
  onPrimary = TutsvilleOnPrimary,
  primaryContainer = TutsvillePrimaryContainer,
  onPrimaryContainer = TutsvilleOnPrimaryContainer,
  secondary = TutsvilleSecondary,
  onSecondary = TutsvilleOnSecondary,
  secondaryContainer = TutsvilleSecondaryContainer,
  onSecondaryContainer = TutsvilleOnSecondaryContainer,
  tertiary = TutsvilleTertiary,
  onTertiary = TutsvilleOnTertiary,
  background = TutsvilleBackground,
  onBackground = TutsvilleOnBackground,
  surface = TutsvilleSurface,
  onSurface = TutsvilleOnSurface,
  surfaceVariant = TutsvilleSurfaceVariant,
  onSurfaceVariant = TutsvilleOnSurfaceVariant,
  outline = TutsvilleOutline,
  outlineVariant = TutsvilleOutlineVariant,
  error = Color(0xFFBA1A1A),
  onError = Color.White
)

private val TutsvilleDarkColorScheme = darkColorScheme(
  primary = Color(0xFFA9CFB9),
  onPrimary = Color(0xFF002517),
  primaryContainer = Color(0xFF183B2B),
  onPrimaryContainer = Color(0xFFC4ECD4),
  secondary = Color(0xFFF4BD6F),
  onSecondary = Color(0xFF291800),
  secondaryContainer = Color(0xFF633F00),
  onSecondaryContainer = Color(0xFFFFDDB3),
  tertiary = Color(0xFFA3D1B8),
  onTertiary = Color(0xFF002114),
  background = Color(0xFF141A17),
  onBackground = Color(0xFFE5E2DA),
  surface = Color(0xFF1B231F),
  onSurface = Color(0xFFF1EEE6),
  surfaceVariant = Color(0xFF28342D),
  onSurfaceVariant = Color(0xFFC1C8C2),
  outline = Color(0xFF3F4E44)
)

@Composable
fun TutsvilleTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) TutsvilleDarkColorScheme else TutsvilleLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  TutsvilleTheme(darkTheme = darkTheme, content = content)
}

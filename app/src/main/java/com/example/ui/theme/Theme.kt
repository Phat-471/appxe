package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkCockpitColorScheme = darkColorScheme(
  primary = AlertAmberPrimary,
  onPrimary = CockpitBackground,
  primaryContainer = AlertAmberDark,
  onPrimaryContainer = CockpitTextPrimary,
  secondary = AlertEmeraldSafe,
  onSecondary = CockpitBackground,
  secondaryContainer = AlertEmeraldDark,
  onSecondaryContainer = CockpitTextPrimary,
  tertiary = AlertCrimsonDanger,
  onTertiary = CockpitTextPrimary,
  tertiaryContainer = AlertCrimsonDanger,
  onTertiaryContainer = CockpitTextPrimary,
  background = CockpitBackground,
  onBackground = CockpitTextPrimary,
  surface = CockpitSurface,
  onSurface = CockpitTextPrimary,
  surfaceVariant = CockpitSurfaceElevated,
  onSurfaceVariant = CockpitTextSecondary,
  outline = CockpitCardBorder,
  error = AlertCrimsonDanger,
  onError = CockpitTextPrimary
)

private val LightCockpitColorScheme = lightColorScheme(
  primary = NavRouteBlue,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0EDFF),
  onPrimaryContainer = NavRouteBlue,
  secondary = AlertEmeraldSafe,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDCFCE7),
  onSecondaryContainer = AlertEmeraldDark,
  tertiary = AlertCrimsonDanger,
  onTertiary = Color.White,
  background = NavLightBackground,
  onBackground = NavLightTextPrimary,
  surface = NavLightSurface,
  onSurface = NavLightTextPrimary,
  surfaceVariant = NavLightSurfaceElevated,
  onSurfaceVariant = NavLightTextSecondary,
  outline = NavLightCardBorder,
  error = AlertCrimsonDanger,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Default to bright clean light theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkCockpitColorScheme else LightCockpitColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = CockpitTypography,
    content = content
  )
}

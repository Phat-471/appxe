package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CockpitTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Black,
    fontSize = 56.sp,
    lineHeight = 64.sp,
    letterSpacing = (-1).sp
  ),
  displayMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 52.sp,
    letterSpacing = (-0.5).sp
  ),
  headlineLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = 0.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.sp
  ),
  titleLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.15.sp
  ),
  titleMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.15.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.25.sp
  ),
  labelLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.5.sp
  ),
  labelSmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp
  )
)

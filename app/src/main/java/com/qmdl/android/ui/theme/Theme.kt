package com.qmdl.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qmdl.android.data.ThemeMode

// Expressive Emerald + Jade + Amethyst palette (Android 8..11 fallback & brand identity)
private val QmdlDarkScheme = darkColorScheme(
  primary = Color(0xFF4BE396),
  onPrimary = Color(0xFF00381F),
  primaryContainer = Color(0xFF00522F),
  onPrimaryContainer = Color(0xFFA7F7CA),
  secondary = Color(0xFF7FD5BE),
  onSecondary = Color(0xFF08382C),
  secondaryContainer = Color(0xFF1D4E41),
  onSecondaryContainer = Color(0xFFC5F2E4),
  tertiary = Color(0xFFD0AEFF),
  onTertiary = Color(0xFF391866),
  tertiaryContainer = Color(0xFF51307F),
  onTertiaryContainer = Color(0xFFEEDCFF),
  background = Color(0xFF0A110E),
  onBackground = Color(0xFFE3EFE9),
  surface = Color(0xFF0D1512),
  onSurface = Color(0xFFE3EFE9),
  surfaceVariant = Color(0xFF1B2B25),
  onSurfaceVariant = Color(0xFF9FB6AC),
  surfaceContainerLowest = Color(0xFF070D0B),
  surfaceContainerLow = Color(0xFF111C18),
  surfaceContainer = Color(0xFF15221D),
  surfaceContainerHigh = Color(0xFF1C2C26),
  surfaceContainerHighest = Color(0xFF24362F),
  outline = Color(0xFF415950),
  outlineVariant = Color(0xFF283A33),
  error = Color(0xFFFF8989),
  onError = Color(0xFF5C0B0B),
  errorContainer = Color(0xFF7E1D1D),
  onErrorContainer = Color(0xFFFFDAD6),
)

private val QmdlLightScheme = lightColorScheme(
  primary = Color(0xFF006D40),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFF8FF7BE),
  onPrimaryContainer = Color(0xFF002110),
  secondary = Color(0xFF2E6857),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFB5EEDB),
  onSecondaryContainer = Color(0xFF072018),
  tertiary = Color(0xFF6B42A4),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFEEDCFF),
  onTertiaryContainer = Color(0xFF250059),
  background = Color(0xFFF3F9F5),
  onBackground = Color(0xFF111F19),
  surface = Color(0xFFF5FAF7),
  onSurface = Color(0xFF111F19),
  surfaceVariant = Color(0xFFDCEAE3),
  onSurfaceVariant = Color(0xFF42574E),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFECF4EF),
  surfaceContainer = Color(0xFFE6EFEA),
  surfaceContainerHigh = Color(0xFFDFEAE4),
  surfaceContainerHighest = Color(0xFFD8E4DE),
  outline = Color(0xFF6D8279),
  outlineVariant = Color(0xFFC0D1C9),
  error = Color(0xFFBA1A1A),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
)

private val ExpressiveShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(28.dp),
  extraLarge = RoundedCornerShape(34.dp),
)

private val ExpressiveTypography = Typography(
  headlineMedium = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.4).sp,
  ),
  titleLarge = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
  ),
  titleMedium = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  labelLarge = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.5.sp,
    lineHeight = 18.sp,
  ),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QmdlExpressiveTheme(
  themeMode: ThemeMode,
  dynamicColor: Boolean,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }
  val context = LocalContext.current
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> QmdlDarkScheme
    else -> QmdlLightScheme
  }

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    motionScheme = MotionScheme.expressive(),
    shapes = ExpressiveShapes,
    typography = ExpressiveTypography,
    content = content,
  )
}

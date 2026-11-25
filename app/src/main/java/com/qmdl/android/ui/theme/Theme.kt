package com.qmdl.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qmdl.android.data.ThemeMode
import com.qmdl.android.ui.liquid.LocalLiquidDark

// Luminous Liquid Glass Emerald + Aqua + Amethyst Palette
private val QmdlLiquidDarkScheme = darkColorScheme(
  primary = Color(0xFF4BE8A0),
  onPrimary = Color(0xFF003820),
  primaryContainer = Color(0xFF0C4D33),
  onPrimaryContainer = Color(0xFFB4FBD7),
  secondary = Color(0xFF6CD9E8),
  onSecondary = Color(0xFF00363E),
  secondaryContainer = Color(0xFF144B56),
  onSecondaryContainer = Color(0xFFC5F4FB),
  tertiary = Color(0xFFCFA8FF),
  onTertiary = Color(0xFF371664),
  tertiaryContainer = Color(0xFF4D2B7A),
  onTertiaryContainer = Color(0xFFF0E0FF),
  background = Color(0xFF060E14),
  onBackground = Color(0xFFEAF4F1),
  surface = Color(0xFF0B161E),
  onSurface = Color(0xFFEAF4F1),
  surfaceVariant = Color(0xFF172731),
  onSurfaceVariant = Color(0xFFA6BEC4),
  surfaceContainerLowest = Color(0xFF050B10),
  surfaceContainerLow = Color(0xFF0F1B23),
  surfaceContainer = Color(0xFF14222B),
  surfaceContainerHigh = Color(0xFF1B2B36),
  surfaceContainerHighest = Color(0xFF233643),
  outline = Color(0xFF465F6B),
  outlineVariant = Color(0xFF283C47),
  error = Color(0xFFFF8A8A),
  onError = Color(0xFF5C0B0B),
  errorContainer = Color(0xFF7E1D1D),
  onErrorContainer = Color(0xFFFFDAD6),
)

private val QmdlLiquidLightScheme = lightColorScheme(
  primary = Color(0xFF007A4B),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFA4F8CD),
  onPrimaryContainer = Color(0xFF002112),
  secondary = Color(0xFF0E6C7C),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFB8F0FA),
  onSecondaryContainer = Color(0xFF002026),
  tertiary = Color(0xFF6B3FA8),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFEFDFFF),
  onTertiaryContainer = Color(0xFF24005A),
  background = Color(0xFFEEF6F4),
  onBackground = Color(0xFF0E1D21),
  surface = Color(0xFFF4FAF8),
  onSurface = Color(0xFF0E1D21),
  surfaceVariant = Color(0xFFDCEBE8),
  onSurfaceVariant = Color(0xFF445D63),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFEAF4F1),
  surfaceContainer = Color(0xFFE3EFEC),
  surfaceContainerHigh = Color(0xFFDCEAE6),
  surfaceContainerHighest = Color(0xFFD4E4DF),
  outline = Color(0xFF6B848B),
  outlineVariant = Color(0xFFBBD0D4),
  error = Color(0xFFC62828),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
)

private val LiquidShapes = Shapes(
  extraSmall = RoundedCornerShape(10.dp),
  small = RoundedCornerShape(16.dp),
  medium = RoundedCornerShape(22.dp),
  large = RoundedCornerShape(28.dp),
  extraLarge = RoundedCornerShape(34.dp),
)

private val LiquidTypography = Typography(
  headlineMedium = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 27.sp,
    lineHeight = 33.sp,
    letterSpacing = (-0.5).sp,
  ),
  titleLarge = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.2).sp,
  ),
  titleMedium = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  titleSmall = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.5.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.5.sp,
    lineHeight = 18.sp,
  ),
)

@Composable
fun QmdlLiquidTheme(
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
    darkTheme -> QmdlLiquidDarkScheme
    else -> QmdlLiquidLightScheme
  }

  CompositionLocalProvider(LocalLiquidDark provides darkTheme) {
    MaterialTheme(
      colorScheme = colorScheme,
      shapes = LiquidShapes,
      typography = LiquidTypography,
      content = content,
    )
  }
}

@Composable
fun QmdlExpressiveTheme(
  themeMode: ThemeMode,
  dynamicColor: Boolean,
  content: @Composable () -> Unit,
) = QmdlLiquidTheme(themeMode = themeMode, dynamicColor = dynamicColor, content = content)

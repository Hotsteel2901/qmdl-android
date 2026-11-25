package com.qmdl.android.ui.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh

/**
 * CompositionLocal carrying the root [Backdrop] and dark-mode flag so every
 * screen and component renders genuine Liquid Glass refraction through the
 * live ambient wallpaper and scrolling content behind it.
 */
val LocalLiquidBackdrop = staticCompositionLocalOf<Backdrop> {
  error("LocalLiquidBackdrop not provided")
}

val LocalLiquidDark = staticCompositionLocalOf { false }

internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

/**
 * Full-bleed luminous ambient mesh canvas that gives every Liquid Glass lens
 * rich specular color gradients to refract and disperse.
 */
@Composable
fun LiquidAmbientWallpaper(
  dark: Boolean,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val baseTop = if (dark) Color(0xFF060E14) else Color(0xFFE9F5F0)
  val baseMid = if (dark) Color(0xFF0A1922) else Color(0xFFF2F7FA)
  val baseBot = if (dark) Color(0xFF0D1426) else Color(0xFFECEAF8)

  Canvas(modifier = modifier.fillMaxSize()) {
    drawRect(
      brush = Brush.verticalGradient(listOf(baseTop, baseMid, baseBot)),
    )
    // Soft luminous aurora orbs for realistic glass refraction & chromatic dispersion
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          primary.copy(alpha = if (dark) 0.34f else 0.28f),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.16f, size.height * 0.12f),
        radius = size.maxDimension * 0.48f,
      ),
      center = Offset(size.width * 0.16f, size.height * 0.12f),
      radius = size.maxDimension * 0.48f,
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          tertiary.copy(alpha = if (dark) 0.28f else 0.22f),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.88f, size.height * 0.32f),
        radius = size.maxDimension * 0.45f,
      ),
      center = Offset(size.width * 0.88f, size.height * 0.32f),
      radius = size.maxDimension * 0.45f,
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          secondary.copy(alpha = if (dark) 0.26f else 0.24f),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.25f, size.height * 0.84f),
        radius = size.maxDimension * 0.50f,
      ),
      center = Offset(size.width * 0.25f, size.height * 0.84f),
      radius = size.maxDimension * 0.50f,
    )
  }
}

/**
 * Genuine Liquid Glass Card using `io.github.kyant0:backdrop` `drawBackdrop`
 * with vibrancy, Gaussian blur, physical lens refraction, specular highlight
 * and subtle translucent surface tint.
 */
@Composable
fun LiquidGlassCard(
  modifier: Modifier = Modifier,
  backdrop: Backdrop = LocalLiquidBackdrop.current,
  cornerRadius: Dp = 26.dp,
  tint: Color = Color.Unspecified,
  blurRadius: Dp = 14.dp,
  refractionHeight: Dp = 14.dp,
  refractionAmount: Dp = 24.dp,
  onClick: (() -> Unit)? = null,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val isDark = LocalLiquidDark.current
  val surfaceColor = if (isDark) Color(0xFF151E24).copy(alpha = 0.44f) else Color.White.copy(alpha = 0.48f)
  val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.65f)

  Column(
    modifier = modifier
      .drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedRectangle(cornerRadius) },
        effects = {
          vibrancy()
          blur(blurRadius.toPx())
          lens(refractionHeight.toPx(), refractionAmount.toPx())
        },
        highlight = { Highlight.Default.copy(alpha = if (isDark) 0.55f else 0.85f) },
        shadow = {
          Shadow(
            radius = 12.dp,
            color = Color.Black.copy(alpha = if (isDark) 0.26f else 0.08f),
          )
        },
        onDrawSurface = {
          if (tint.isSpecified) {
            drawRect(tint.copy(alpha = if (isDark) 0.24f else 0.18f))
          }
          drawRect(surfaceColor)
        },
      )
      .border(0.75.dp, borderColor, RoundedCornerShape(cornerRadius))
      .then(
        if (onClick != null) {
          Modifier.clickable(onClick = onClick)
        } else {
          Modifier
        },
      )
      .padding(contentPadding),
    content = content,
  )
}

/**
 * Full Kyant0-style interactive Liquid Glass Capsule Button with spring
 * squash/stretch, drag offset, AGSL specular highlight, and lens refraction.
 */
@Composable
fun LiquidGlassButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  backdrop: Backdrop = LocalLiquidBackdrop.current,
  enabled: Boolean = true,
  tint: Color = Color.Unspecified,
  height: Dp = 44.dp,
  horizontalPadding: Dp = 16.dp,
  content: @Composable RowScope.() -> Unit,
) {
  val isDark = LocalLiquidDark.current
  val animationScope = rememberCoroutineScope()
  val interactiveHighlight = remember(animationScope) {
    InteractiveHighlight(animationScope = animationScope)
  }
  val defaultSurface = if (isDark) Color(0xFF1E2930).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.58f)

  Row(
    modifier = modifier
      .alpha(if (enabled) 1f else 0.45f)
      .drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = {
          vibrancy()
          blur(4.dp.toPx())
          lens(12.dp.toPx(), 22.dp.toPx())
        },
        highlight = { Highlight.Default.copy(alpha = if (isDark) 0.6f else 0.9f) },
        layerBlock = if (enabled) {
          {
            val width = size.width
            val heightPx = size.height
            if (width > 0f && heightPx > 0f) {
              val progress = interactiveHighlight.pressProgress
              val scale = lerp(1f, 1f + 3.5f.dp.toPx() / heightPx, progress)
              val maxOffset = size.minDimension.coerceAtLeast(1f)
              val offset = interactiveHighlight.offset
              translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
              translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)

              val maxDragScale = 3.5f.dp.toPx() / heightPx
              val offsetAngle = atan2(offset.y, offset.x)
              val maxDim = size.maxDimension.coerceAtLeast(1f)
              scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / maxDim) *
                (width / heightPx).fastCoerceAtMost(1f)
              scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / maxDim) *
                (heightPx / width).fastCoerceAtMost(1f)
            }
          }
        } else null,
        onDrawSurface = {
          if (tint.isSpecified) {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = if (isDark) 0.72f else 0.80f))
          } else {
            drawRect(defaultSurface)
          }
        },
      )
      .clickable(
        enabled = enabled,
        interactionSource = null,
        indication = if (enabled) null else LocalIndication.current,
        role = Role.Button,
        onClick = onClick,
      )
      .then(
        if (enabled) {
          Modifier
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
        } else Modifier,
      )
      .height(height)
      .padding(horizontal = horizontalPadding),
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

/** Compact Liquid Glass Pill Chip for filters, quality, mirror, and tags. */
@Composable
fun LiquidGlassChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  backdrop: Backdrop = LocalLiquidBackdrop.current,
  leading: (@Composable () -> Unit)? = null,
) {
  val isDark = LocalLiquidDark.current
  val accent = MaterialTheme.colorScheme.primary
  val surface = when {
    selected -> accent.copy(alpha = if (isDark) 0.42f else 0.24f)
    isDark -> Color(0xFF1A252C).copy(alpha = 0.45f)
    else -> Color.White.copy(alpha = 0.52f)
  }
  val border = if (selected) {
    accent.copy(alpha = 0.75f)
  } else {
    if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.70f)
  }

  Row(
    modifier = modifier
      .drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = {
          vibrancy()
          blur(6.dp.toPx())
          lens(8.dp.toPx(), 16.dp.toPx())
        },
        highlight = { Highlight.Default.copy(alpha = if (selected) 0.85f else 0.45f) },
        onDrawSurface = { drawRect(surface) },
      )
      .border(if (selected) 1.dp else 0.75.dp, border, CircleShape)
      .clickable(role = Role.Tab, onClick = onClick)
      .padding(horizontal = 13.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    if (leading != null) leading()
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) {
        if (isDark) Color.White else MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurface
      },
      maxLines = 1,
    )
  }
}

/**
 * Full Kyant0-style draggable Liquid Glass Toggle with refractive lens thumb,
 * chromatic aberration, and velocity-based squash physics.
 */
@Composable
fun LiquidGlassToggle(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  backdrop: Backdrop = LocalLiquidBackdrop.current,
) {
  val isDark = LocalLiquidDark.current
  val accentColor = MaterialTheme.colorScheme.primary
  val trackColor = if (isDark) Color(0xFF787880).copy(0.38f) else Color(0xFF787878).copy(0.22f)

  val density = LocalDensity.current
  val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
  val dragWidth = with(density) { 20.dp.toPx() }
  val animationScope = rememberCoroutineScope()
  var didDrag by remember { mutableStateOf(false) }
  var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }

  val dampedDragAnimation = remember(animationScope) {
    DampedDragAnimation(
      animationScope = animationScope,
      initialValue = fraction,
      valueRange = 0f..1f,
      visibilityThreshold = 0.001f,
      initialScale = 1f,
      pressedScale = 1.45f,
      onDragStarted = {},
      onDragStopped = {
        if (didDrag) {
          val next = targetValue >= 0.5f
          fraction = if (next) 1f else 0f
          onCheckedChange(next)
          didDrag = false
        } else {
          val next = !checked
          fraction = if (next) 1f else 0f
          onCheckedChange(next)
        }
      },
      onDrag = { _, dragAmount ->
        if (!didDrag) didDrag = dragAmount.x != 0f
        val delta = dragAmount.x / dragWidth
        fraction = if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
        else (fraction - delta).fastCoerceIn(0f, 1f)
      },
    )
  }

  LaunchedEffect(dampedDragAnimation) {
    snapshotFlow { fraction }.collectLatest { dampedDragAnimation.updateValue(it) }
  }
  LaunchedEffect(checked) {
    val target = if (checked) 1f else 0f
    if (target != fraction) {
      fraction = target
      dampedDragAnimation.animateToValue(target)
    }
  }

  val trackBackdrop = rememberLayerBackdrop()

  Box(modifier, contentAlignment = Alignment.CenterStart) {
    Box(
      Modifier
        .layerBackdrop(trackBackdrop)
        .clip(Capsule())
        .drawBehind {
          drawRect(lerp(trackColor, accentColor, dampedDragAnimation.value))
        }
        .size(62.dp, 28.dp),
    )

    Box(
      Modifier
        .graphicsLayer {
          val pad = 2.dp.toPx()
          val f = dampedDragAnimation.value
          translationX = if (isLtr) lerp(pad, pad + dragWidth, f) else lerp(-pad, -(pad + dragWidth), f)
        }
        .semantics { role = Role.Switch }
        .then(dampedDragAnimation.modifier)
        .drawBackdrop(
          backdrop = rememberCombinedBackdrop(
            backdrop,
            rememberBackdrop(trackBackdrop) { drawBackdrop ->
              val p = dampedDragAnimation.pressProgress
              scale(lerp(2f / 3f, 0.75f, p), lerp(0f, 0.75f, p)) { drawBackdrop() }
            },
          ),
          shape = { Capsule() },
          effects = {
            val p = dampedDragAnimation.pressProgress
            blur(8.dp.toPx() * (1f - p))
            lens(5.dp.toPx() * p, 10.dp.toPx() * p, chromaticAberration = true)
          },
          highlight = {
            val p = dampedDragAnimation.pressProgress
            Highlight.Ambient.copy(
              width = Highlight.Ambient.width / 1.5f,
              blurRadius = Highlight.Ambient.blurRadius / 1.5f,
              alpha = p,
            )
          },
          shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.08f)) },
          innerShadow = {
            val p = dampedDragAnimation.pressProgress
            InnerShadow(radius = 4.dp * p, alpha = p)
          },
          layerBlock = {
            scaleX = dampedDragAnimation.scaleX
            scaleY = dampedDragAnimation.scaleY
            val v = dampedDragAnimation.velocity / 50f
            scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
            scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
          },
          onDrawSurface = {
            val p = dampedDragAnimation.pressProgress
            drawRect(Color.White.copy(alpha = 1f - p))
          },
        )
        .size(38.dp, 24.dp),
    )
  }
}

/**
 * Full Kyant0-style Liquid Glass Floating Bottom Navigation Bar with
 * draggable refractive lens indicator, chromatic aberration, and spring physics.
 */
@Composable
fun LiquidGlassBottomTabs(
  selectedTabIndex: () -> Int,
  onTabSelected: (Int) -> Unit,
  tabsCount: Int,
  modifier: Modifier = Modifier,
  backdrop: Backdrop = LocalLiquidBackdrop.current,
  content: @Composable RowScope.() -> Unit,
) {
  val isDark = LocalLiquidDark.current
  val accentColor = MaterialTheme.colorScheme.primary
  val containerColor = if (isDark) Color(0xFF101920).copy(0.46f) else Color(0xFFFAFAFA).copy(0.44f)
  val tabsBackdrop = rememberLayerBackdrop()

  BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
    val density = LocalDensity.current
    val tabWidth = with(density) {
      ((constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount).coerceAtLeast(1f)
    }
    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density, constraints.maxWidth) {
      derivedStateOf {
        val maxW = constraints.maxWidth.coerceAtLeast(1).toFloat()
        val fraction = (offsetAnimation.value / maxW).fastCoerceIn(-1f, 1f)
        with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
      }
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    var currentIndex by remember(selectedTabIndex) { mutableIntStateOf(selectedTabIndex()) }
    val dampedDragAnimation = remember(animationScope, tabsCount) {
      DampedDragAnimation(
        animationScope = animationScope,
        initialValue = selectedTabIndex().toFloat(),
        valueRange = 0f..(tabsCount - 1).toFloat(),
        visibilityThreshold = 0.001f,
        initialScale = 1f,
        pressedScale = 76f / 56f,
        onDragStarted = {},
        onDragStopped = {
          val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
          currentIndex = targetIndex
          animateToValue(targetIndex.toFloat())
          animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
        },
        onDrag = { _, dragAmount ->
          updateValue(
            (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
              .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
          )
          animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
        },
      )
    }

    LaunchedEffect(selectedTabIndex) {
      snapshotFlow { selectedTabIndex() }.collectLatest { idx ->
        if (idx != currentIndex) {
          currentIndex = idx
          dampedDragAnimation.animateToValue(idx.toFloat())
        }
      }
    }
    LaunchedEffect(dampedDragAnimation) {
      snapshotFlow { currentIndex }.drop(1).collectLatest { idx ->
        onTabSelected(idx)
      }
    }

    val interactiveHighlight = remember(animationScope) {
      InteractiveHighlight(
        animationScope = animationScope,
        position = { size, _ ->
          Offset(
            if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
            else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
            size.height / 2f,
          )
        },
      )
    }

    // Main glass bar
    Row(
      Modifier
        .graphicsLayer { translationX = panelOffset }
        .drawBackdrop(
          backdrop = backdrop,
          shape = { Capsule() },
          effects = {
            vibrancy()
            blur(10.dp.toPx())
            lens(24.dp.toPx(), 24.dp.toPx())
          },
          highlight = { Highlight.Default.copy(alpha = if (isDark) 0.6f else 0.9f) },
          shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.12f)) },
          layerBlock = {
            val p = dampedDragAnimation.pressProgress
            if (size.width > 0f) {
              val s = lerp(1f, 1f + 14.dp.toPx() / size.width, p)
              scaleX = s
              scaleY = s
            }
          },
          onDrawSurface = { drawRect(containerColor) },
        )
        .then(interactiveHighlight.modifier)
        .height(64.dp)
        .fillMaxWidth()
        .padding(4.dp),
      verticalAlignment = Alignment.CenterVertically,
      content = content,
    )

    // Accent-tinted hidden layer sampled by the sliding lens thumb
    CompositionLocalProvider(
      LocalLiquidBottomTabScale provides { lerp(1f, 1.18f, dampedDragAnimation.pressProgress) },
    ) {
      Row(
        Modifier
          .clearAndSetSemantics {}
          .alpha(0f)
          .layerBackdrop(tabsBackdrop)
          .graphicsLayer { translationX = panelOffset }
          .drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = {
              val p = dampedDragAnimation.pressProgress
              vibrancy()
              blur(8.dp.toPx())
              lens(24.dp.toPx() * p, 24.dp.toPx() * p)
            },
            highlight = { Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress) },
            onDrawSurface = { drawRect(containerColor) },
          )
          .then(interactiveHighlight.modifier)
          .height(56.dp)
          .fillMaxWidth()
          .padding(horizontal = 4.dp)
          .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
      )
    }

    // Sliding refractive glass lens pill
    Box(
      Modifier
        .padding(horizontal = 4.dp)
        .graphicsLayer {
          translationX = if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
          else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
        }
        .then(interactiveHighlight.gestureModifier)
        .then(dampedDragAnimation.modifier)
        .drawBackdrop(
          backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
          shape = { Capsule() },
          effects = {
            val p = dampedDragAnimation.pressProgress
            lens(10.dp.toPx() * p, 14.dp.toPx() * p, chromaticAberration = true)
          },
          highlight = { Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress) },
          shadow = { Shadow(alpha = dampedDragAnimation.pressProgress) },
          innerShadow = {
            val p = dampedDragAnimation.pressProgress
            InnerShadow(radius = 8.dp * p, alpha = p)
          },
          layerBlock = {
            scaleX = dampedDragAnimation.scaleX
            scaleY = dampedDragAnimation.scaleY
            val v = dampedDragAnimation.velocity / 10f
            scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
            scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
          },
          onDrawSurface = {
            val p = dampedDragAnimation.pressProgress
            drawRect(
              if (isDark) Color.White.copy(0.14f) else Color.Black.copy(0.08f),
              alpha = 1f - p,
            )
            drawRect(Color.Black.copy(alpha = 0.03f * p))
          },
        )
        .height(56.dp)
        .fillMaxWidth(1f / tabsCount),
    )
  }
}

@Composable
fun RowScope.LiquidGlassBottomTab(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val scaleProvider = LocalLiquidBottomTabScale.current
  Column(
    modifier = modifier
      .clip(Capsule())
      .clickable(
        interactionSource = null,
        indication = null,
        role = Role.Tab,
        onClick = onClick,
      )
      .fillMaxHeight()
      .weight(1f)
      .graphicsLayer {
        val s = scaleProvider()
        scaleX = s
        scaleY = s
      },
    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
    content = content,
  )
}

/** Liquid Glass Capsule Search Input Field */
@Composable
fun LiquidGlassTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  backdrop: Backdrop = LocalLiquidBackdrop.current,
  singleLine: Boolean = true,
  minLines: Int = 1,
  maxLines: Int = if (singleLine) 1 else 5,
  leading: (@Composable () -> Unit)? = null,
  trailing: (@Composable () -> Unit)? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  shape: Shape = Capsule(),
) {
  val isDark = LocalLiquidDark.current
  val surface = if (isDark) Color(0xFF141E24).copy(alpha = 0.56f) else Color.White.copy(alpha = 0.62f)
  val border = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.80f)
  val radius = if (singleLine) 28.dp else 20.dp

  Row(
    modifier = modifier
      .drawBackdrop(
        backdrop = backdrop,
        shape = { if (singleLine) Capsule() else RoundedRectangle(radius) },
        effects = {
          vibrancy()
          blur(10.dp.toPx())
          lens(10.dp.toPx(), 18.dp.toPx())
        },
        highlight = { Highlight.Default.copy(alpha = if (isDark) 0.5f else 0.85f) },
        onDrawSurface = { drawRect(surface) },
      )
      .border(0.75.dp, border, shape)
      .padding(horizontal = 14.dp, vertical = if (singleLine) 10.dp else 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (leading != null) leading()
    Box(modifier = Modifier.weight(1f)) {
      if (value.isEmpty()) {
        Text(
          text = placeholder,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
        )
      }
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = TextStyle(
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 15.sp,
          lineHeight = 21.sp,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    if (trailing != null) trailing()
  }
}

/** Smooth Liquid Glass Progress Bar (stable replacement for alpha LinearWavyProgressIndicator) */
@Composable
fun LiquidGlassProgressBar(
  progress: Float?,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalLiquidDark.current
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.tertiary
  val track = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)

  if (progress != null) {
    Box(
      modifier = modifier
        .height(7.dp)
        .clip(CircleShape)
        .background(track),
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(progress.coerceIn(0.02f, 1f))
          .clip(CircleShape)
          .background(Brush.horizontalGradient(listOf(primary, secondary))),
      )
    }
  } else {
    val transition = rememberInfiniteTransition(label = "liquid-indeterminate")
    val shift by transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Reverse),
      label = "shift",
    )
    Box(
      modifier = modifier
        .height(7.dp)
        .clip(CircleShape)
        .background(track),
    ) {
      Row(Modifier.fillMaxSize()) {
        if (shift > 0.01f) Spacer(Modifier.weight(shift.coerceAtLeast(0.01f)))
        Box(
          Modifier
            .fillMaxHeight()
            .weight(0.38f)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(primary, secondary))),
        )
        val tail = (1f - shift).coerceAtLeast(0.01f)
        Spacer(Modifier.weight(tail))
      }
    }
  }
}

/** Compact luminous glass loading spinner (stable replacement for alpha LoadingIndicator) */
@Composable
fun LiquidGlassSpinner(
  modifier: Modifier = Modifier,
  size: Dp = 36.dp,
) {
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val transition = rememberInfiniteTransition(label = "liquid-spin")
  val angle by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing)),
    label = "angle",
  )
  Canvas(
    modifier = modifier
      .size(size)
      .graphicsLayer { rotationZ = angle },
  ) {
    drawArc(
      brush = Brush.sweepGradient(listOf(Color.Transparent, primary, tertiary)),
      startAngle = 0f,
      sweepAngle = 285f,
      useCenter = false,
      style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = 3.5.dp.toPx(),
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
      ),
    )
  }
}

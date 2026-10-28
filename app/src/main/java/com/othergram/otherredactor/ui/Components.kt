package com.othergram.otherredactor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.othergram.otherredactor.LocalAnimations
import com.othergram.otherredactor.tr
import kotlinx.coroutines.delay

/** A shape that is a frozen frame of a [Morph] between two Material shapes. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress).asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

val LogoShapes: List<RoundedPolygon>
    get() = listOf(
        MaterialShapes.Cookie9Sided, MaterialShapes.Clover8Leaf, MaterialShapes.SoftBurst,
        MaterialShapes.Cookie6Sided, MaterialShapes.Flower, MaterialShapes.Sunny,
    )

/** Endless morph through [polygons]; each shape holds, then springs into the next. */
@Composable
fun rememberMorphingShape(polygons: List<RoundedPolygon> = LogoShapes, stepMs: Int = 2600): Shape {
    val animate = LocalAnimations.current
    val morphs = remember(polygons) {
        polygons.indices.map { Morph(polygons[it], polygons[(it + 1) % polygons.size]) }
    }
    if (!animate) return polygons.first().toShape()
    val transition = rememberInfiniteTransition(label = "morph")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = polygons.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(stepMs * polygons.size, easing = LinearEasing)),
        label = "phase",
    )
    val idx = phase.toInt().coerceIn(0, polygons.size - 1)
    val local = phase - idx
    // Hold for the first 55% of each step, then morph with an overshooting ease.
    val t = ((local - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val eased = 1f - (1f - t) * (1f - t) * (1f - t)
    return MorphShape(morphs[idx], eased)
}

@Composable
fun rememberSpin(periodMs: Int = 18000): Float {
    if (!LocalAnimations.current) return 0f
    val transition = rememberInfiniteTransition(label = "spin")
    val r by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "r",
    )
    return r
}

/** Staggered entrance: fade + rise + slight scale, keyed by list position. */
@Composable
fun Modifier.staggered(index: Int, distance: Dp = 40.dp): Modifier {
    val animate = LocalAnimations.current
    val p = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate && p.value < 1f) {
            delay(index.coerceAtMost(12) * 45L)
            p.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
        }
    }
    return this.graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * distance.toPx()
        val s = 0.92f + 0.08f * v
        scaleX = s; scaleY = s
    }
}

@Composable
fun ShapeIcon(
    icon: ImageVector,
    polygon: RoundedPolygon,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    rotation: Float = 0f,
) {
    val shape = polygon.toShape()
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Surface(
            shape = shape,
            color = container,
            modifier = Modifier.size(size).graphicsLayer { rotationZ = rotation },
        ) {}
        Icon(icon, null, tint = content, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    val shape = rememberMorphingShape()
    val spin = rememberSpin()
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(120.dp).graphicsLayer { rotationZ = spin },
            ) {}
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        actions()
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    label: String = tr("Имя", "Name"),
    numeric: Boolean = false,
    extra: (@Composable () -> Unit)? = null,
    confirmText: String = tr("Готово", "Done"),
) {
    var value by remember {
        val dot = initial.lastIndexOf('.').takeIf { it > 0 } ?: initial.length
        mutableStateOf(TextFieldValue(initial, TextRange(0, dot)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(value.text) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                extra?.invoke()
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена", "Cancel")) } },
    )
}

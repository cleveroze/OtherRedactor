package com.othergram.otherredactor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.othergram.otherredactor.AppSettings
import com.othergram.otherredactor.CodeLang
import com.othergram.otherredactor.SyntaxTransformation

private fun lineIndexOf(starts: List<Int>, offset: Int): Int {
    var lo = 0; var hi = starts.size - 1
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        if (starts[mid] <= offset) lo = mid else hi = mid - 1
    }
    return lo
}

@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    lang: CodeLang,
    settings: AppSettings,
    query: String,
    scrollRequest: Int,
    onKey: (KeyEvent) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val syntax = LocalSyntaxColors.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textStyle = remember(settings.fontSize, scheme.onSurface) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * 1.55f).sp,
            color = scheme.onSurface,
        )
    }
    val gutterStyle = remember(textStyle, scheme.outline) { textStyle.copy(color = scheme.outline.copy(alpha = 0.8f)) }
    val gutterActive = remember(gutterStyle, scheme.primary) { gutterStyle.copy(color = scheme.primary, fontWeight = FontWeight.Bold) }
    val transformation = remember(lang, syntax, settings.syntax, query) {
        SyntaxTransformation(lang, syntax, settings.syntax, query)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val measurer = rememberTextMeasurer()
    val text = value.text
    val lineStarts = remember(text) {
        val list = ArrayList<Int>(text.length / 30 + 1)
        list.add(0)
        for (i in text.indices) if (text[i] == '\n') list.add(i + 1)
        list
    }
    val digits = lineStarts.size.toString().length.coerceAtLeast(2)
    val charW = remember(gutterStyle) { measurer.measure("0", gutterStyle).size.width }
    val gutterPx = if (settings.lineNumbers) charW * digits + with(density) { 22.dp.roundToPx() } else 0
    val padTop = 12.dp
    val padBottom = 160.dp
    val padTopPx = with(density) { padTop.toPx() }
    val cursor = value.selection.start
    val cursorLine = lineIndexOf(lineStarts, cursor.coerceIn(0, text.length))

    LaunchedEffect(scrollRequest) {
        if (scrollRequest == 0) return@LaunchedEffect
        val l = layout ?: return@LaunchedEffect
        val off = value.selection.start.coerceIn(0, l.layoutInput.text.length)
        val rect = l.getCursorRect(off)
        val target = (rect.top + padTopPx - vScroll.viewportSize / 3f).toInt().coerceAtLeast(0)
        vScroll.animateScrollTo(target)
        if (!settings.wordWrap) hScroll.animateScrollTo((rect.left - 160f).toInt().coerceAtLeast(0))
    }

    BoxWithConstraints(modifier.background(scheme.surface)) {
        val viewportW = constraints.maxWidth
        Row(
            Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
                .drawBehind {
                    if (!settings.highlightLine) return@drawBehind
                    val l = layout ?: return@drawBehind
                    val off = cursor.coerceIn(0, l.layoutInput.text.length)
                    val line = l.getLineForOffset(off)
                    val top = l.getLineTop(line) + padTopPx
                    val bottom = l.getLineBottom(line) + padTopPx
                    drawRect(syntax.currentLine, Offset(0f, top), Size(size.width, bottom - top))
                },
        ) {
            if (settings.lineNumbers) {
                val contentH = (layout?.size?.height ?: 0) + padTopPx + with(density) { padBottom.toPx() }
                Canvas(
                    Modifier
                        .width(with(density) { gutterPx.toDp() })
                        .height(with(density) { contentH.toDp() })
                        .background(scheme.surfaceContainerLow),
                ) {
                    val l = layout ?: return@Canvas
                    val len = l.layoutInput.text.length
                    val visTop = vScroll.value - 200f
                    val visBottom = vScroll.value + vScroll.viewportSize + 200f
                    // Binary search the first logical line that is visible.
                    val firstVisual = l.getLineForVerticalPosition((visTop - padTopPx).coerceAtLeast(0f))
                    val firstOffset = l.getLineStart(firstVisual)
                    var i = lineIndexOf(lineStarts, firstOffset)
                    val right = size.width - 10.dp.toPx()
                    while (i < lineStarts.size) {
                        val start = lineStarts[i]
                        if (start > len) break
                        val top = l.getLineTop(l.getLineForOffset(start)) + padTopPx
                        if (top > visBottom) break
                        val res = measurer.measure((i + 1).toString(), if (i == cursorLine) gutterActive else gutterStyle)
                        drawText(res, topLeft = Offset(right - res.size.width, top))
                        i++
                    }
                }
            }
            val fieldBox = if (settings.wordWrap) Modifier.weight(1f) else Modifier.weight(1f).horizontalScroll(hScroll)
            Box(fieldBox) {
                val minW = with(density) { (viewportW - gutterPx).coerceAtLeast(0).toDp() }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle,
                    visualTransformation = transformation,
                    onTextLayout = { layout = it },
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                    ),
                    modifier = (if (settings.wordWrap) Modifier.fillMaxWidth() else Modifier.widthIn(min = minW))
                        .padding(start = 10.dp, end = 24.dp, top = padTop, bottom = padBottom)
                        .onPreviewKeyEvent(onKey),
                )
            }
        }
    }
}

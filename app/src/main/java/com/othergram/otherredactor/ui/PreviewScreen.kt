package com.othergram.otherredactor.ui

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.TabletMac
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.othergram.otherredactor.AppViewModel
import com.othergram.otherredactor.AppViewModel.PreviewDoc
import com.othergram.otherredactor.EditorTab
import com.othergram.otherredactor.Lang
import com.othergram.otherredactor.LocalLang
import com.othergram.otherredactor.tr
import kotlin.math.min
import kotlin.math.roundToInt

data class Device(
    val id: String,
    val ru: String,
    val en: String,
    val w: Int,
    val h: Int,
    val icon: ImageVector,
    val desktop: Boolean = false,
)

val Devices = listOf(
    Device("this", "Это устройство", "This device", 0, 0, Icons.Rounded.Smartphone),
    Device("se", "iPhone SE", "iPhone SE", 375, 667, Icons.Rounded.PhoneIphone),
    Device("iphone", "iPhone 15 Pro", "iPhone 15 Pro", 393, 852, Icons.Rounded.PhoneIphone),
    Device("pixel", "Pixel 9", "Pixel 9", 412, 915, Icons.Rounded.PhoneAndroid),
    Device("galaxy", "Galaxy S24", "Galaxy S24", 360, 780, Icons.Rounded.PhoneAndroid),
    Device("fold", "Складной (открыт)", "Foldable (open)", 673, 841, Icons.Rounded.TabletAndroid),
    Device("ipadmini", "iPad mini", "iPad mini", 744, 1133, Icons.Rounded.TabletMac),
    Device("ipad", "iPad Pro 12.9″", "iPad Pro 12.9″", 1024, 1366, Icons.Rounded.TabletMac),
    Device("laptop", "Ноутбук", "Laptop", 1366, 768, Icons.Rounded.Laptop, desktop = true),
    Device("desktop", "Монитор Full HD", "Full HD monitor", 1920, 1080, Icons.Rounded.DesktopWindows, desktop = true),
    Device("custom", "Своё разрешение", "Custom size", 0, 0, Icons.Rounded.AspectRatio),
)

private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

data class LogLine(val level: ConsoleMessage.MessageLevel, val text: String, val line: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewScreen(vm: AppViewModel, tab: EditorTab, onClose: () -> Unit) {
    val s = vm.settings
    val lang = LocalLang.current
    var deviceId by remember { mutableStateOf(s.defaultDevice) }
    var landscape by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var progress by remember { mutableIntStateOf(0) }
    var consoleOpen by remember { mutableStateOf(false) }
    var customDialog by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<LogLine>() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val doc = remember(reloadKey, tab.id) { vm.previewDoc(tab) }

    val device = Devices.firstOrNull { it.id == deviceId } ?: Devices.first()
    val baseW = if (device.id == "custom") s.customWidth else device.w
    val baseH = if (device.id == "custom") s.customHeight else device.h
    val w = if (landscape) baseH else baseW
    val h = if (landscape) baseW else baseH

    BackHandler { if (consoleOpen) consoleOpen = false else onClose() }
    LaunchedEffect(reloadKey, deviceId, landscape) { logs.clear() }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад", "Back")) }
                },
                title = {
                    Column {
                        Text(tr("Предпросмотр", "Preview"), fontWeight = FontWeight.Bold)
                        Text(
                            (if (lang == Lang.RU) device.ru else device.en) + if (device.id != "this") " · $w × $h" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { landscape = !landscape },
                        enabled = device.id != "this",
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        val rot by animateFloatAsState(if (landscape) 90f else 0f, spring(0.6f, Spring.StiffnessLow), label = "rot")
                        Icon(Icons.Rounded.ScreenRotation, tr("Повернуть", "Rotate"), Modifier.graphicsLayer { rotationZ = rot })
                    }
                    IconButton(onClick = { reloadKey++ }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Refresh, tr("Обновить", "Reload"))
                    }
                    if (s.showConsole) IconButton(onClick = { consoleOpen = !consoleOpen }, shapes = IconButtonDefaults.shapes()) {
                        val errors = logs.count { it.level == ConsoleMessage.MessageLevel.ERROR }
                        BadgedBox(badge = {
                            if (logs.isNotEmpty()) Badge(
                                containerColor = if (errors > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            ) { Text("${logs.size}") }
                        }) { Icon(Icons.Rounded.Terminal, tr("Консоль", "Console")) }
                    }
                },
            )

            val listState = rememberLazyListState()
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Devices, key = { it.id }) { d ->
                    FilterChip(
                        selected = d.id == deviceId,
                        onClick = { if (d.id == "custom") customDialog = true else deviceId = d.id },
                        label = {
                            Text(
                                (if (lang == Lang.RU) d.ru else d.en) +
                                    if (d.id == "custom" && deviceId == "custom") " ${s.customWidth}×${s.customHeight}" else "",
                            )
                        },
                        leadingIcon = { Icon(d.icon, null, Modifier.size(FilterChipDefaults.IconSize)) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            LaunchedEffect(Unit) {
                val i = Devices.indexOfFirst { it.id == deviceId }
                if (i > 0) listState.scrollToItem(i)
            }

            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                key(deviceId, landscape) {
                    if (device.id == "this") {
                        WebHost(
                            doc, reloadKey, s.javaScript, desktop = false,
                            onLog = { logs.add(it) }, onProgress = { progress = it }, onCreated = { webView = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        DeviceFrame(w, h) {
                            WebHost(
                                doc, reloadKey, s.javaScript, desktop = device.desktop,
                                onLog = { logs.add(it) }, onProgress = { progress = it }, onCreated = { webView = it },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                val loading by animateFloatAsState(if (progress in 0..99) 1f else 0f, spring(0.6f, Spring.StiffnessMediumLow), label = "loading")
                if (loading > 0.01f) {
                    LoadingIndicator(
                        Modifier.align(Alignment.Center).size(72.dp)
                            .graphicsLayer { alpha = loading.coerceIn(0f, 1f); scaleX = loading; scaleY = loading },
                    )
                }
            }

            AnimatedVisibility(
                visible = consoleOpen && s.showConsole,
                enter = expandVertically(spring(0.8f, Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ConsolePanel(
                    logs = logs,
                    onClear = { logs.clear() },
                    onClose = { consoleOpen = false },
                    onEval = { code ->
                        logs.add(LogLine(ConsoleMessage.MessageLevel.DEBUG, "› $code", 0))
                        webView?.evaluateJavascript(code) { result ->
                            logs.add(LogLine(ConsoleMessage.MessageLevel.LOG, "← $result", 0))
                        }
                    },
                )
            }
        }
    }

    if (customDialog) {
        CustomSizeDialog(
            initialW = s.customWidth, initialH = s.customHeight,
            onDismiss = { customDialog = false },
            onConfirm = { cw, ch ->
                vm.update { it.copy(customWidth = cw, customHeight = ch) }
                deviceId = "custom"; landscape = false; customDialog = false
            },
        )
    }
}

/** Renders [content] at exactly w×h dp, then scales it down to fit, inside a device bezel. */
@Composable
private fun DeviceFrame(w: Int, h: Int, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val bezel = 10f
        val availW = maxWidth.value - bezel * 2
        val availH = maxHeight.value - bezel * 2 - 28f
        val scale = min(1f, min(availW / w, availH / h)).coerceAtLeast(0.05f)
        val animScale by animateFloatAsState(scale, spring(0.8f, Spring.StiffnessMediumLow), label = "scale")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size((w * animScale + bezel * 2).dp, (h * animScale + bezel * 2).dp)
                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape((28 * animScale + bezel).dp))
                    .padding(bezel.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .requiredSize(w.dp, h.dp)
                        .graphicsLayer { scaleX = animScale; scaleY = animScale }
                        .clip(RoundedCornerShape(22.dp)),
                ) { content() }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "$w × $h · ${(animScale * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebHost(
    doc: PreviewDoc,
    reloadKey: Int,
    js: Boolean,
    desktop: Boolean,
    onLog: (LogLine) -> Unit,
    onProgress: (Int) -> Unit,
    onCreated: (WebView) -> Unit,
    modifier: Modifier,
) {
    val log by rememberUpdatedState(onLog)
    val prog by rememberUpdatedState(onProgress)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    domStorageEnabled = true
                    allowFileAccess = true
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                    javaScriptCanOpenWindowsAutomatically = true
                }
                setBackgroundColor(android.graphics.Color.WHITE)
                webViewClient = object : WebViewClient() {}
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                        log(LogLine(m.messageLevel(), m.message(), m.lineNumber()))
                        return true
                    }

                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        prog(newProgress)
                    }
                }
                onCreated(this)
            }
        },
        update = { wv ->
            val tag = "$reloadKey|$js|$desktop|${doc.html.hashCode()}"
            if (wv.tag != tag) {
                wv.tag = tag
                wv.settings.javaScriptEnabled = js
                wv.settings.userAgentString = if (desktop) DESKTOP_UA else null
                wv.loadDataWithBaseURL(doc.baseUrl, doc.html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
private fun ConsolePanel(
    logs: List<LogLine>,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onEval: (String) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    var input by remember { mutableStateOf("") }
    val state = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) state.animateScrollToItem(logs.size - 1) }
    Surface(
        color = c.surfaceContainerHighest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth().height(280.dp),
    ) {
        Column {
            Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Terminal, null, tint = c.primary)
                Spacer(Modifier.width(10.dp))
                Text(tr("Консоль", "Console"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteSweep, tr("Очистить", "Clear")) }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null) }
            }
            LazyColumn(state = state, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                if (logs.isEmpty()) item {
                    Text(
                        tr("Здесь появится вывод console.log и ошибки", "console.log output and errors appear here"),
                        color = c.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(logs.size) { i ->
                    val l = logs[i]
                    val color = when (l.level) {
                        ConsoleMessage.MessageLevel.ERROR -> c.error
                        ConsoleMessage.MessageLevel.WARNING -> c.tertiary
                        ConsoleMessage.MessageLevel.DEBUG -> c.primary
                        else -> c.onSurface
                    }
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(
                            l.text, color = color, fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                        )
                        if (l.line > 0) Text(":${l.line}", color = c.outline, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it }, singleLine = true,
                    placeholder = { Text(tr("Выполнить JavaScript…", "Run JavaScript…"), fontFamily = FontFamily.Monospace) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onSend = { if (input.isNotBlank()) { onEval(input); input = "" } }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { if (input.isNotBlank()) { onEval(input); input = "" } }) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = c.primary)
                }
            }
        }
    }
}

@Composable
private fun CustomSizeDialog(initialW: Int, initialH: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var w by remember { mutableStateOf(initialW.toString()) }
    var h by remember { mutableStateOf(initialH.toString()) }
    val valid = (w.toIntOrNull() ?: 0) in 100..4000 && (h.toIntOrNull() ?: 0) in 100..4000
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.AspectRatio, null) },
        title = { Text(tr("Своё разрешение", "Custom size")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    tr("Ширина и высота в CSS-пикселях (100–4000)", "Width and height in CSS pixels (100–4000)"),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = w, onValueChange = { w = it.filter(Char::isDigit).take(4) }, singleLine = true,
                        label = { Text(tr("Ширина", "Width")) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Text("×")
                    OutlinedTextField(
                        value = h, onValueChange = { h = it.filter(Char::isDigit).take(4) }, singleLine = true,
                        label = { Text(tr("Высота", "Height")) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = valid, onClick = { onConfirm(w.toInt(), h.toInt()) }) {
                Text(tr("Применить", "Apply"))
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Отмена", "Cancel")) } },
    )
}

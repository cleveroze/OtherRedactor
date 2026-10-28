package com.othergram.otherredactor.ui

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.othergram.otherredactor.AppSettings
import com.othergram.otherredactor.AppViewModel
import com.othergram.otherredactor.Lang
import com.othergram.otherredactor.LocalLang
import com.othergram.otherredactor.Palette
import com.othergram.otherredactor.R
import com.othergram.otherredactor.ThemeMode
import com.othergram.otherredactor.tr

const val TELEGRAM_URL = "https://t.me/OtherGramWorks"
const val GITHUB_URL = "https://github.com/cleveroze/OtherRedactor"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s = vm.settings
    val set: ((AppSettings) -> AppSettings) -> Unit = { vm.update(it) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var deviceDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text(tr("Настройки", "Settings"), fontWeight = FontWeight.Black) },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item { SectionTitle(tr("Внешний вид", "Appearance")) }
            item {
                Group {
                    Item(Icons.Rounded.Language, tr("Язык", "Language")) {
                        Segmented(
                            listOf("Русский", "English"), if (s.lang == Lang.RU) 0 else 1,
                        ) { i -> set { it.copy(lang = if (i == 0) Lang.RU else Lang.EN) } }
                    }
                    Item(Icons.Rounded.Brightness4, tr("Тема", "Theme")) {
                        Segmented(
                            listOf(tr("Система", "System"), tr("Светлая", "Light"), tr("Тёмная", "Dark")),
                            s.themeMode.ordinal,
                            icons = listOf(Icons.Rounded.SettingsBrightness, Icons.Rounded.LightMode, Icons.Rounded.DarkMode),
                        ) { i -> set { it.copy(themeMode = ThemeMode.entries[i]) } }
                    }
                    Item(Icons.Rounded.Palette, tr("Цвет приложения", "App colour")) {
                        PalettePicker(s.palette, vm) { p -> set { it.copy(palette = p) } }
                    }
                    SwitchRow(Icons.Rounded.Contrast, tr("Чёрный AMOLED", "Pure black (AMOLED)"), tr("Для тёмной темы", "For dark theme"), s.amoled) { v -> set { it.copy(amoled = v) } }
                    SwitchRow(Icons.Rounded.Animation, tr("Анимации", "Animations"), tr("Выразительные пружинные анимации", "Expressive spring motion"), s.animations) { v -> set { it.copy(animations = v) } }
                }
            }

            item { SectionTitle(tr("Редактор", "Editor")) }
            item {
                Group {
                    Item(Icons.Rounded.TextFields, tr("Размер шрифта", "Font size"), "${s.fontSize} sp") {
                        Slider(
                            value = s.fontSize.toFloat(),
                            onValueChange = { v -> set { it.copy(fontSize = v.toInt()) } },
                            valueRange = 9f..28f,
                            steps = 18,
                        )
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                            Text(
                                "<p class=\"hi\">Hello</p>",
                                fontFamily = FontFamily.Monospace, fontSize = s.fontSize.sp,
                                modifier = Modifier.fillMaxWidth().padding(12.dp).animateContentSize(),
                            )
                        }
                    }
                    Item(Icons.Rounded.Tab, tr("Размер табуляции", "Tab size")) {
                        Segmented(listOf("2", "4", "8"), listOf(2, 4, 8).indexOf(s.tabSize).coerceAtLeast(0)) { i ->
                            set { it.copy(tabSize = listOf(2, 4, 8)[i]) }
                        }
                    }
                    SwitchRow(Icons.Rounded.SpaceBar, tr("Пробелы вместо табов", "Insert spaces for tabs"), null, s.useSpaces) { v -> set { it.copy(useSpaces = v) } }
                    SwitchRow(Icons.Rounded.FormatListNumbered, tr("Номера строк", "Line numbers"), null, s.lineNumbers) { v -> set { it.copy(lineNumbers = v) } }
                    SwitchRow(Icons.AutoMirrored.Rounded.WrapText, tr("Перенос строк", "Word wrap"), null, s.wordWrap) { v -> set { it.copy(wordWrap = v) } }
                    SwitchRow(Icons.Rounded.Code, tr("Подсветка синтаксиса", "Syntax highlighting"), "HTML · CSS · JS", s.syntax) { v -> set { it.copy(syntax = v) } }
                    SwitchRow(Icons.Rounded.Highlight, tr("Подсветка текущей строки", "Highlight current line"), null, s.highlightLine) { v -> set { it.copy(highlightLine = v) } }
                    SwitchRow(Icons.Rounded.DataObject, tr("Автозакрытие скобок и кавычек", "Auto-close brackets & quotes"), null, s.autoCloseBrackets) { v -> set { it.copy(autoCloseBrackets = v) } }
                    SwitchRow(Icons.Rounded.Code, tr("Автозакрытие тегов", "Auto-close tags"), "<div>|</div>", s.autoCloseTags) { v -> set { it.copy(autoCloseTags = v) } }
                    SwitchRow(Icons.AutoMirrored.Rounded.FormatIndentIncrease, tr("Автоотступ", "Auto-indent"), tr("Сохранять отступ на новой строке", "Keep indentation on new lines"), s.autoIndent) { v -> set { it.copy(autoIndent = v) } }
                    SwitchRow(Icons.Rounded.Keyboard, tr("Панель символов", "Symbol bar"), tr("< > { } и сниппеты над клавиатурой", "< > { } and snippets above keyboard"), s.symbolBar) { v -> set { it.copy(symbolBar = v) } }
                    SwitchRow(Icons.Rounded.Save, tr("Автосохранение", "Auto-save"), tr("Через 1,5 с после ввода", "1.5 s after you stop typing"), s.autoSave) { v -> set { it.copy(autoSave = v) } }
                    SwitchRow(Icons.Rounded.Warning, tr("Спрашивать при закрытии", "Confirm closing unsaved tabs"), null, s.confirmClose) { v -> set { it.copy(confirmClose = v) } }
                    SwitchRow(Icons.Rounded.Restore, tr("Восстанавливать вкладки", "Restore tabs on launch"), null, s.restoreTabs) { v -> set { it.copy(restoreTabs = v) } }
                    SwitchRow(Icons.Rounded.ScreenLockPortrait, tr("Не гасить экран", "Keep screen on"), tr("Пока открыт редактор", "While the editor is open"), s.keepScreenOn) { v -> set { it.copy(keepScreenOn = v) } }
                }
            }

            item { SectionTitle(tr("Предпросмотр", "Preview")) }
            item {
                Group {
                    SwitchRow(Icons.Rounded.Javascript, "JavaScript", tr("Выполнять скрипты на странице", "Run scripts on the page"), s.javaScript) { v -> set { it.copy(javaScript = v) } }
                    SwitchRow(Icons.Rounded.PlayCircle, tr("Сохранять перед запуском", "Save before running"), tr("Чтобы подтянулись связанные CSS/JS", "So linked CSS/JS files are up to date"), s.saveBeforeRun) { v -> set { it.copy(saveBeforeRun = v) } }
                    SwitchRow(Icons.Rounded.Terminal, tr("Консоль", "Console"), tr("console.log и ошибки страницы", "console.log and page errors"), s.showConsole) { v -> set { it.copy(showConsole = v) } }
                    val dev = Devices.firstOrNull { it.id == s.defaultDevice } ?: Devices.first()
                    ClickRow(
                        Icons.Rounded.Devices, tr("Устройство по умолчанию", "Default device"),
                        if (LocalLang.current == Lang.RU) dev.ru else dev.en,
                    ) { deviceDialog = true }
                }
            }

            item { SectionTitle(tr("О приложении", "About")) }
            item { AboutCard(vm) }
        }
    }

    if (deviceDialog) {
        AlertDialog(
            onDismissRequest = { deviceDialog = false },
            title = { Text(tr("Устройство по умолчанию", "Default device")) },
            text = {
                LazyColumn {
                    items(Devices.filter { it.id != "custom" }) { d ->
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                                .selectable(d.id == s.defaultDevice) { set { it.copy(defaultDevice = d.id) }; deviceDialog = false }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = d.id == s.defaultDevice, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Icon(d.icon, null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(if (LocalLang.current == Lang.RU) d.ru else d.en)
                                if (d.w > 0) Text("${d.w} × ${d.h}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { deviceDialog = false }) { Text(tr("Закрыть", "Close")) } },
        )
    }
}

/** Expressive grouped list: big outer radius, tight inner gaps. */
@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(28.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) { content() }
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Box(
        Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun Item(icon: ImageVector, title: String, trailing: String? = null, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(6.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIcon(icon)
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (trailing != null) Text(trailing, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onChange(!checked) },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                thumbContent = if (checked) ({ Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }) else null,
            )
        }
    }
}

@Composable
private fun ClickRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, icons: List<ImageVector>? = null, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = i == selected,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {
                    if (icons != null && i != selected) Icon(icons[i], null, Modifier.size(18.dp))
                    else SegmentedButtonDefaults.Icon(i == selected)
                },
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PalettePicker(current: Palette, vm: AppViewModel, onPick: (Palette) -> Unit) {
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    val dark = LocalIsDark.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Palette.entries) { p ->
            val selected = p == current
            val size by animateDpAsState(if (selected) 56.dp else 46.dp, spring(0.5f, Spring.StiffnessMediumLow), label = "sz")
            val rot by animateFloatAsState(if (selected) 45f else 0f, spring(0.5f, Spring.StiffnessLow), label = "rot")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Box(
                    Modifier
                        .size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val bg = if (p == Palette.DYNAMIC) Brush.sweepGradient(listOf(Color(0xFFE57373), Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFBA68C8), Color(0xFFE57373)))
                    else Brush.linearGradient(listOf(p.seed(), if (dark) p.seed().copy(alpha = 0.6f) else LogoCream))
                    Box(
                        Modifier
                            .size(size)
                            .graphicsLayer { rotationZ = rot }
                            .clip(if (selected) cookie else CircleShape)
                            .background(bg)
                            .clickable {
                                if (p == Palette.DYNAMIC && Build.VERSION.SDK_INT < 31) vm.toast("Нужен Android 12+", "Requires Android 12+")
                                else onPick(p)
                            },
                    )
                    if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White)
                    else if (p == Palette.DYNAMIC) Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    when (p) {
                        Palette.VANILLA -> tr("Ваниль", "Vanilla")
                        Palette.DYNAMIC -> "You"
                        Palette.MOCHA -> tr("Мокко", "Mocha")
                        Palette.SAGE -> tr("Шалфей", "Sage")
                        Palette.OCEAN -> tr("Океан", "Ocean")
                        Palette.ROSE -> tr("Роза", "Rose")
                        Palette.LAVENDER -> tr("Лаванда", "Lavender")
                    },
                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutCard(vm: AppViewModel) {
    val uri = LocalUriHandler.current
    val c = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = c.surfaceContainer) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                val shape = rememberMorphingShape()
                Image(
                    painterResource(R.drawable.logo), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(shape),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("OtherRedactor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(tr("Версия", "Version") + " 1.0.0", color = c.onSurfaceVariant)
                    Text("by OtherGram Works", style = MaterialTheme.typography.bodySmall, color = c.primary)
                }
            }
        }
        // Telegram invite lives here after onboarding.
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = c.primaryContainer,
            contentColor = c.onPrimaryContainer,
            onClick = { uri.openUri(TELEGRAM_URL) },
        ) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(Icons.AutoMirrored.Rounded.Send, MaterialShapes.SoftBurst, c.primary, c.onPrimary, size = 56.dp, rotation = rememberSpin(16000))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Наш Telegram-канал", "Our Telegram channel"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(tr("Новости и обновления — @OtherGramWorks", "News and updates — @OtherGramWorks"), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { uri.openUri(GITHUB_URL) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Code, null); Spacer(Modifier.width(8.dp)); Text("GitHub")
            }
            Button(
                onClick = { vm.update { it.copy(onboarded = false) } },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.WavingHand, null); Spacer(Modifier.width(8.dp)); Text(tr("Приветствие", "Welcome"), maxLines = 1)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

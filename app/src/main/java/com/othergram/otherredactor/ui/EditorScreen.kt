package com.othergram.otherredactor.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.othergram.otherredactor.AppViewModel
import com.othergram.otherredactor.CodeLang
import com.othergram.otherredactor.EditorLogic
import com.othergram.otherredactor.EditorTab
import com.othergram.otherredactor.Source
import com.othergram.otherredactor.tr
import kotlinx.coroutines.delay

private sealed interface EditorDialog {
    data class SaveAs(val tab: EditorTab, val closeAfter: Boolean = false) : EditorDialog
    data class ConfirmClose(val tab: EditorTab) : EditorDialog
    data object GoToLine : EditorDialog
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditorScreen(vm: AppViewModel) {
    val tab = vm.active
    val s = vm.settings
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.openExternal(it) }
    }

    if (tab == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                Icons.Rounded.Code,
                tr("Нет открытых файлов", "No open files"),
                tr("Создайте новый файл или откройте существующий", "Create a new file or open an existing one"),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.newUntitled() }, shapes = ButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(tr("Новый", "New"))
                    }
                    FilledTonalButton(onClick = { openDoc.launch(arrayOf("text/*", "application/javascript")) }, shapes = ButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text(tr("Открыть", "Open"))
                    }
                }
            }
        }
        return
    }

    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    var menu by remember { mutableStateOf(false) }
    var findOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var scrollRequest by remember { mutableIntStateOf(0) }
    var pendingSaveAs by remember { mutableStateOf<EditorDialog.SaveAs?>(null) }

    val saveAsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        val p = pendingSaveAs
        if (uri != null && p != null) {
            vm.saveAs(p.tab, uri)
            if (p.closeAfter) vm.close(p.tab)
        }
        pendingSaveAs = null
    }

    fun requestSave(t: EditorTab, closeAfter: Boolean = false) {
        if (vm.save(t)) { if (closeAfter) vm.close(t) } else dialog = EditorDialog.SaveAs(t, closeAfter)
    }
    fun requestClose(t: EditorTab) {
        if (t.dirty && s.confirmClose) dialog = EditorDialog.ConfirmClose(t) else vm.close(t)
    }
    fun run() {
        if (s.saveBeforeRun) vm.saveAll()
        vm.previewOpen = true
    }
    fun find(forward: Boolean) {
        if (query.isEmpty()) return
        val text = tab.value.text
        val from = if (forward) tab.value.selection.max else tab.value.selection.min
        val idx = if (forward) {
            text.indexOf(query, from, ignoreCase = true).takeIf { it >= 0 } ?: text.indexOf(query, 0, ignoreCase = true)
        } else {
            text.lastIndexOf(query, (from - 1).coerceAtLeast(0), ignoreCase = true).takeIf { it >= 0 && it < from }
                ?: text.lastIndexOf(query, text.length, ignoreCase = true)
        }
        if (idx < 0) { vm.toast("Не найдено", "Not found"); return }
        tab.value = tab.value.copy(selection = TextRange(idx, idx + query.length))
        scrollRequest++
    }

    // Auto-save shortly after typing stops.
    LaunchedEffect(tab.id, tab.value.text, s.autoSave) {
        if (s.autoSave && tab.dirty && tab.source !is Source.Untitled) {
            delay(1500)
            vm.save(tab, quiet = true)
        }
    }
    val view = LocalView.current
    DisposableEffect(s.keepScreenOn) {
        view.keepScreenOn = s.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    BackHandler(enabled = findOpen && !vm.previewOpen) { findOpen = false; query = "" }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(tab.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                val where = when (tab.source) {
                                    is Source.Local -> tr("Мои файлы", "My files")
                                    is Source.External -> tr("Устройство", "Device")
                                    Source.Untitled -> tr("Не сохранён", "Not saved")
                                }
                                Text(
                                    where + if (tab.dirty) " • " + tr("изменён", "modified") else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (tab.dirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { vm.undo(tab) }, enabled = tab.canUndo, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, tr("Отменить", "Undo"))
                            }
                            IconButton(onClick = { vm.redo(tab) }, enabled = tab.canRedo, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Rounded.Redo, tr("Повторить", "Redo"))
                            }
                            IconButton(onClick = { requestSave(tab) }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Rounded.Save, tr("Сохранить", "Save"))
                            }
                            FilledIconButton(onClick = { run() }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Rounded.PlayArrow, tr("Запустить", "Run"))
                            }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                                EditorMenu(
                                    expanded = menu,
                                    onDismiss = { menu = false },
                                    vm = vm,
                                    tab = tab,
                                    onFind = { findOpen = true },
                                    onSaveAs = { dialog = EditorDialog.SaveAs(tab) },
                                    onGoTo = { dialog = EditorDialog.GoToLine },
                                    onClose = { requestClose(tab) },
                                )
                            }
                        },
                    )
                    TabStrip(vm, onClose = { requestClose(it) })
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
                AnimatedVisibility(
                    visible = findOpen,
                    enter = expandVertically(spring(0.8f, Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    FindBar(
                        query = query, onQuery = { query = it },
                        replacement = replacement, onReplacement = { replacement = it },
                        count = remember(query, tab.value.text) {
                            if (query.isEmpty()) 0 else Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(tab.value.text).count()
                        },
                        onNext = { find(true) }, onPrev = { find(false) },
                        onReplace = {
                            val sel = tab.value.selection
                            val selected = tab.value.text.substring(sel.min, sel.max)
                            if (query.isNotEmpty() && selected.equals(query, ignoreCase = true)) {
                                vm.edit(tab, EditorLogic.insert(tab.value, replacement), smart = false)
                            }
                            find(true)
                        },
                        onReplaceAll = {
                            if (query.isNotEmpty()) {
                                val n = tab.value.text.replace(query, replacement, ignoreCase = true)
                                vm.edit(tab, tab.value.copy(text = n, selection = TextRange(0)), smart = false)
                            }
                        },
                        onClose = { findOpen = false; query = "" },
                    )
                }
                CodeEditor(
                    value = tab.value,
                    onValueChange = { vm.edit(tab, it) },
                    lang = tab.lang,
                    settings = s,
                    query = if (findOpen) query else "",
                    scrollRequest = scrollRequest,
                    onKey = { e ->
                        if (e.type != KeyEventType.KeyDown) false
                        else when {
                            e.key == Key.Tab -> {
                                val v = tab.value
                                val nv = if (e.isShiftPressed || !v.selection.collapsed) EditorLogic.indentSelection(v, s.indentUnit, e.isShiftPressed)
                                else EditorLogic.insert(v, s.indentUnit)
                                vm.edit(tab, nv, smart = false); true
                            }
                            e.isCtrlPressed && e.key == Key.S -> { requestSave(tab); true }
                            e.isCtrlPressed && e.key == Key.Z -> { if (e.isShiftPressed) vm.redo(tab) else vm.undo(tab); true }
                            e.isCtrlPressed && e.key == Key.Y -> { vm.redo(tab); true }
                            e.isCtrlPressed && e.key == Key.F -> { findOpen = true; true }
                            e.isCtrlPressed && e.key == Key.Enter -> { run(); true }
                            e.isCtrlPressed && e.key == Key.Slash -> { vm.edit(tab, EditorLogic.toggleComment(tab.value, tab.lang), smart = false); true }
                            else -> false
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                AnimatedVisibility(
                    visible = s.symbolBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SymbolBar(
                        lang = tab.lang,
                        onInsert = { text, caret -> vm.edit(tab, EditorLogic.insert(tab.value, text, caret), smart = false) },
                        onTab = { vm.edit(tab, EditorLogic.insert(tab.value, s.indentUnit), smart = false) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = vm.previewOpen,
            enter = slideInVertically(spring(0.85f, Spring.StiffnessMediumLow)) { it / 3 } + fadeIn() + scaleIn(initialScale = 0.96f),
            exit = slideOutVertically(spring(1f, Spring.StiffnessMedium)) { it / 3 } + fadeOut() + scaleOut(targetScale = 0.96f),
        ) {
            PreviewScreen(vm, tab, onClose = { vm.previewOpen = false })
        }
    }

    when (val d = dialog) {
        is EditorDialog.SaveAs -> SaveAsDialog(
            name = d.tab.name,
            folderName = if (vm.currentDir == vm.workspace) tr("Мои файлы", "My files") else vm.currentDir.name,
            onDismiss = { dialog = null },
            onWorkspace = { name ->
                vm.saveToWorkspace(d.tab, name)
                if (d.closeAfter) vm.close(d.tab)
                dialog = null
            },
            onDevice = { name ->
                pendingSaveAs = d
                saveAsLauncher.launch(name)
                dialog = null
            },
        )
        is EditorDialog.ConfirmClose -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.Save, null) },
            title = { Text(tr("Сохранить изменения?", "Save changes?")) },
            text = { Text(tr("В «${d.tab.name}» есть несохранённые изменения.", "“${d.tab.name}” has unsaved changes.")) },
            confirmButton = {
                TextButton(onClick = { dialog = null; requestSave(d.tab, closeAfter = true) }) { Text(tr("Сохранить", "Save")) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { dialog = null }) { Text(tr("Отмена", "Cancel")) }
                    TextButton(onClick = { vm.close(d.tab); dialog = null }) {
                        Text(tr("Не сохранять", "Discard"), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
        EditorDialog.GoToLine -> NameDialog(
            title = tr("Перейти к строке", "Go to line"),
            initial = "",
            label = tr("Номер строки", "Line number"),
            numeric = true,
            onDismiss = { dialog = null },
            onConfirm = { input ->
                dialog = null
                val n = input.trim().toIntOrNull() ?: return@NameDialog
                val text = tab.value.text
                var offset = 0
                var line = 1
                while (line < n) {
                    val nl = text.indexOf('\n', offset)
                    if (nl < 0) break
                    offset = nl + 1; line++
                }
                tab.value = tab.value.copy(selection = TextRange(offset))
                scrollRequest++
            },
        )
        null -> {}
    }
}

@Composable
private fun EditorMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    vm: AppViewModel,
    tab: EditorTab,
    onFind: () -> Unit,
    onSaveAs: () -> Unit,
    onGoTo: () -> Unit,
    onClose: () -> Unit,
) {
    val s = vm.settings
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, shape = MaterialTheme.shapes.large) {
        DropdownMenuItem(
            text = { Text(tr("Найти и заменить", "Find & replace")) },
            leadingIcon = { Icon(Icons.Rounded.FindReplace, null) },
            onClick = { onDismiss(); onFind() },
        )
        DropdownMenuItem(
            text = { Text(tr("Перейти к строке", "Go to line")) },
            leadingIcon = { Icon(Icons.Rounded.FormatListNumbered, null) },
            onClick = { onDismiss(); onGoTo() },
        )
        DropdownMenuItem(
            text = { Text(tr("Форматировать отступы", "Reformat indentation")) },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.FormatIndentIncrease, null) },
            onClick = {
                onDismiss()
                val n = EditorLogic.reindent(tab.value.text, s.indentUnit)
                vm.edit(tab, tab.value.copy(text = n, selection = TextRange(tab.value.selection.start.coerceAtMost(n.length))), smart = false)
            },
        )
        DropdownMenuItem(
            text = { Text(tr("Закомментировать", "Toggle comment")) },
            leadingIcon = { Icon(Icons.Rounded.Code, null) },
            onClick = { onDismiss(); vm.edit(tab, EditorLogic.toggleComment(tab.value, tab.lang), smart = false) },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(tr("Перенос строк", "Word wrap")) },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.WrapText, null) },
            trailingIcon = { if (s.wordWrap) Icon(Icons.Rounded.DoneAll, null) },
            onClick = { vm.update { it.copy(wordWrap = !it.wordWrap) } },
        )
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.update { it.copy(fontSize = (it.fontSize - 1).coerceAtLeast(9)) } }) {
                Icon(Icons.Rounded.TextDecrease, null)
            }
            Text("${s.fontSize} sp", Modifier.weight(1f, fill = false).padding(horizontal = 8.dp), fontFamily = FontFamily.Monospace)
            IconButton(onClick = { vm.update { it.copy(fontSize = (it.fontSize + 1).coerceAtMost(30)) } }) {
                Icon(Icons.Rounded.TextIncrease, null)
            }
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(tr("Сохранить как…", "Save as…")) },
            leadingIcon = { Icon(Icons.Rounded.SaveAs, null) },
            onClick = { onDismiss(); onSaveAs() },
        )
        DropdownMenuItem(
            text = { Text(tr("Сохранить всё", "Save all")) },
            leadingIcon = { Icon(Icons.Rounded.DoneAll, null) },
            onClick = { onDismiss(); vm.saveAll(quiet = false) },
        )
        DropdownMenuItem(
            text = { Text(tr("Закрыть вкладку", "Close tab")) },
            leadingIcon = { Icon(Icons.Rounded.Close, null) },
            onClick = { onDismiss(); onClose() },
        )
        DropdownMenuItem(
            text = { Text(tr("Закрыть все вкладки", "Close all tabs")) },
            leadingIcon = { Icon(Icons.Rounded.Close, null) },
            onClick = {
                onDismiss()
                if (vm.tabs.any { it.dirty } && s.confirmClose) {
                    vm.toast("Сначала сохраните изменённые вкладки", "Save modified tabs first")
                } else vm.closeAll()
            },
        )
    }
}

@Composable
private fun TabStrip(vm: AppViewModel, onClose: (EditorTab) -> Unit) {
    val state = rememberLazyListState()
    val idx = vm.tabs.indexOfFirst { it.id == vm.activeId }
    LaunchedEffect(vm.activeId, vm.tabs.size) { if (idx >= 0) state.animateScrollToItem(idx) }
    val colors = MaterialTheme.colorScheme
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(vm.tabs, key = { it.id }) { t ->
            val active = t.id == vm.activeId
            val bg by animateColorAsState(if (active) colors.secondaryContainer else colors.surfaceContainerHigh, label = "tabBg")
            val fg by animateColorAsState(if (active) colors.onSecondaryContainer else colors.onSurfaceVariant, label = "tabFg")
            Surface(
                onClick = { vm.select(t) },
                shape = if (active) RoundedCornerShape(14.dp) else CircleShape,
                color = bg,
                contentColor = fg,
                modifier = Modifier.animateItem().height(40.dp),
            ) {
                Row(Modifier.padding(start = 12.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    val dot = when (t.lang) {
                        CodeLang.HTML -> colors.primary
                        CodeLang.CSS -> colors.secondary
                        CodeLang.JS -> colors.tertiary
                        CodeLang.PLAIN -> colors.outline
                    }
                    Box(Modifier.size(8.dp).background(dot, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        t.name, style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp),
                    )
                    AnimatedVisibility(t.dirty, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                        Text(" ●", color = colors.tertiary, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { onClose(t) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, tr("Закрыть", "Close"), Modifier.size(16.dp))
                    }
                }
            }
        }
        item(key = "new") {
            Surface(
                onClick = { vm.newUntitled() },
                shape = CircleShape,
                color = colors.surfaceContainerHigh,
                modifier = Modifier.size(40.dp).animateItem(),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, tr("Новая вкладка", "New tab"), Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun FindBar(
    query: String,
    onQuery: (String) -> Unit,
    replacement: String,
    onReplacement: (String) -> Unit,
    count: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = onQuery, singleLine = true,
                    placeholder = { Text(tr("Найти", "Find")) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    suffix = { if (query.isNotEmpty()) Text("$count") },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrectEnabled = false),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPrev) { Icon(Icons.Rounded.KeyboardArrowUp, null) }
                IconButton(onClick = onNext) { Icon(Icons.Rounded.KeyboardArrowDown, null) }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = replacement, onValueChange = onReplacement, singleLine = true,
                    placeholder = { Text(tr("Заменить на", "Replace with")) },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onReplace, shapes = ButtonDefaults.shapes()) { Text(tr("Заменить", "Replace")) }
                FilledTonalButton(onClick = onReplaceAll, shapes = ButtonDefaults.shapes()) { Text(tr("Все", "All")) }
            }
        }
    }
}

private data class Sym(val label: String, val text: String, val caret: Int = text.length)

@Composable
private fun SymbolBar(lang: CodeLang, onInsert: (String, Int) -> Unit, onTab: () -> Unit) {
    val symbols = remember(lang) {
        val base = listOf("<", ">", "/", "=", "\"", "'", "{", "}", "(", ")", "[", "]", ";", ":", ".", "#", "!", "-", "_", "&", "$", "+", "*", "`", "|", "?", "\\", "%", "@")
            .map { Sym(it, it) }
        val snippets = when (lang) {
            CodeLang.HTML -> listOf(
                Sym("div", "<div></div>", 5), Sym("p", "<p></p>", 3), Sym("a", "<a href=\"\"></a>", 9),
                Sym("img", "<img src=\"\" alt=\"\">", 10), Sym("btn", "<button></button>", 8),
                Sym("style", "<style>\n\n</style>", 8), Sym("script", "<script>\n\n</script>", 9),
                Sym("</>", "</"),
            )
            CodeLang.CSS -> listOf(Sym("{ }", "{\n  \n}", 4), Sym("px", "px"), Sym("rem", "rem"), Sym("%", "%"), Sym("!imp", " !important"))
            CodeLang.JS -> listOf(
                Sym("log", "console.log();", 12), Sym("fn", "function () {\n  \n}", 9), Sym("=>", "() => "),
                Sym("const", "const "), Sym("let", "let "), Sym("if", "if () {\n  \n}", 4),
            )
            CodeLang.PLAIN -> emptyList()
        }
        snippets + base
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SymKey("⇥", primary = true) { onTab() }
            symbols.forEach { sym -> SymKey(sym.label, primary = sym.label.length > 1) { onInsert(sym.text, sym.caret) } }
        }
    }
}

@Composable
private fun SymKey(label: String, primary: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (primary) c.secondaryContainer else c.surfaceContainerHighest,
        contentColor = if (primary) c.onSecondaryContainer else c.onSurface,
        modifier = Modifier.height(38.dp).widthIn(min = 38.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SaveAsDialog(
    name: String,
    folderName: String,
    onDismiss: () -> Unit,
    onWorkspace: (String) -> Unit,
    onDevice: (String) -> Unit,
) {
    var value by remember { mutableStateOf(name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SaveAs, null) },
        title = { Text(tr("Сохранить как", "Save as")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value, onValueChange = { value = it }, singleLine = true,
                    label = { Text(tr("Имя файла", "File name")) },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { onWorkspace(value) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Folder, null); Spacer(Modifier.width(8.dp))
                    Text(tr("В «$folderName»", "To “$folderName”"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(onClick = { onDevice(value) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PhoneAndroid, null); Spacer(Modifier.width(8.dp))
                    Text(tr("На устройство…", "To device…"))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена", "Cancel")) } },
    )
}


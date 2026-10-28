package com.othergram.otherredactor.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.othergram.otherredactor.AppViewModel
import com.othergram.otherredactor.LocalAnimations
import com.othergram.otherredactor.langFor
import com.othergram.otherredactor.tr
import java.io.File
import java.text.DateFormat
import java.util.Date

private sealed interface FileDialog {
    data object NewFile : FileDialog
    data object NewFolder : FileDialog
    data class Rename(val f: File) : FileDialog
    data class Delete(val f: File) : FileDialog
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilesScreen(vm: AppViewModel) {
    var dialog by remember { mutableStateOf<FileDialog?>(null) }
    var fabOpen by remember { mutableStateOf(false) }
    var exportFile by remember { mutableStateOf<File?>(null) }
    val animate = LocalAnimations.current

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importInto(it) }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val f = exportFile
        if (uri != null && f != null) vm.exportTo(f, uri)
        exportFile = null
    }

    BackHandler(enabled = fabOpen) { fabOpen = false }
    BackHandler(enabled = !fabOpen && vm.currentDir != vm.workspace) { vm.up() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(tr("Мои файлы", "My files"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (vm.currentDir != vm.workspace) IconButton(onClick = { vm.up() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад", "Back"))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabOpen,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabOpen,
                        onCheckedChange = { fabOpen = it },
                        modifier = Modifier.semantics { contentDescription = "Create" },
                    ) {
                        val painter = rememberVectorPainter(if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Add)
                        Icon(painter, null, modifier = Modifier.animateIcon({ checkedProgress }))
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = { fabOpen = false; dialog = FileDialog.NewFile },
                    text = { Text(tr("Новый файл", "New file")) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.NoteAdd, null) },
                )
                FloatingActionButtonMenuItem(
                    onClick = { fabOpen = false; dialog = FileDialog.NewFolder },
                    text = { Text(tr("Новая папка", "New folder")) },
                    icon = { Icon(Icons.Rounded.CreateNewFolder, null) },
                )
                FloatingActionButtonMenuItem(
                    onClick = { fabOpen = false; importer.launch(arrayOf("*/*")) },
                    text = { Text(tr("Импорт с устройства", "Import from device")) },
                    icon = { Icon(Icons.Rounded.FileDownload, null) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Breadcrumbs(vm)
            AnimatedContent(
                targetState = vm.currentDir,
                transitionSpec = {
                    if (!animate) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else {
                        val deeper = targetState.absolutePath.length > initialState.absolutePath.length
                        val d = if (deeper) 1 else -1
                        (slideInHorizontally(spring(0.85f, Spring.StiffnessMediumLow)) { it * d / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally(spring(0.85f, Spring.StiffnessMediumLow)) { -it * d / 3 } + fadeOut())
                    }
                },
                label = "dir",
                modifier = Modifier.weight(1f),
            ) { dir ->
                val files = remember(dir, vm.filesVersion) { vm.listDir(dir) }
                if (files.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            Icons.Rounded.FolderOff,
                            tr("Папка пуста", "Folder is empty"),
                            tr("Нажмите + чтобы создать файл или папку", "Tap + to create a file or folder"),
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(files, key = { _, f -> f.absolutePath }) { i, f ->
                            FileRow(
                                f, vm,
                                modifier = Modifier.animateItem().staggered(i),
                                onRename = { dialog = FileDialog.Rename(f) },
                                onDelete = { dialog = FileDialog.Delete(f) },
                                onExport = { exportFile = f; exporter.launch(f.name) },
                            )
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        FileDialog.NewFile -> NameDialog(
            tr("Новый файл", "New file"), "index.html",
            onDismiss = { dialog = null },
            onConfirm = { vm.createFile(it); dialog = null },
        )
        FileDialog.NewFolder -> NameDialog(
            tr("Новая папка", "New folder"), tr("Новый проект", "New project"),
            onDismiss = { dialog = null },
            onConfirm = { vm.createFolder(it); dialog = null },
        )
        is FileDialog.Rename -> NameDialog(
            tr("Переименовать", "Rename"), d.f.name,
            onDismiss = { dialog = null },
            onConfirm = { vm.rename(d.f, it); dialog = null },
        )
        is FileDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(tr("Удалить «${d.f.name}»?", "Delete “${d.f.name}”?")) },
            text = {
                Text(
                    if (d.f.isDirectory) tr("Папка и всё её содержимое будут удалены навсегда.", "The folder and everything inside will be deleted permanently.")
                    else tr("Файл будет удалён навсегда.", "The file will be deleted permanently."),
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.delete(d.f); dialog = null }) {
                    Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(tr("Отмена", "Cancel")) } },
        )
        null -> {}
    }
}

@Composable
private fun Breadcrumbs(vm: AppViewModel) {
    val chain = remember(vm.currentDir) {
        generateSequence(vm.currentDir) { if (it == vm.workspace) null else it.parentFile }.toList().reversed()
    }
    val state = rememberLazyListState()
    LaunchedEffect(chain.size) { state.animateScrollToItem((chain.size - 1).coerceAtLeast(0)) }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(chain, key = { _, f -> f.absolutePath }) { i, dir ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.animateItem()) {
                if (i > 0) Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                AssistChip(
                    onClick = { vm.enter(dir) },
                    label = { Text(if (i == 0) tr("Мои файлы", "My files") else dir.name) },
                    leadingIcon = if (i == 0) ({ Icon(Icons.Rounded.Home, null) }) else null,
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    f: File,
    vm: AppViewModel,
    modifier: Modifier,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val subtitle = remember(f, vm.filesVersion) {
        if (f.isDirectory) {
            val n = f.listFiles()?.size ?: 0
            vm.settings.lang.let { if (it == com.othergram.otherredactor.Lang.RU) "Элементов: $n" else "$n items" }
        } else {
            val size = f.length()
            val s = if (size < 1024) "$size B" else "%.1f KB".format(size / 1024f)
            s + " · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(f.lastModified()))
        }
    }
    Surface(
        onClick = { if (f.isDirectory) vm.enter(f) else vm.openLocal(f) },
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FileBadge(langFor(f.name), folder = f.isDirectory)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = MaterialTheme.shapes.large) {
                    if (!f.isDirectory) DropdownMenuItem(
                        text = { Text(tr("Открыть", "Open")) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
                        onClick = { menu = false; vm.openLocal(f) },
                    )
                    DropdownMenuItem(
                        text = { Text(tr("Переименовать", "Rename")) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menu = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(tr("Дублировать", "Duplicate")) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = { menu = false; vm.duplicate(f) },
                    )
                    if (!f.isDirectory) DropdownMenuItem(
                        text = { Text(tr("Сохранить на устройство", "Save to device")) },
                        leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                        onClick = { menu = false; onExport() },
                    )
                    DropdownMenuItem(
                        text = { Text(tr("Удалить", "Delete"), color = colors.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = colors.error) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}

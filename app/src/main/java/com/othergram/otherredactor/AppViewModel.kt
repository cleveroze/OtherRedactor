package com.othergram.otherredactor

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class NavTab { HOME, FILES, EDITOR, SETTINGS }

sealed interface Source {
    data class Local(val path: String) : Source
    data class External(val uri: String) : Source
    data object Untitled : Source
}

class EditorTab(val id: Long, name: String, source: Source, text: String, saved: String = text) {
    var name by mutableStateOf(name)
    var source by mutableStateOf(source)
    var value by mutableStateOf(TextFieldValue(text, TextRange(0)))
    var saved by mutableStateOf(saved)
    val dirty: Boolean get() = value.text != saved
    val lang: CodeLang get() = langFor(name)

    internal val undo = ArrayDeque<TextFieldValue>()
    internal val redo = ArrayDeque<TextFieldValue>()
    internal var lastPush = 0L
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
}

data class RecentEntry(val kind: Char, val ref: String, val name: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    private val prefs = app.getSharedPreferences("otherredactor", Context.MODE_PRIVATE)

    var settings by mutableStateOf(SettingsStore.load(prefs))
        private set

    fun update(f: (AppSettings) -> AppSettings) {
        settings = f(settings)
        SettingsStore.save(prefs, settings)
    }

    private val lang get() = settings.lang

    var nav by mutableStateOf(NavTab.HOME)
    val tabs = mutableStateListOf<EditorTab>()
    var activeId by mutableStateOf<Long?>(null)
    val active: EditorTab? get() = tabs.firstOrNull { it.id == activeId }
    var previewOpen by mutableStateOf(false)

    val workspace = File(app.filesDir, "workspace")
    private val drafts = File(app.filesDir, "drafts")
    var currentDir by mutableStateOf(workspace)
    var filesVersion by mutableIntStateOf(0)
        private set
    val recents = mutableStateListOf<RecentEntry>()

    var message by mutableStateOf<String?>(null)

    private var nextId = System.currentTimeMillis()

    init {
        if (!workspace.exists()) {
            workspace.mkdirs()
            val demo = File(workspace, lang.t("Мой сайт", "My site")).apply { mkdirs() }
            File(demo, "index.html").writeText(Templates.sampleIndex)
            File(demo, "style.css").writeText(Templates.sampleCss)
            File(demo, "script.js").writeText(Templates.sampleJs)
        }
        drafts.mkdirs()
        loadRecents()
        if (settings.restoreTabs) restoreSession()
    }

    fun toast(ru: String, en: String) { message = lang.t(ru, en) }

    // ---------- tabs ----------

    private fun addTab(tab: EditorTab, open: Boolean = true) {
        tabs.add(tab)
        activeId = tab.id
        if (open) nav = NavTab.EDITOR
        persistSession()
    }

    fun select(tab: EditorTab) { activeId = tab.id }

    private fun findOpen(source: Source) = tabs.firstOrNull { it.source == source }

    fun newUntitled(code: String = Templates.blank, name: String? = null) {
        val base = name ?: "untitled"
        var n = 1
        var candidate = "$base.html"
        while (tabs.any { it.name == candidate }) { n++; candidate = "$base-$n.html" }
        addTab(EditorTab(nextId++, candidate, Source.Untitled, code, ""))
    }

    fun openLocal(file: File) {
        val src = Source.Local(file.absolutePath)
        findOpen(src)?.let { activeId = it.id; nav = NavTab.EDITOR; return }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrNull() }
            if (text == null) { toast("Не удалось открыть файл", "Could not open file"); return@launch }
            addTab(EditorTab(nextId++, file.name, src, text))
            pushRecent(RecentEntry('L', file.absolutePath, file.name))
        }
    }

    fun openExternal(uri: Uri, persist: Boolean = true) {
        val src = Source.External(uri.toString())
        findOpen(src)?.let { activeId = it.id; nav = NavTab.EDITOR; return }
        if (persist) runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.recoverCatching {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { readUri(uri) }
            if (text == null) { toast("Не удалось открыть файл", "Could not open file"); return@launch }
            val name = displayName(uri)
            addTab(EditorTab(nextId++, name, src, text))
            pushRecent(RecentEntry('E', uri.toString(), name))
        }
    }

    fun openRecent(r: RecentEntry) {
        when (r.kind) {
            'L' -> File(r.ref).let { if (it.exists()) openLocal(it) else { recents.remove(r); saveRecents(); toast("Файл не найден", "File not found") } }
            else -> openExternal(Uri.parse(r.ref), persist = false)
        }
    }

    fun close(tab: EditorTab) {
        val i = tabs.indexOf(tab)
        if (i < 0) return
        tabs.removeAt(i)
        File(drafts, "${tab.id}.txt").delete()
        if (activeId == tab.id) activeId = tabs.getOrNull(i.coerceAtMost(tabs.size - 1))?.id
        if (tabs.isEmpty()) previewOpen = false
        persistSession()
    }

    fun closeAll() { tabs.toList().forEach { close(it) } }

    fun edit(tab: EditorTab, newValue: TextFieldValue, smart: Boolean = true) {
        val old = tab.value
        val v = if (smart) EditorLogic.process(old, newValue, settings, tab.lang) else newValue
        if (v.text != old.text) {
            val now = System.currentTimeMillis()
            if (now - tab.lastPush > 800 || tab.undo.isEmpty()) {
                tab.undo.addLast(old)
                if (tab.undo.size > 200) tab.undo.removeFirst()
            }
            tab.lastPush = now
            tab.redo.clear()
            tab.canUndo = true; tab.canRedo = false
        }
        tab.value = v
    }

    fun undo(tab: EditorTab) {
        val prev = tab.undo.removeLastOrNull() ?: return
        tab.redo.addLast(tab.value)
        tab.value = prev
        tab.lastPush = 0
        tab.canUndo = tab.undo.isNotEmpty(); tab.canRedo = true
    }

    fun redo(tab: EditorTab) {
        val next = tab.redo.removeLastOrNull() ?: return
        tab.undo.addLast(tab.value)
        tab.value = next
        tab.lastPush = 0
        tab.canUndo = true; tab.canRedo = tab.redo.isNotEmpty()
    }

    /** Returns false when the tab has nowhere to be saved yet (needs "Save as"). */
    fun save(tab: EditorTab, quiet: Boolean = false): Boolean {
        val text = tab.value.text
        when (val s = tab.source) {
            is Source.Untitled -> return false
            is Source.Local -> runCatching { File(s.path).writeText(text) }
                .onSuccess { tab.saved = text; if (!quiet) toast("Сохранено", "Saved"); filesVersion++ }
                .onFailure { toast("Ошибка сохранения", "Save failed") }
            is Source.External -> runCatching {
                ctx.contentResolver.openOutputStream(Uri.parse(s.uri), "wt")!!.use { it.write(text.toByteArray()) }
            }.onSuccess { tab.saved = text; if (!quiet) toast("Сохранено", "Saved") }
                .onFailure { toast("Нет доступа к файлу — используйте «Сохранить как»", "No write access — use “Save as”") }
        }
        return true
    }

    fun saveAll(quiet: Boolean = true) { tabs.filter { it.dirty && it.source !is Source.Untitled }.forEach { save(it, quiet) } }

    fun saveToWorkspace(tab: EditorTab, name: String, dir: File = currentDir) {
        val clean = name.trim().ifEmpty { tab.name }
        val f = uniqueFile(dir, clean)
        runCatching { f.writeText(tab.value.text) }
            .onSuccess {
                tab.source = Source.Local(f.absolutePath); tab.name = f.name; tab.saved = tab.value.text
                filesVersion++; pushRecent(RecentEntry('L', f.absolutePath, f.name)); persistSession()
                toast("Сохранено в «${dir.name}»", "Saved to “${dir.name}”")
            }
            .onFailure { toast("Ошибка сохранения", "Save failed") }
    }

    fun saveAs(tab: EditorTab, uri: Uri) {
        runCatching {
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(tab.value.text.toByteArray()) }
        }.onSuccess {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            tab.source = Source.External(uri.toString()); tab.name = displayName(uri); tab.saved = tab.value.text
            pushRecent(RecentEntry('E', uri.toString(), tab.name)); persistSession()
            toast("Сохранено на устройство", "Saved to device")
        }.onFailure { toast("Ошибка сохранения", "Save failed") }
    }

    // ---------- files ----------

    fun listDir(dir: File): List<File> =
        (dir.listFiles()?.toList() ?: emptyList())
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))

    fun enter(dir: File) { currentDir = dir }
    fun up(): Boolean {
        if (currentDir == workspace) return false
        currentDir = currentDir.parentFile ?: workspace
        return true
    }

    fun createFolder(name: String) {
        val n = sanitize(name) ?: return toast("Некорректное имя", "Invalid name")
        val f = File(currentDir, n)
        if (f.exists()) return toast("Уже существует", "Already exists")
        f.mkdirs(); filesVersion++
    }

    fun createFile(name: String, open: Boolean = true) {
        var n = sanitize(name) ?: return toast("Некорректное имя", "Invalid name")
        if (!n.contains('.')) n += ".html"
        val f = File(currentDir, n)
        if (f.exists()) return toast("Уже существует", "Already exists")
        val content = when (langFor(n)) {
            CodeLang.HTML -> Templates.blank
            else -> ""
        }
        f.writeText(content); filesVersion++
        if (open) openLocal(f)
    }

    fun rename(f: File, name: String) {
        val n = sanitize(name) ?: return toast("Некорректное имя", "Invalid name")
        val target = File(f.parentFile, n)
        if (target.exists()) return toast("Уже существует", "Already exists")
        if (f.renameTo(target)) {
            tabs.forEach { t ->
                val s = t.source
                if (s is Source.Local && (s.path == f.absolutePath || s.path.startsWith(f.absolutePath + File.separator))) {
                    val np = target.absolutePath + s.path.removePrefix(f.absolutePath)
                    t.source = Source.Local(np); t.name = File(np).name
                }
            }
            filesVersion++; persistSession()
        } else toast("Не удалось переименовать", "Rename failed")
    }

    fun delete(f: File) {
        val path = f.absolutePath
        tabs.filter { (it.source as? Source.Local)?.path?.let { p -> p == path || p.startsWith(path + File.separator) } == true }
            .forEach { close(it) }
        f.deleteRecursively()
        recents.removeAll { it.kind == 'L' && (it.ref == path || it.ref.startsWith(path + File.separator)) }
        saveRecents(); filesVersion++
    }

    fun duplicate(f: File) {
        val copy = uniqueFile(f.parentFile ?: workspace, f.name)
        runCatching { f.copyRecursively(copy) }.onSuccess { filesVersion++ }
    }

    fun importInto(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { readUri(uri) }
            if (text == null) { toast("Не удалось импортировать", "Import failed"); return@launch }
            val f = uniqueFile(currentDir, displayName(uri))
            withContext(Dispatchers.IO) { f.writeText(text) }
            filesVersion++
            toast("Импортировано: ${f.name}", "Imported: ${f.name}")
        }
    }

    fun exportTo(f: File, uri: Uri) {
        runCatching {
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use { out -> f.inputStream().use { it.copyTo(out) } }
        }.onSuccess { toast("Экспортировано", "Exported") }.onFailure { toast("Ошибка экспорта", "Export failed") }
    }

    fun refreshFiles() { filesVersion++ }

    private fun sanitize(name: String): String? {
        val n = name.trim()
        if (n.isEmpty() || n == "." || n == ".." || n.any { it in "/\\:*?\"<>|" }) return null
        return n
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty() || it == name) "" else ".$it" }
        var i = 2
        while (f.exists()) { f = File(dir, "$base ($i)$ext"); i++ }
        return f
    }

    // ---------- preview ----------

    data class PreviewDoc(val html: String, val baseUrl: String?)

    fun previewDoc(tab: EditorTab): PreviewDoc {
        val src = tab.source
        val dir = (src as? Source.Local)?.let { File(it.path).parentFile }
        val base = dir?.let { "file://" + it.absolutePath + "/" }
        return when (tab.lang) {
            CodeLang.HTML -> PreviewDoc(tab.value.text, base)
            CodeLang.CSS, CodeLang.JS -> {
                // Prefer an index.html next to the file so stylesheets/scripts load in context.
                val index = dir?.let { File(it, "index.html") }?.takeIf { it.exists() }
                val openIndex = index?.let { f -> tabs.firstOrNull { (it.source as? Source.Local)?.path == f.absolutePath } }
                when {
                    openIndex != null -> PreviewDoc(openIndex.value.text, base)
                    index != null -> PreviewDoc(index.readText(), base)
                    tab.lang == CodeLang.CSS -> PreviewDoc(
                        "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>${tab.value.text}</style></head>" +
                            "<body><h1>Heading</h1><p>Paragraph with <a href=\"#\">a link</a>.</p><button>Button</button></body></html>",
                        base,
                    )
                    else -> PreviewDoc(
                        "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head><body><script>${tab.value.text}</script></body></html>",
                        base,
                    )
                }
            }
            CodeLang.PLAIN -> PreviewDoc("<pre style=\"white-space:pre-wrap\">" + tab.value.text.replace("&", "&amp;").replace("<", "&lt;") + "</pre>", base)
        }
    }

    // ---------- io helpers ----------

    private fun readUri(uri: Uri): String? = runCatching {
        ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()

    fun displayName(uri: Uri): String {
        runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "file.html"
    }

    // ---------- recents ----------

    private fun pushRecent(r: RecentEntry) {
        recents.removeAll { it.ref == r.ref }
        recents.add(0, r)
        while (recents.size > 15) recents.removeAt(recents.size - 1)
        saveRecents()
    }

    fun clearRecents() { recents.clear(); saveRecents() }

    private fun loadRecents() {
        prefs.getString("recents", "")!!.lines().filter { it.isNotBlank() }.forEach { line ->
            val p = line.split('\t')
            if (p.size == 3) recents.add(RecentEntry(p[0][0], p[1], p[2]))
        }
    }

    private fun saveRecents() {
        prefs.edit().putString("recents", recents.joinToString("\n") { "${it.kind}\t${it.ref}\t${it.name}" }).apply()
    }

    // ---------- session ----------

    fun persistSession() {
        val lines = tabs.joinToString("\n") { t ->
            val (k, ref) = when (val s = t.source) {
                is Source.Local -> "L" to s.path
                is Source.External -> "E" to s.uri
                Source.Untitled -> "U" to ""
            }
            "${t.id}\t$k\t$ref\t${t.name}"
        }
        prefs.edit().putString("session", lines).putLong("activeTab", activeId ?: -1).apply()
    }

    /** Called when the app goes to background: keep unsaved text so nothing is lost. */
    fun persistDrafts() {
        persistSession()
        tabs.forEach { t ->
            val f = File(drafts, "${t.id}.txt")
            if (t.dirty || t.source is Source.Untitled) runCatching { f.writeText(t.value.text) } else f.delete()
        }
    }

    private fun restoreSession() {
        val raw = prefs.getString("session", "") ?: ""
        for (line in raw.lines()) {
            val p = line.split('\t')
            if (p.size < 4) continue
            val id = p[0].toLongOrNull() ?: continue
            val draft = File(drafts, "$id.txt").takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }
            val tab = when (p[1]) {
                "L" -> {
                    val f = File(p[2])
                    if (!f.exists()) null else {
                        val saved = runCatching { f.readText() }.getOrNull() ?: continue
                        EditorTab(id, f.name, Source.Local(f.absolutePath), draft ?: saved, saved)
                    }
                }
                "E" -> {
                    val saved = readUri(Uri.parse(p[2]))
                    if (saved == null && draft == null) null
                    else EditorTab(id, p[3], Source.External(p[2]), draft ?: saved!!, saved ?: "")
                }
                else -> draft?.let { EditorTab(id, p[3], Source.Untitled, it, "") }
            }
            if (tab != null) { tabs.add(tab); nextId = maxOf(nextId, id + 1) }
        }
        val a = prefs.getLong("activeTab", -1)
        activeId = tabs.firstOrNull { it.id == a }?.id ?: tabs.firstOrNull()?.id
    }
}

package com.othergram.otherredactor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.othergram.otherredactor.ui.SyntaxColors

enum class CodeLang { HTML, CSS, JS, PLAIN }

fun langFor(name: String): CodeLang = when (name.substringAfterLast('.', "").lowercase()) {
    "html", "htm", "xhtml", "svg", "xml", "vue" -> CodeLang.HTML
    "css", "scss", "less" -> CodeLang.CSS
    "js", "mjs", "cjs", "ts", "json", "jsx" -> CodeLang.JS
    else -> CodeLang.PLAIN
}

object Highlighter {
    private const val MAX_LEN = 200_000
    private val IC = setOf(RegexOption.IGNORE_CASE)

    private val htmlComment = Regex("<!--[\\s\\S]*?(-->|$)")
    private val doctype = Regex("<!DOCTYPE[^>]*>", IC)
    private val tagOpen = Regex("(</?)([a-zA-Z][\\w:-]*)")
    private val tagClose = Regex("/?>")
    private val attrName = Regex("(?<=\\s)([a-zA-Z_:@][\\w:.\\-]*)(?=\\s*=)")
    private val attrValue = Regex("(?<==)\\s*(\"[^\"]*\"?|'[^']*'?)")
    private val entity = Regex("&[a-zA-Z#0-9]+;")
    private val scriptBlock = Regex("(<script\\b[^>]*>)([\\s\\S]*?)(</script\\s*>|$)", IC)
    private val styleBlock = Regex("(<style\\b[^>]*>)([\\s\\S]*?)(</style\\s*>|$)", IC)

    private val jsKeyword = Regex(
        "\\b(break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|" +
            "finally|for|from|function|if|import|in|instanceof|let|new|return|super|switch|this|throw|" +
            "try|typeof|var|void|while|with|yield|async|await|of|static|get|set)\\b",
    )
    private val jsLiteral = Regex("\\b(true|false|null|undefined|NaN|Infinity)\\b")
    private val number = Regex("\\b(0x[0-9a-fA-F]+|\\d+(\\.\\d+)?([eE][+-]?\\d+)?)\\b")
    private val jsCall = Regex("\\b([a-zA-Z_$][\\w$]*)(?=\\s*\\()")
    private val jsProp = Regex("(?<=\\.)([a-zA-Z_$][\\w$]*)")
    private val jsString = Regex("\"(\\\\.|[^\"\\\\\\n])*\"?|'(\\\\.|[^'\\\\\\n])*'?|`(\\\\.|[^`\\\\])*`?")
    private val jsComment = Regex("//[^\\n]*|/\\*[\\s\\S]*?(\\*/|$)")
    private val punct = Regex("[{}()\\[\\];,]")

    private val cssComment = Regex("/\\*[\\s\\S]*?(\\*/|$)")
    private val cssSelector = Regex("[^{};/]+(?=\\{)")
    private val cssProp = Regex("([a-zA-Z-]+)(?=\\s*:[^{}]*?[;}\\n])")
    private val cssNumber = Regex("-?\\b\\d+(\\.\\d+)?(px|em|rem|%|vh|vw|vmin|vmax|s|ms|deg|fr|ch|pt)?\\b")
    private val cssHex = Regex("#[0-9a-fA-F]{3,8}\\b")
    private val cssAt = Regex("@[a-zA-Z-]+")
    private val cssImportant = Regex("!important")

    fun highlight(text: String, lang: CodeLang, c: SyntaxColors, query: String): AnnotatedString {
        val b = AnnotatedString.Builder(text)
        if (text.length <= MAX_LEN) {
            when (lang) {
                CodeLang.HTML -> html(b, text, c)
                CodeLang.CSS -> css(b, text, 0, text.length, c)
                CodeLang.JS -> js(b, text, 0, text.length, c)
                CodeLang.PLAIN -> {}
            }
        }
        if (query.isNotEmpty()) {
            var i = text.indexOf(query, 0, ignoreCase = true)
            var guard = 0
            while (i >= 0 && guard++ < 5000) {
                b.addStyle(SpanStyle(background = c.search), i, i + query.length)
                i = text.indexOf(query, i + query.length, ignoreCase = true)
            }
        }
        return b.toAnnotatedString()
    }

    private fun AnnotatedString.Builder.paint(re: Regex, text: String, from: Int, to: Int, color: Color, group: Int = 0, italic: Boolean = false) {
        val region = text.substring(from, to)
        for (m in re.findAll(region)) {
            val g = m.groups[group] ?: continue
            val style = if (italic) SpanStyle(color = color, fontStyle = FontStyle.Italic) else SpanStyle(color = color)
            addStyle(style, from + g.range.first, from + g.range.last + 1)
        }
    }

    private fun html(b: AnnotatedString.Builder, t: String, c: SyntaxColors) {
        val n = t.length
        b.paint(tagOpen, t, 0, n, c.punct, 1)
        b.paint(tagOpen, t, 0, n, c.tag, 2)
        b.paint(tagClose, t, 0, n, c.punct)
        b.paint(attrName, t, 0, n, c.attr, 1)
        b.paint(attrValue, t, 0, n, c.string, 1)
        b.paint(entity, t, 0, n, c.number)
        b.paint(doctype, t, 0, n, c.comment)
        b.paint(htmlComment, t, 0, n, c.comment, italic = true)
        for (m in scriptBlock.findAll(t)) {
            val g = m.groups[2] ?: continue
            if (g.range.isEmpty()) continue
            js(b, t, g.range.first, g.range.last + 1, c, reset = true)
        }
        for (m in styleBlock.findAll(t)) {
            val g = m.groups[2] ?: continue
            if (g.range.isEmpty()) continue
            css(b, t, g.range.first, g.range.last + 1, c, reset = true)
        }
    }

    private fun js(b: AnnotatedString.Builder, t: String, from: Int, to: Int, c: SyntaxColors, reset: Boolean = false) {
        if (reset) b.addStyle(SpanStyle(color = c.plain, fontStyle = FontStyle.Normal), from, to)
        b.paint(punct, t, from, to, c.punct)
        b.paint(jsProp, t, from, to, c.prop)
        b.paint(jsCall, t, from, to, c.func, 1)
        b.paint(number, t, from, to, c.number)
        b.paint(jsLiteral, t, from, to, c.number)
        b.paint(jsKeyword, t, from, to, c.keyword)
        b.paint(jsString, t, from, to, c.string)
        b.paint(jsComment, t, from, to, c.comment, italic = true)
    }

    private fun css(b: AnnotatedString.Builder, t: String, from: Int, to: Int, c: SyntaxColors, reset: Boolean = false) {
        if (reset) b.addStyle(SpanStyle(color = c.plain, fontStyle = FontStyle.Normal), from, to)
        b.paint(cssSelector, t, from, to, c.selector)
        b.paint(cssProp, t, from, to, c.prop, 1)
        b.paint(cssNumber, t, from, to, c.number)
        b.paint(cssHex, t, from, to, c.number)
        b.paint(cssAt, t, from, to, c.keyword)
        b.paint(cssImportant, t, from, to, c.keyword)
        b.paint(punct, t, from, to, c.punct)
        b.paint(jsString, t, from, to, c.string)
        b.paint(cssComment, t, from, to, c.comment, italic = true)
    }
}

class SyntaxTransformation(
    private val lang: CodeLang,
    private val colors: SyntaxColors,
    private val enabled: Boolean,
    private val query: String,
) : VisualTransformation {
    private var cacheText: String? = null
    private var cache: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        if (!enabled && query.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val s = text.text
        val out = if (s == cacheText && cache != null) cache!! else {
            Highlighter.highlight(s, if (enabled) lang else CodeLang.PLAIN, colors, query).also {
                cacheText = s; cache = it
            }
        }
        return TransformedText(out, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean =
        other is SyntaxTransformation && other.lang == lang && other.colors == colors &&
            other.enabled == enabled && other.query == query

    override fun hashCode(): Int = listOf(lang, colors, enabled, query).hashCode()
}

private val VOID_TAGS = setOf(
    "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
    "param", "source", "track", "wbr", "!doctype",
)
private val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'', '`' to '`')

object EditorLogic {

    /** Applies smart editing (auto-close, auto-indent) when exactly one character was typed. */
    fun process(old: TextFieldValue, new: TextFieldValue, s: AppSettings, lang: CodeLang): TextFieldValue {
        val ot = old.text; val nt = new.text
        if (nt.length != ot.length + 1 || !new.selection.collapsed || !old.selection.collapsed) return new
        val pos = new.selection.start
        if (pos < 1 || pos - 1 != old.selection.start) return new
        if (!nt.regionMatches(0, ot, 0, pos - 1) || !nt.regionMatches(pos, ot, pos - 1, ot.length - pos + 1)) return new
        val ch = nt[pos - 1]
        val next = nt.getOrNull(pos)
        val prev = nt.getOrNull(pos - 2)

        // Type over an auto-inserted closer.
        if (s.autoCloseBrackets && ch in ")]}\"'`" && next == ch) {
            return TextFieldValue(ot, TextRange(pos))
        }
        if (s.autoCloseBrackets && ch in PAIRS.keys) {
            val isQuote = ch == '"' || ch == '\'' || ch == '`'
            val okBefore = !isQuote || prev == null || !prev.isLetterOrDigit()
            val okAfter = next == null || next.isWhitespace() || next in ")]}>;,"
            if (okBefore && okAfter && !(lang == CodeLang.HTML && isQuote && ch == '\'' && prev?.isLetter() == true)) {
                return insertAt(new, pos, PAIRS.getValue(ch).toString(), 0)
            }
        }
        if (s.autoCloseTags && ch == '>' && lang == CodeLang.HTML) {
            val lt = nt.lastIndexOf('<', pos - 2)
            val gt = nt.lastIndexOf('>', pos - 2)
            if (lt > gt && lt >= 0) {
                val inner = nt.substring(lt + 1, pos - 1)
                if (inner.isNotEmpty() && inner[0].isLetter() && !inner.endsWith("/")) {
                    val name = inner.takeWhile { it.isLetterOrDigit() || it == '-' || it == ':' }.lowercase()
                    if (name.isNotEmpty() && name !in VOID_TAGS) {
                        return insertAt(new, pos, "</$name>", 0)
                    }
                }
            }
        }
        if (s.autoIndent && ch == '\n') {
            val lineStart = nt.lastIndexOf('\n', pos - 2) + 1
            val prevLine = nt.substring(lineStart, pos - 1)
            val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
            val trimmed = prevLine.trimEnd()
            val opensBlock = trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("[") ||
                (lang == CodeLang.HTML && opensTag(trimmed))
            val rest = nt.substring(pos)
            val closesNext = rest.startsWith("}") || rest.startsWith(")") || rest.startsWith("]") || rest.startsWith("</")
            return if (opensBlock && closesNext) {
                val inner = indent + s.indentUnit
                insertAt(new, pos, inner + "\n" + indent, inner.length)
            } else if (opensBlock) {
                insertAt(new, pos, indent + s.indentUnit, indent.length + s.indentUnit.length)
            } else {
                insertAt(new, pos, indent, indent.length)
            }
        }
        return new
    }

    private fun opensTag(line: String): Boolean {
        if (!line.endsWith(">") || line.endsWith("/>")) return false
        val lt = line.lastIndexOf('<')
        if (lt < 0 || line.getOrNull(lt + 1) == '/' || line.getOrNull(lt + 1) == '!') return false
        val name = line.substring(lt + 1).takeWhile { it.isLetterOrDigit() || it == '-' }.lowercase()
        return name.isNotEmpty() && name !in VOID_TAGS && !line.substring(lt).contains("</")
    }

    private fun insertAt(v: TextFieldValue, pos: Int, s: String, cursorInS: Int): TextFieldValue {
        val t = v.text.substring(0, pos) + s + v.text.substring(pos)
        return TextFieldValue(t, TextRange(pos + cursorInS))
    }

    /** Inserts text at the cursor, replacing the selection. `cursor` is the caret offset within [s]. */
    fun insert(v: TextFieldValue, s: String, cursor: Int = s.length): TextFieldValue {
        val start = v.selection.min; val end = v.selection.max
        val t = v.text.substring(0, start) + s + v.text.substring(end)
        return TextFieldValue(t, TextRange(start + cursor))
    }

    /** Wraps the selection in a pair, or inserts the pair with the caret in between. */
    fun wrap(v: TextFieldValue, open: String, close: String): TextFieldValue {
        val start = v.selection.min; val end = v.selection.max
        val sel = v.text.substring(start, end)
        val t = v.text.substring(0, start) + open + sel + close + v.text.substring(end)
        return if (sel.isEmpty()) TextFieldValue(t, TextRange(start + open.length))
        else TextFieldValue(t, TextRange(start + open.length, start + open.length + sel.length))
    }

    fun indentSelection(v: TextFieldValue, unit: String, outdent: Boolean): TextFieldValue {
        val t = v.text
        val start = t.lastIndexOf('\n', (v.selection.min - 1).coerceAtLeast(0)).let { if (v.selection.min == 0) 0 else it + 1 }
        val endLine = t.indexOf('\n', v.selection.max).let { if (it < 0) t.length else it }
        val block = t.substring(start, endLine)
        val lines = block.split('\n')
        var deltaFirst = 0
        var delta = 0
        val newLines = lines.mapIndexed { i, l ->
            if (outdent) {
                val remove = when {
                    l.startsWith(unit) -> unit.length
                    l.startsWith("\t") -> 1
                    else -> l.takeWhile { it == ' ' }.length.coerceAtMost(unit.length)
                }
                if (i == 0) deltaFirst = -remove
                delta -= remove
                l.substring(remove)
            } else {
                if (i == 0) deltaFirst = unit.length
                delta += unit.length
                unit + l
            }
        }
        val nt = t.substring(0, start) + newLines.joinToString("\n") + t.substring(endLine)
        val selStart = (v.selection.min + deltaFirst).coerceIn(start, nt.length)
        val selEnd = (v.selection.max + delta).coerceIn(selStart, nt.length)
        return TextFieldValue(nt, if (v.selection.collapsed) TextRange(selStart) else TextRange(selStart, selEnd))
    }

    fun toggleComment(v: TextFieldValue, lang: CodeLang): TextFieldValue {
        val t = v.text
        val start = if (v.selection.min == 0) 0 else t.lastIndexOf('\n', v.selection.min - 1) + 1
        val end = t.indexOf('\n', v.selection.max).let { if (it < 0) t.length else it }
        val block = t.substring(start, end)
        val (open, close) = when (lang) {
            CodeLang.HTML -> "<!-- " to " -->"
            CodeLang.CSS -> "/* " to " */"
            else -> "// " to ""
        }
        val trimmed = block.trim()
        val replaced = if (trimmed.startsWith(open.trim()) && (close.isEmpty() || trimmed.endsWith(close.trim()))) {
            if (close.isEmpty()) block.lines().joinToString("\n") { it.replaceFirst(Regex("^(\\s*)// ?"), "$1") }
            else block.replaceFirst(Regex("\\s?" + Regex.escape(open.trim()) + " ?"), "")
                .let { val i = it.lastIndexOf(close.trim()); if (i >= 0) it.removeRange(if (i > 0 && it[i - 1] == ' ') i - 1 else i, i + close.trim().length) else it }
        } else {
            if (close.isEmpty()) block.lines().joinToString("\n") { line ->
                val ind = line.takeWhile { it == ' ' || it == '\t' }
                ind + open + line.substring(ind.length)
            } else {
                val ind = block.takeWhile { it == ' ' || it == '\t' }
                ind + open + block.substring(ind.length) + close
            }
        }
        val nt = t.substring(0, start) + replaced + t.substring(end)
        return TextFieldValue(nt, TextRange(start, start + replaced.length))
    }

    /** Very small HTML pretty-printer: re-indents lines by tag/brace depth. */
    fun reindent(text: String, unit: String): String {
        var depth = 0
        val out = StringBuilder()
        val openTag = Regex("<([a-zA-Z][\\w-]*)[^>]*(?<!/)>")
        val closeTag = Regex("</([a-zA-Z][\\w-]*)\\s*>")
        var inPre = false
        for ((idx, raw) in text.lines().withIndex()) {
            val line = raw.trim()
            if (inPre) { out.append(raw); if (line.contains("</pre", true) || line.contains("</textarea", true)) inPre = false }
            else {
                val leadingClose = line.startsWith("</") || line.startsWith("}") || line.startsWith(")") || line.startsWith("]")
                if (leadingClose) depth = (depth - 1).coerceAtLeast(0)
                out.append(if (line.isEmpty()) "" else unit.repeat(depth) + line)
                val opens = openTag.findAll(line).count { it.groupValues[1].lowercase() !in VOID_TAGS } +
                    line.count { it == '{' || it == '(' || it == '[' }
                val closes = closeTag.findAll(line).count() + line.count { it == '}' || it == ')' || it == ']' }
                depth = (depth + opens - closes + if (leadingClose) 1 else 0).coerceAtLeast(0)
                if (line.contains("<pre", true) && !line.contains("</pre", true)) inPre = true
            }
            if (idx < text.lines().size - 1) out.append('\n')
        }
        return out.toString()
    }
}

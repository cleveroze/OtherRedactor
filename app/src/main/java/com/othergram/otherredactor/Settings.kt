package com.othergram.otherredactor

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class Lang { RU, EN }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Palette { VANILLA, DYNAMIC, MOCHA, SAGE, OCEAN, ROSE, LAVENDER }

data class AppSettings(
    val lang: Lang = if (Locale.getDefault().language == "ru") Lang.RU else Lang.EN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: Palette = Palette.VANILLA,
    val amoled: Boolean = false,
    val animations: Boolean = true,
    // editor
    val fontSize: Int = 14,
    val lineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val syntax: Boolean = true,
    val highlightLine: Boolean = true,
    val autoCloseBrackets: Boolean = true,
    val autoCloseTags: Boolean = true,
    val autoIndent: Boolean = true,
    val tabSize: Int = 2,
    val useSpaces: Boolean = true,
    val symbolBar: Boolean = true,
    val autoSave: Boolean = false,
    val keepScreenOn: Boolean = false,
    val confirmClose: Boolean = true,
    val restoreTabs: Boolean = true,
    // preview
    val javaScript: Boolean = true,
    val saveBeforeRun: Boolean = true,
    val showConsole: Boolean = true,
    val defaultDevice: String = "this",
    val customWidth: Int = 412,
    val customHeight: Int = 915,
    // misc
    val onboarded: Boolean = false,
) {
    val indentUnit: String get() = if (useSpaces) " ".repeat(tabSize) else "\t"
}

object SettingsStore {
    fun load(p: SharedPreferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            lang = enumOr(p.getString("lang", null), d.lang),
            themeMode = enumOr(p.getString("themeMode", null), d.themeMode),
            palette = enumOr(p.getString("palette", null), d.palette),
            amoled = p.getBoolean("amoled", d.amoled),
            animations = p.getBoolean("animations", d.animations),
            fontSize = p.getInt("fontSize", d.fontSize),
            lineNumbers = p.getBoolean("lineNumbers", d.lineNumbers),
            wordWrap = p.getBoolean("wordWrap", d.wordWrap),
            syntax = p.getBoolean("syntax", d.syntax),
            highlightLine = p.getBoolean("highlightLine", d.highlightLine),
            autoCloseBrackets = p.getBoolean("autoCloseBrackets", d.autoCloseBrackets),
            autoCloseTags = p.getBoolean("autoCloseTags", d.autoCloseTags),
            autoIndent = p.getBoolean("autoIndent", d.autoIndent),
            tabSize = p.getInt("tabSize", d.tabSize),
            useSpaces = p.getBoolean("useSpaces", d.useSpaces),
            symbolBar = p.getBoolean("symbolBar", d.symbolBar),
            autoSave = p.getBoolean("autoSave", d.autoSave),
            keepScreenOn = p.getBoolean("keepScreenOn", d.keepScreenOn),
            confirmClose = p.getBoolean("confirmClose", d.confirmClose),
            restoreTabs = p.getBoolean("restoreTabs", d.restoreTabs),
            javaScript = p.getBoolean("javaScript", d.javaScript),
            saveBeforeRun = p.getBoolean("saveBeforeRun", d.saveBeforeRun),
            showConsole = p.getBoolean("showConsole", d.showConsole),
            defaultDevice = p.getString("defaultDevice", d.defaultDevice) ?: d.defaultDevice,
            customWidth = p.getInt("customWidth", d.customWidth),
            customHeight = p.getInt("customHeight", d.customHeight),
            onboarded = p.getBoolean("onboarded", d.onboarded),
        )
    }

    fun save(p: SharedPreferences, s: AppSettings) {
        p.edit()
            .putString("lang", s.lang.name)
            .putString("themeMode", s.themeMode.name)
            .putString("palette", s.palette.name)
            .putBoolean("amoled", s.amoled)
            .putBoolean("animations", s.animations)
            .putInt("fontSize", s.fontSize)
            .putBoolean("lineNumbers", s.lineNumbers)
            .putBoolean("wordWrap", s.wordWrap)
            .putBoolean("syntax", s.syntax)
            .putBoolean("highlightLine", s.highlightLine)
            .putBoolean("autoCloseBrackets", s.autoCloseBrackets)
            .putBoolean("autoCloseTags", s.autoCloseTags)
            .putBoolean("autoIndent", s.autoIndent)
            .putInt("tabSize", s.tabSize)
            .putBoolean("useSpaces", s.useSpaces)
            .putBoolean("symbolBar", s.symbolBar)
            .putBoolean("autoSave", s.autoSave)
            .putBoolean("keepScreenOn", s.keepScreenOn)
            .putBoolean("confirmClose", s.confirmClose)
            .putBoolean("restoreTabs", s.restoreTabs)
            .putBoolean("javaScript", s.javaScript)
            .putBoolean("saveBeforeRun", s.saveBeforeRun)
            .putBoolean("showConsole", s.showConsole)
            .putString("defaultDevice", s.defaultDevice)
            .putInt("customWidth", s.customWidth)
            .putInt("customHeight", s.customHeight)
            .putBoolean("onboarded", s.onboarded)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: fallback
}

val LocalLang = staticCompositionLocalOf { Lang.EN }
val LocalAnimations = staticCompositionLocalOf { true }

/** Inline bilingual string: tr("Привет", "Hello"). */
@Composable
fun tr(ru: String, en: String): String = if (LocalLang.current == Lang.RU) ru else en

fun Lang.t(ru: String, en: String): String = if (this == Lang.RU) ru else en

package com.othergram.otherredactor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.othergram.otherredactor.ui.LocalIsDark
import com.othergram.otherredactor.ui.OtherApp
import com.othergram.otherredactor.ui.OtherTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            OtherTheme(vm.settings) {
                val dark = LocalIsDark.current
                LaunchedEffect(dark) {
                    val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                OtherApp(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i == null || (i.action != Intent.ACTION_VIEW && i.action != Intent.ACTION_EDIT)) return
        val uri = i.data ?: return
        if (uri.scheme == "file") uri.path?.let { vm.openLocal(File(it)) }
        else vm.openExternal(uri, persist = (i.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0)
        if (!vm.settings.onboarded) vm.update { it.copy(onboarded = true) }
    }

    override fun onStop() {
        super.onStop()
        vm.persistDrafts()
    }
}

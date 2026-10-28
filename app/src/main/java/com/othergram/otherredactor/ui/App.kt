package com.othergram.otherredactor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.othergram.otherredactor.AppViewModel
import com.othergram.otherredactor.LocalAnimations
import com.othergram.otherredactor.NavTab
import com.othergram.otherredactor.tr
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun OtherApp(vm: AppViewModel) {
    val animate = LocalAnimations.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = vm.settings.onboarded,
            transitionSpec = {
                if (!animate) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else (fadeIn(tween(500)) + scaleIn(spring(0.7f, Spring.StiffnessLow), initialScale = 0.85f)) togetherWith
                    (fadeOut(tween(250)) + scaleOut(targetScale = 1.1f))
            },
            label = "root",
        ) { onboarded ->
            if (!onboarded) OnboardingScreen(vm) else MainShell(vm)
        }
    }
}

private val order = NavTab.entries

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MainShell(vm: AppViewModel) {
    val animate = LocalAnimations.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        snapshotFlow { vm.message }.filterNotNull().collect { m ->
            vm.message = null
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(m) }
        }
    }
    BackHandler(enabled = vm.nav != NavTab.HOME) { vm.nav = NavTab.HOME }

    val imeVisible = WindowInsets.isImeVisible
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = vm.nav,
                transitionSpec = { navTransition(animate) },
                label = "nav",
            ) { tab ->
                when (tab) {
                    NavTab.HOME -> HomeScreen(vm)
                    NavTab.FILES -> FilesScreen(vm)
                    NavTab.EDITOR -> EditorScreen(vm)
                    NavTab.SETTINGS -> SettingsScreen(vm)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
        AnimatedVisibility(
            visible = !imeVisible && !vm.previewOpen,
            enter = expandVertically(spring(0.8f, Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(spring(1f, Spring.StiffnessMedium)) + fadeOut(),
        ) {
            ShortNavigationBar {
                NavTab.entries.forEach { tab ->
                    val selected = vm.nav == tab
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = { vm.nav = tab },
                        icon = {
                            val icon = when (tab) {
                                NavTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
                                NavTab.FILES -> if (selected) Icons.Rounded.Folder else Icons.Outlined.Folder
                                NavTab.EDITOR -> if (selected) Icons.Rounded.Code else Icons.Outlined.Code
                                NavTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
                            }
                            if (tab == NavTab.EDITOR && vm.tabs.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text("${vm.tabs.size}") } }) { Icon(icon, null) }
                            } else Icon(icon, null)
                        },
                        label = {
                            Text(
                                when (tab) {
                                    NavTab.HOME -> tr("Главная", "Home")
                                    NavTab.FILES -> tr("Файлы", "Files")
                                    NavTab.EDITOR -> tr("Редактор", "Editor")
                                    NavTab.SETTINGS -> tr("Настройки", "Settings")
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavTab>.navTransition(animate: Boolean): ContentTransform {
    if (!animate) return fadeIn(tween(0)) togetherWith fadeOut(tween(0))
    val forward = order.indexOf(targetState) > order.indexOf(initialState)
    val dir = if (forward) 1 else -1
    val spec = spring<IntOffset>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    return (slideInHorizontally(spec) { w -> dir * w / 5 } + fadeIn(tween(220, delayMillis = 60))) togetherWith
        (slideOutHorizontally(spec) { w -> -dir * w / 5 } + fadeOut(tween(160)))
}

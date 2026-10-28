package com.othergram.otherredactor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.othergram.otherredactor.AppViewModel
import com.othergram.otherredactor.Lang
import com.othergram.otherredactor.R
import com.othergram.otherredactor.tr
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    fun finish() = vm.update { it.copy(onboarded = true) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < 2) TextButton(onClick = { scope.launch { pager.animateScrollToPage(2) } }) {
                Text(tr("Пропустить", "Skip"))
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val f = 1f - offset.coerceIn(0f, 1f)
                    alpha = 0.3f + 0.7f * f
                    scaleX = 0.88f + 0.12f * f; scaleY = scaleX
                },
                contentAlignment = Alignment.Center,
            ) {
                when (page) {
                    0 -> WelcomePage(vm)
                    1 -> FeaturesPage()
                    else -> ChannelPage(
                        onJoin = { runCatching { uri.openUri(TELEGRAM_URL) }; finish() },
                        onSkip = { finish() },
                    )
                }
            }
        }
        // Expressive page indicator: the active dot stretches into a pill.
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val active = pager.currentPage == i
                val w by animateDpAsState(if (active) 28.dp else 10.dp, spring(0.55f, Spring.StiffnessMediumLow), label = "dot")
                Box(
                    Modifier.size(width = w, height = 10.dp).clip(CircleShape)
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        AnimatedContent(
            targetState = pager.currentPage < 2,
            transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { it / 2 }) },
            label = "cta",
        ) { notLast ->
            if (notLast) {
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(tr("Далее", "Next"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            } else Spacer(Modifier.height(ButtonDefaults.MediumContainerHeight + 24.dp))
        }
    }
}

@Composable
private fun WelcomePage(vm: AppViewModel) {
    val shape = rememberMorphingShape()
    val spin = rememberSpin(30000)
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(220.dp).graphicsLayer { rotationZ = spin }.clip(shape),
        ) {
            Image(
                painterResource(R.drawable.logo), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = -spin; scaleX = 1.35f; scaleY = 1.35f },
            )
        }
        Spacer(Modifier.height(32.dp))
        Text("OtherRedactor", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            tr("HTML, CSS и JavaScript прямо в кармане", "HTML, CSS and JavaScript right in your pocket"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        val ru = vm.settings.lang == Lang.RU
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = ru, onClick = { vm.update { it.copy(lang = Lang.RU) } },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text("Русский") }
            SegmentedButton(
                selected = !ru, onClick = { vm.update { it.copy(lang = Lang.EN) } },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text("English") }
        }
    }
}

@Composable
private fun FeaturesPage() {
    val c = MaterialTheme.colorScheme
    data class F(val icon: ImageVector, val title: String, val text: String, val poly: androidx.graphics.shapes.RoundedPolygon)
    val items = listOf(
        F(Icons.Rounded.Code, tr("Настоящий редактор", "A real code editor"), tr("Подсветка, автоскобки, отступы, поиск", "Highlighting, auto-brackets, indent, search"), MaterialShapes.Cookie7Sided),
        F(Icons.Rounded.Tab, tr("Вкладки как в браузере", "Browser-like tabs"), tr("Переключайтесь между файлами одним касанием", "Switch files with a single tap"), MaterialShapes.Pill),
        F(Icons.Rounded.Devices, tr("Любые экраны", "Any screen"), tr("iPhone, iPad, ноутбук или своё разрешение", "iPhone, iPad, laptop or a custom size"), MaterialShapes.Clover4Leaf),
        F(Icons.Rounded.Terminal, tr("Консоль JS", "JS console"), tr("Логи и ошибки страницы в реальном времени", "Live page logs and errors"), MaterialShapes.Sunny),
        F(Icons.Rounded.FolderCopy, tr("Папки и проекты", "Folders & projects"), tr("Открывайте и сохраняйте файлы на устройство", "Open and save files on your device"), MaterialShapes.Cookie4Sided),
    )
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            tr("Всё для вёрстки", "Everything for web"),
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
            modifier = Modifier.staggered(0),
        )
        items.forEachIndexed { i, f ->
            Surface(shape = MaterialTheme.shapes.large, color = c.surfaceContainer, modifier = Modifier.fillMaxWidth().staggered(i + 1)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShapeIcon(f.icon, f.poly, c.primaryContainer, c.onPrimaryContainer, size = 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(f.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(f.text, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChannelPage(onJoin: () -> Unit, onSkip: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val spin = rememberSpin(14000)
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = rememberMorphingShape(listOf(MaterialShapes.SoftBurst, MaterialShapes.Cookie12Sided, MaterialShapes.Flower)),
                color = c.primaryContainer,
                modifier = Modifier.size(200.dp).graphicsLayer { rotationZ = spin },
            ) {}
            Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(80.dp), tint = c.onPrimaryContainer)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            tr("Присоединяйтесь к каналу", "Join our channel"),
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            tr(
                "Новости, обновления OtherRedactor и других проектов OtherGram Works",
                "News and updates for OtherRedactor and other OtherGram Works projects",
            ),
            style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text("t.me/OtherGramWorks", style = MaterialTheme.typography.labelLarge, color = c.primary)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onJoin,
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Send, null); Spacer(Modifier.width(10.dp))
            Text(tr("Присоединиться", "Join"), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        FilledTonalButton(
            onClick = onSkip,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MinHeight + 8.dp),
        ) { Text(tr("Пропустить и начать", "Skip and start")) }
    }
}

package com.othergram.otherredactor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Html
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Css
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.othergram.otherredactor.AppViewModel
import com.othergram.otherredactor.CodeLang
import com.othergram.otherredactor.NavTab
import com.othergram.otherredactor.R
import com.othergram.otherredactor.Templates
import com.othergram.otherredactor.langFor
import com.othergram.otherredactor.tr
import java.util.Calendar

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.openExternal(it) }
    }
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> tr("Доброе утро", "Good morning")
        in 12..17 -> tr("Добрый день", "Good afternoon")
        in 18..22 -> tr("Добрый вечер", "Good evening")
        else -> tr("Доброй ночи", "Good night")
    }
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.statusBarsPadding().padding(top = 24.dp, bottom = 8.dp).staggered(0),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shape = rememberMorphingShape()
                val spin = rememberSpin(24000)
                Box(Modifier.size(72.dp).graphicsLayer { rotationZ = spin }.clip(shape)) {
                    Image(
                        painterResource(R.drawable.logo), null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = -spin; scaleX = 1.5f; scaleY = 1.5f },
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(greeting, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                    Text("OtherRedactor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
            }
        }

        vm.active?.let { tab ->
            item {
                Surface(
                    onClick = { vm.nav = NavTab.EDITOR },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.primary,
                    contentColor = colors.onPrimary,
                    modifier = Modifier.fillMaxWidth().staggered(1),
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        ShapeIcon(Icons.Rounded.RocketLaunch, MaterialShapes.SoftBurst, colors.onPrimary, colors.primary, rotation = rememberSpin(12000))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Продолжить", "Continue editing"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                tab.name + " · " + tr("вкладок: ${vm.tabs.size}", "${vm.tabs.size} tabs"),
                                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                    }
                }
            }
        }

        item {
            Surface(
                onClick = { vm.newUntitled() },
                shape = MaterialTheme.shapes.extraLarge,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth().staggered(2),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShapeIcon(Icons.Rounded.Add, MaterialShapes.Cookie9Sided, colors.primary, colors.onPrimary, size = 56.dp, rotation = rememberSpin(20000))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(tr("Новый HTML-файл", "New HTML file"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(tr("Чистый шаблон HTML5", "Clean HTML5 skeleton"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().staggered(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickCard(
                    tr("Открыть", "Open"), tr("с устройства", "from device"), Icons.Rounded.FolderOpen,
                    MaterialShapes.Clover4Leaf, colors.secondaryContainer, colors.onSecondaryContainer,
                    Modifier.weight(1f),
                ) { openDoc.launch(arrayOf("text/*", "application/javascript", "application/json", "application/octet-stream")) }
                QuickCard(
                    tr("Мои файлы", "My files"), tr("папки и проекты", "folders & projects"), Icons.Rounded.Folder,
                    MaterialShapes.Cookie4Sided, colors.tertiaryContainer, colors.onTertiaryContainer,
                    Modifier.weight(1f),
                ) { vm.nav = NavTab.FILES }
            }
        }

        item { SectionTitle(tr("Шаблоны", "Templates"), Modifier.staggered(4)) }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.staggered(5),
            ) {
                itemsIndexed(Templates.all) { i, t ->
                    val icon = when (t.id) {
                        "landing" -> Icons.Rounded.Brush
                        "counter" -> Icons.Rounded.TouchApp
                        "anim" -> Icons.Rounded.Animation
                        "canvas" -> Icons.Rounded.PhoneAndroid
                        else -> Icons.Rounded.Html
                    }
                    val poly = listOf(MaterialShapes.Pill, MaterialShapes.Gem, MaterialShapes.Sunny, MaterialShapes.Arch, MaterialShapes.Pentagon)[i % 5]
                    Surface(
                        onClick = { vm.newUntitled(t.code, t.id) },
                        shape = MaterialTheme.shapes.large,
                        color = colors.surfaceContainerHigh,
                        modifier = Modifier.width(170.dp).height(150.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            ShapeIcon(icon, poly, colors.secondaryContainer, colors.onSecondaryContainer, size = 44.dp)
                            Spacer(Modifier.weight(1f))
                            Text(tr(t.ru, t.en), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                tr(t.descRu, t.descEn), style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.staggered(6)) {
                SectionTitle(tr("Недавние", "Recent"), Modifier.weight(1f))
                if (vm.recents.isNotEmpty()) TextButton(onClick = { vm.clearRecents() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(tr("Очистить", "Clear"))
                }
            }
        }
        if (vm.recents.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().staggered(7),
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, null, tint = colors.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            tr("Здесь появятся открытые файлы", "Files you open will show up here"),
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(vm.recents, key = { it.ref }) { r ->
                val lang = langFor(r.name)
                Surface(
                    onClick = { vm.openRecent(r) },
                    shape = MaterialTheme.shapes.large,
                    color = colors.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().animateItem(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FileBadge(lang)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (r.kind == 'L') tr("Мои файлы", "My files") else tr("Устройство", "Device"),
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    poly: androidx.graphics.shapes.RoundedPolygon,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.extraLarge, color = container, contentColor = content, modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            ShapeIcon(icon, poly, content, container, size = 44.dp)
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun FileBadge(lang: CodeLang, folder: Boolean = false) {
    val c = MaterialTheme.colorScheme
    when {
        folder -> ShapeIcon(Icons.Rounded.Folder, MaterialShapes.Cookie4Sided, c.tertiaryContainer, c.onTertiaryContainer, size = 42.dp)
        lang == CodeLang.HTML -> ShapeIcon(Icons.Rounded.Html, MaterialShapes.Cookie7Sided, c.primaryContainer, c.onPrimaryContainer, size = 42.dp)
        lang == CodeLang.CSS -> ShapeIcon(Icons.Rounded.Css, MaterialShapes.Clover4Leaf, c.secondaryContainer, c.onSecondaryContainer, size = 42.dp)
        lang == CodeLang.JS -> ShapeIcon(Icons.Rounded.Javascript, MaterialShapes.Sunny, c.tertiaryContainer, c.onTertiaryContainer, size = 42.dp)
        else -> ShapeIcon(Icons.Rounded.Description, MaterialShapes.Square, c.surfaceContainerHighest, c.onSurfaceVariant, size = 42.dp)
    }
}

package de.flexy06.ilias.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.flexy06.ilias.AppSettings
import de.flexy06.ilias.BrowserHost
import de.flexy06.ilias.BuildConfig
import de.flexy06.ilias.Config
import de.flexy06.ilias.IliasBrowser
import de.flexy06.ilias.IliasSession
import de.flexy06.ilias.PageStyle
import de.flexy06.ilias.Tab
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    hostFactory: (SnackbarHostState, CoroutineScope) -> BrowserHost,
    deepLinks: Flow<String>,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val host = remember { hostFactory(snackbar, scope) }
    val browsers = remember { mutableMapOf<Tab, IliasBrowser>() }
    var tab by rememberSaveable { mutableStateOf(Tab.Dashboard) }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val pageTheme = remember(colors, dark) { PageStyle.theme(colors, dark) }

    fun browserFor(t: Tab): IliasBrowser = browsers.getOrPut(t) {
        IliasBrowser(context, t.url, host).also {
            it.setColors(pageTheme, colors.primary.toArgb(), colors.surface.toArgb())
        }
    }

    val current = browserFor(tab)
    LaunchedEffect(tab) { current.ensureLoaded() }

    // Warm up the other tabs in the background so switching is instant.
    LaunchedEffect(Unit) {
        delay(2500)
        Tab.entries.forEach { t ->
            browserFor(t).ensureLoaded()
            delay(600)
        }
    }

    LaunchedEffect(Unit) {
        deepLinks.collect { url ->
            tab = Tab.Dashboard
            browserFor(Tab.Dashboard).load(url)
        }
    }

    LaunchedEffect(pageTheme, AppSettings.compactMode, AppSettings.appTheme) {
        browsers.values.forEach { it.setColors(pageTheme, colors.primary.toArgb(), colors.surface.toArgb()) }
    }

    DisposableEffect(Unit) {
        onDispose { browsers.values.forEach { it.destroy() }; browsers.clear() }
    }

    // Predictive back: the page shrinks and follows the finger, then goes back in WebView history.
    val backProgress = remember { Animatable(0f) }
    var backEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    PredictiveBackHandler(enabled = current.canGoBack || tab != Tab.Dashboard) { events ->
        try {
            events.collect { e ->
                backEdge = e.swipeEdge
                backProgress.snapTo(e.progress)
            }
            if (current.canGoBack) current.goBack() else tab = Tab.Dashboard
            scope.launch { backProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
        } catch (e: CancellationException) {
            scope.launch { backProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
            throw e
        }
    }

    Scaffold(
        containerColor = colors.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = current.title.ifEmpty { tab.label } to current.subtitle,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "title",
                        ) { (title, sub) ->
                            Column {
                                if (sub.isNotEmpty()) {
                                    Text(
                                        sub,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    title,
                                    style = if (sub.isNotEmpty()) MaterialTheme.typography.titleLarge
                                    else MaterialTheme.typography.headlineSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        AnimatedVisibility(current.canGoBack, enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { current.goBack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { current.load(Config.SEARCH_URL) }) {
                            Icon(Icons.Outlined.Search, "Suchen")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, "Mehr")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Neu laden") },
                                    leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                                    onClick = { menuOpen = false; current.reload() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Link teilen") },
                                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                    onClick = {
                                        menuOpen = false
                                        current.currentUrl?.let { url ->
                                            val send = Intent(Intent.ACTION_SEND)
                                                .setType("text/plain")
                                                .putExtra(Intent.EXTRA_TEXT, url)
                                                .putExtra(Intent.EXTRA_SUBJECT, current.title)
                                            context.startActivity(Intent.createChooser(send, null))
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Im Browser öffnen") },
                                    leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null) },
                                    onClick = {
                                        menuOpen = false
                                        current.currentUrl?.let { host.openExternal(Uri.parse(it)) }
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("ILIAS-Profil") },
                                    leadingIcon = { Icon(Icons.Outlined.AccountCircle, null) },
                                    onClick = { menuOpen = false; current.load(Config.PROFILE_URL) },
                                )
                                DropdownMenuItem(
                                    text = { Text("App-Einstellungen") },
                                    leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                    onClick = { menuOpen = false; settingsOpen = true },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.onSurface,
                    ),
                )
                Box(Modifier.fillMaxWidth().height(4.dp)) {
                    androidx.compose.animation.AnimatedVisibility(current.loading, enter = fadeIn(), exit = fadeOut()) {
                        LinearWavyProgressIndicator(
                            progress = { current.progress.coerceAtLeast(0.08f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = colors.surfaceContainer) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { if (t == tab) browserFor(t).goHome() else tab = t },
                        icon = { Icon(if (t == tab) t.selectedIcon else t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val p = backProgress.value
        val maxShift = with(density) { 16.dp.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .background(colors.surfaceContainerHighest)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = lerp(1f, 0.9f, p)
                        scaleX = s
                        scaleY = s
                        translationX = (if (backEdge == BackEventCompat.EDGE_LEFT) 1f else -1f) * maxShift * p
                    }
                    .clip(RoundedCornerShape(lerp(0f, 28f, p).dp))
                    .background(colors.surface)
            ) {
                key(tab) {
                    AndroidView(
                        factory = { current.container.also { (it.parent as? android.view.ViewGroup)?.removeView(it) } },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (settingsOpen) {
        SettingsSheet(
            onDismiss = { settingsOpen = false },
            onLogout = {
                settingsOpen = false
                scope.launch {
                    IliasSession.logout()
                    onLogout()
                }
            },
        )
    }
}

@Composable
private fun SettingsSheet(onDismiss: () -> Unit, onLogout: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirm by remember { mutableStateOf(false) }
    val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "Einstellungen",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text(IliasSession.credentials.username() ?: "–") },
                supportingContent = { Text("Angemeldet · wird automatisch erneuert") },
                leadingContent = { Icon(Icons.Outlined.Person, null) },
                colors = itemColors,
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("App-Design für ILIAS") },
                supportingContent = { Text("Seiten in Material-You-Farben der App darstellen") },
                leadingContent = { Icon(Icons.Outlined.Palette, null) },
                trailingContent = { Switch(checked = AppSettings.appTheme, onCheckedChange = AppSettings::updateAppTheme) },
                colors = itemColors,
            )
            ListItem(
                headlineContent = { Text("ILIAS-Leisten ausblenden") },
                supportingContent = { Text("Kopf- und Menüleiste durch App-Navigation ersetzen") },
                leadingContent = { Icon(Icons.Outlined.ViewCompact, null) },
                trailingContent = { Switch(checked = AppSettings.compactMode, onCheckedChange = AppSettings::updateCompact) },
                colors = itemColors,
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Abmelden", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Zugangsdaten und Cookies vom Gerät löschen") },
                leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { confirm = true },
                colors = itemColors,
            )
            ListItem(
                headlineContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("ilias.h-ka.de") },
                leadingContent = { Icon(Icons.Outlined.Info, null) },
                colors = itemColors,
            )
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
            title = { Text("Abmelden?") },
            text = { Text("Deine gespeicherten Zugangsdaten werden gelöscht. Du musst dich danach neu anmelden.") },
            confirmButton = { TextButton(onClick = { confirm = false; onLogout() }) { Text("Abmelden") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Abbrechen") } },
        )
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

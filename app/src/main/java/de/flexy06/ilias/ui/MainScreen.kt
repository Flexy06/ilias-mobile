package de.flexy06.ilias.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.flexy06.ilias.AppSettings
import de.flexy06.ilias.BrowserHost
import de.flexy06.ilias.BuildConfig
import de.flexy06.ilias.IliasBrowser
import de.flexy06.ilias.IliasSession
import de.flexy06.ilias.Tab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    hostFactory: (SnackbarHostState, kotlinx.coroutines.CoroutineScope) -> BrowserHost,
    deepLinks: Flow<String>,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val host = remember { hostFactory(snackbar, scope) }
    val browsers = remember { mutableMapOf<Tab, IliasBrowser>() }
    var tab by rememberSaveable { mutableStateOf(Tab.Dashboard) }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    fun browserFor(t: Tab): IliasBrowser = browsers.getOrPut(t) {
        IliasBrowser(context, t.url, host).also {
            it.setRefreshColors(colors.primary.toArgb(), colors.surfaceContainerHigh.toArgb())
        }
    }

    val current = browserFor(tab)
    LaunchedEffect(tab) { current.ensureLoaded() }

    LaunchedEffect(Unit) {
        deepLinks.collect { url ->
            tab = Tab.Dashboard
            browserFor(Tab.Dashboard).load(url)
        }
    }

    LaunchedEffect(AppSettings.compactMode, AppSettings.darkPages) {
        browsers.values.forEach { it.applySettings() }
    }

    DisposableEffect(Unit) {
        onDispose { browsers.values.forEach { it.destroy() }; browsers.clear() }
    }

    BackHandler {
        when {
            current.canGoBack -> current.goBack()
            tab != Tab.Dashboard -> tab = Tab.Dashboard
            else -> (context as? android.app.Activity)?.moveTaskToBack(true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            current.title.ifEmpty { tab.label },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        AnimatedVisibility(current.canGoBack, enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { current.goBack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { current.reload() }) {
                            Icon(Icons.Outlined.Refresh, "Neu laden")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, "Mehr")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                                        current.currentUrl?.let { host.openExternal(android.net.Uri.parse(it)) }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Einstellungen") },
                                    leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                    onClick = { menuOpen = false; settingsOpen = true },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceContainer),
                )
                Box(Modifier.fillMaxWidth()) {
                    androidx.compose.animation.AnimatedVisibility(
                        current.loading,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        LinearWavyProgressIndicator(
                            progress = { current.progress.coerceAtLeast(0.05f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = {
                            if (t == tab) browserFor(t).goHome() else tab = t
                        },
                        icon = { Icon(if (t == tab) t.selectedIcon else t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            key(tab) {
                AndroidView(
                    factory = { current.container.also { (it.parent as? android.view.ViewGroup)?.removeView(it) } },
                    modifier = Modifier.fillMaxSize(),
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(onDismiss: () -> Unit, onLogout: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirm by remember { mutableStateOf(false) }
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
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("ILIAS-Menüleiste ausblenden") },
                supportingContent = { Text("Die App-Navigation ersetzt die ILIAS-Leiste") },
                leadingContent = { Icon(Icons.Outlined.ViewCompact, null) },
                trailingContent = {
                    Switch(checked = AppSettings.compactMode, onCheckedChange = AppSettings::updateCompact)
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            ListItem(
                headlineContent = { Text("Dunkle Seiten im Dark Mode") },
                supportingContent = { Text("ILIAS-Seiten automatisch abdunkeln") },
                leadingContent = { Icon(Icons.Outlined.DarkMode, null) },
                trailingContent = {
                    Switch(checked = AppSettings.darkPages, onCheckedChange = AppSettings::updateDarkPages)
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Abmelden", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Zugangsdaten und Cookies vom Gerät löschen") },
                leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { confirm = true },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            ListItem(
                headlineContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("ilias.h-ka.de") },
                leadingContent = { Icon(Icons.Outlined.Info, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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

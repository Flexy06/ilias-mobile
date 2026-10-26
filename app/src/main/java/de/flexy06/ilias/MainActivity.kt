package de.flexy06.ilias

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import de.flexy06.ilias.ui.IliasTheme
import de.flexy06.ilias.ui.LoginScreen
import de.flexy06.ilias.ui.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val deepLinks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 4)
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data))
        fileCallback = null
    }

    /** Set by the browser host when the stored login no longer works. */
    private var sessionLost: ((String?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            IliasTheme {
                var loggedIn by rememberSaveable { mutableStateOf(IliasSession.hasCredentials()) }
                var loginMessage by rememberSaveable { mutableStateOf<String?>(null) }
                sessionLost = { msg ->
                    loginMessage = msg
                    loggedIn = false
                }
                AnimatedContent(targetState = loggedIn, label = "auth") { isIn ->
                    if (isIn) {
                        MainScreen(
                            hostFactory = { snackbar, scope -> Host(snackbar, scope) },
                            deepLinks = deepLinks,
                            onLogout = { loginMessage = null; loggedIn = false },
                        )
                    } else {
                        LoginScreen(initialMessage = loginMessage) {
                            loginMessage = null
                            loggedIn = true
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (intent.action == Intent.ACTION_VIEW && Config.isIliasHost(data)) {
            deepLinks.tryEmit(data.toString())
        }
    }

    private inner class Host(
        private val snackbar: SnackbarHostState,
        override val scope: CoroutineScope,
    ) : BrowserHost {

        override fun openExternal(uri: Uri) {
            try {
                if (uri.scheme == "http" || uri.scheme == "https") {
                    CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this@MainActivity, uri)
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            } catch (_: ActivityNotFoundException) {
                showMessage("Keine App zum Öffnen gefunden")
            }
        }

        override fun download(url: String, userAgent: String, contentDisposition: String?, mimeType: String?) {
            if (!url.startsWith("http")) {
                showMessage("Dieser Download wird nicht unterstützt")
                return
            }
            val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val req = DownloadManager.Request(Uri.parse(url))
                .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                .addRequestHeader("User-Agent", userAgent)
                .setTitle(name)
                .setDescription("ILIAS")
                .setMimeType(mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ILIAS/$name")
            val dm = getSystemService(DownloadManager::class.java)
            dm.enqueue(req)
            scope.launch {
                val r = snackbar.showSnackbar("„$name“ wird geladen", "Downloads", duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) {
                    runCatching { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                }
            }
        }

        override fun chooseFiles(
            callback: ValueCallback<Array<Uri>>,
            params: WebChromeClient.FileChooserParams,
        ): Boolean {
            fileCallback?.onReceiveValue(null)
            fileCallback = callback
            return try {
                fileChooser.launch(params.createIntent())
                true
            } catch (_: ActivityNotFoundException) {
                fileCallback = null
                false
            }
        }

        override fun onSessionLost(message: String?) {
            scope.launch {
                IliasSession.logout()
                sessionLost?.invoke(message)
            }
        }

        override fun onUserLoggedOut() {
            onSessionLost(null)
        }

        override fun showMessage(message: String) {
            scope.launch { snackbar.showSnackbar(message) }
        }
    }
}

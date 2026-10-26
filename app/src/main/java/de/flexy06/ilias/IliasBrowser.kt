package de.flexy06.ilias

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.ScriptHandler
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Everything a browser tab needs from the hosting screen. */
interface BrowserHost {
    val scope: CoroutineScope
    fun openExternal(uri: Uri)
    fun download(url: String, userAgent: String, contentDisposition: String?, mimeType: String?)
    fun chooseFiles(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean
    fun onSessionLost(message: String?)
    fun onUserLoggedOut()
    fun showMessage(message: String)
}

/** One ILIAS WebView (per bottom-navigation tab) with pull-to-refresh and silent re-login. */
@SuppressLint("SetJavaScriptEnabled")
class IliasBrowser(context: Context, val homeUrl: String, private val host: BrowserHost) {

    var title by mutableStateOf("")
        private set
    var subtitle by mutableStateOf("")
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var loading by mutableStateOf(false)
        private set
    var canGoBack by mutableStateOf(false)
        private set
    var currentUrl by mutableStateOf<String?>(null)
        private set

    private var lastGoodUrl: String? = null
    private var reloginRunning = false
    private var lastReloginAt = 0L
    @Volatile private var atTop = true
    private var theme: PageStyle.Theme? = null
    private var styleScript: String = PageStyle.installScript(null, AppSettings.compactMode)
    private var startScript: ScriptHandler? = null
    private val docStartSupported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    val webView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.offscreenPreRaster = true
        isVerticalScrollBarEnabled = false
        overScrollMode = WebView.OVER_SCROLL_NEVER
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        addJavascriptInterface(Bridge(), "IliasApp")
        webViewClient = Client()
        webChromeClient = Chrome()
        setDownloadListener { url, ua, cd, mime, _ -> host.download(url, ua, cd, mime) }
    }

    val container: SwipeRefreshLayout = SwipeRefreshLayout(context).apply {
        addView(webView)
        setOnRefreshListener { webView.reload() }
        setOnChildScrollUpCallback { _, _ -> webView.scrollY > 0 || !atTop }
    }

    fun ensureLoaded() {
        if (webView.url == null) webView.loadUrl(homeUrl)
    }

    fun load(url: String) = webView.loadUrl(url)
    fun goHome() = webView.loadUrl(homeUrl)
    fun goBack() = webView.goBack()
    fun reload() = webView.reload()

    fun setColors(theme: PageStyle.Theme, indicator: Int, background: Int) {
        this.theme = theme
        container.setColorSchemeColors(indicator)
        container.setProgressBackgroundColorSchemeColor(background)
        webView.setBackgroundColor(background)
        applySettings()
    }

    /** Re-applies theme / compact settings to the current page and all future pages. */
    fun applySettings() {
        val useTheme = AppSettings.appTheme
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            // Our own theme already handles dark mode; only fall back to Chrome's darkening without it.
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, !useTheme)
        }
        styleScript = PageStyle.installScript(if (useTheme) theme else null, AppSettings.compactMode)
        if (docStartSupported) {
            startScript?.remove()
            startScript = WebViewCompat.addDocumentStartJavaScript(webView, styleScript, setOf("https://${Config.HOST}"))
        }
        webView.evaluateJavascript(styleScript, null)
    }

    fun destroy() {
        startScript?.remove()
        container.removeAllViews()
        webView.destroy()
    }

    private fun injectFallback() {
        if (!docStartSupported) webView.evaluateJavascript(styleScript, null)
    }

    private fun relogin(view: WebView, loginUri: Uri?) {
        if (reloginRunning) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastReloginAt < 15_000) {
            // We just logged in and landed on the login page again → credentials no longer valid.
            host.onSessionLost("ILIAS hat die Anmeldung abgelehnt. Bitte melde dich neu an.")
            return
        }
        reloginRunning = true
        lastReloginAt = now
        loading = true
        val target = loginUri?.let { Config.targetFromLogin(it) } ?: lastGoodUrl ?: homeUrl
        view.stopLoading()
        host.scope.launch {
            val result = IliasSession.relogin()
            reloginRunning = false
            when (result) {
                LoginResult.Success -> view.loadUrl(target)
                is LoginResult.InvalidCredentials -> host.onSessionLost(result.message)
                is LoginResult.Error -> {
                    loading = false
                    lastReloginAt = 0L
                    host.showMessage(result.message)
                }
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onScroll(top: Boolean) {
            atTop = top
        }

        @JavascriptInterface
        fun onPageInfo(pageTitle: String?, parent: String?) {
            webView.post {
                pageTitle?.trim()?.takeIf { it.isNotEmpty() }?.let { title = it }
                subtitle = parent?.trim().orEmpty()
            }
        }
    }

    private inner class Client : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            if (AssetCache.isStatic(request)) AssetCache.load(request) else null

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (!request.isForMainFrame) return false
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                host.openExternal(uri); return true
            }
            if (!Config.isIliasHost(uri)) {
                host.openExternal(uri); return true
            }
            if (Config.isLogoutUrl(uri)) {
                host.onUserLoggedOut(); return true
            }
            if (Config.isLoginUrl(uri) && IliasSession.hasCredentials()) {
                relogin(view, uri); return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            loading = true
            atTop = true
            currentUrl = url
            val uri = Uri.parse(url)
            if (Config.isLoginUrl(uri) && IliasSession.hasCredentials()) relogin(view, uri)
        }

        override fun onPageCommitVisible(view: WebView, url: String) {
            injectFallback()
            loading = false
            container.isRefreshing = false
        }

        override fun onPageFinished(view: WebView, url: String) {
            loading = false
            container.isRefreshing = false
            canGoBack = view.canGoBack()
            currentUrl = url
            if (title.isEmpty()) title = cleanTitle(view.title)
            injectFallback()
            val uri = Uri.parse(url)
            if (Config.isIliasHost(uri) && !Config.isLoginUrl(uri)) {
                lastGoodUrl = url
                // Some ILIAS pages render the login form without redirecting to login.php.
                view.evaluateJavascript(
                    "(function(){return !!document.querySelector('form[action*=\"ilstartupgui\" i] input[type=password], form[action*=\"standardauth\" i] input[type=password], form[action*=\"login.php\" i] input[type=password]')})()"
                ) { r ->
                    if (r == "true" && IliasSession.hasCredentials()) relogin(view, null)
                }
            }
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            canGoBack = view.canGoBack()
            currentUrl = url
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progress = newProgress / 100f
        }

        override fun onReceivedTitle(view: WebView, t: String?) {
            // The page's own <h1> (reported via onPageInfo) wins; this is only the first guess.
            title = cleanTitle(t)
            subtitle = ""
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean = host.chooseFiles(filePathCallback, fileChooserParams)
    }

    /** "Inhalt: Höhere Mathematik 1: ILIAS" → "Höhere Mathematik 1" */
    private fun cleanTitle(t: String?): String {
        var s = t.orEmpty().trim()
        if (s.isEmpty() || s.startsWith("http")) return ""
        s = s.removeSuffix(": ILIAS").removeSuffix(" - ILIAS").removeSuffix("ILIAS").trim().trimEnd(':', '-', ' ')
        val parts = s.split(": ")
        return (if (parts.size > 1) parts.drop(1).joinToString(": ") else s).trim()
    }
}

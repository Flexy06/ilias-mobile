package de.flexy06.ilias

import android.content.Context
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.FormElement
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed interface LoginResult {
    data object Success : LoginResult
    data class InvalidCredentials(val message: String) : LoginResult
    data class Error(val message: String) : LoginResult
}

/**
 * Logs into ILIAS natively (OkHttp + Jsoup) and shares the resulting session cookies with
 * the WebView. Used for the first login and for silent re-logins when the session expired.
 */
object IliasSession {
    private lateinit var appContext: Context
    private lateinit var credentialStore: CredentialStore
    private val mutex = Mutex()
    @Volatile private var lastSuccessAt = 0L

    val userAgent: String by lazy { WebSettings.getDefaultUserAgent(appContext) }

    private val client by lazy {
        OkHttpClient.Builder()
            .cookieJar(WebViewCookieJar())
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        credentialStore = CredentialStore(appContext)
    }

    val credentials: CredentialStore get() = credentialStore

    fun hasCredentials(): Boolean = credentialStore.username() != null

    /** Single-flight re-login with the stored credentials. */
    suspend fun relogin(): LoginResult = mutex.withLock {
        if (SystemClock.elapsedRealtime() - lastSuccessAt < 5_000) return LoginResult.Success
        val c = credentialStore.load()
            ?: return LoginResult.InvalidCredentials("Keine gespeicherten Zugangsdaten")
        loginInternal(c)
    }

    /** First login from the login screen; stores the credentials on success. */
    suspend fun login(c: Credentials): LoginResult = mutex.withLock {
        val r = loginInternal(c)
        if (r is LoginResult.Success) credentialStore.save(c)
        r
    }

    suspend fun logout() {
        credentialStore.clear()
        lastSuccessAt = 0L
        withContext(Dispatchers.Main) {
            suspendCoroutine { cont ->
                CookieManager.getInstance().removeAllCookies { cont.resume(Unit) }
            }
            CookieManager.getInstance().flush()
        }
    }

    private suspend fun loginInternal(c: Credentials): LoginResult = withContext(Dispatchers.IO) {
        try {
            val page = client.newCall(
                Request.Builder().url(Config.LOGIN_URL).header("User-Agent", userAgent).build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext LoginResult.Error("ILIAS antwortet nicht (HTTP ${resp.code})")
                Jsoup.parse(resp.body!!.string(), resp.request.url.toString())
            }

            val form = page.select("form").firstOrNull { it.selectFirst("input[type=password]") != null } as? FormElement
                ?: return@withContext LoginResult.Error("Login-Formular von ILIAS nicht gefunden")
            val passField = form.selectFirst("input[type=password]")!!.attr("name")
            val userField = form.select("input").firstOrNull {
                val t = it.attr("type").lowercase()
                (t.isEmpty() || t == "text" || t == "email") && it.attr("name").isNotEmpty()
            }?.attr("name") ?: return@withContext LoginResult.Error("Benutzerfeld nicht gefunden")

            val body = FormBody.Builder()
            val seen = mutableSetOf<String>()
            for (kv in form.formData()) {
                when (kv.key()) {
                    userField -> body.add(userField, c.username)
                    passField -> body.add(passField, c.password)
                    else -> body.add(kv.key(), kv.value())
                }
                seen += kv.key()
            }
            if (userField !in seen) body.add(userField, c.username)
            if (passField !in seen) body.add(passField, c.password)

            val action = form.absUrl("action").ifEmpty { page.location() }
            client.newCall(
                Request.Builder()
                    .url(action)
                    .header("User-Agent", userAgent)
                    .header("Referer", page.location())
                    .post(body.build())
                    .build()
            ).execute().use { resp ->
                val finalUrl = resp.request.url.toString()
                val doc = Jsoup.parse(resp.body!!.string(), finalUrl)
                val stillLogin = doc.select("form input[type=password]").isNotEmpty() &&
                    (finalUrl.contains("login.php", true) || finalUrl.contains("ilstartupgui", true))
                if (stillLogin) {
                    val msg = doc.select(".alert-danger, .alert-warning, [role=alert], .alert")
                        .text().trim().ifEmpty { "Benutzername oder Passwort falsch" }
                    LoginResult.InvalidCredentials(msg)
                } else {
                    lastSuccessAt = SystemClock.elapsedRealtime()
                    CookieManager.getInstance().flush()
                    LoginResult.Success
                }
            }
        } catch (e: IOException) {
            LoginResult.Error("Keine Verbindung zu ILIAS: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }
}

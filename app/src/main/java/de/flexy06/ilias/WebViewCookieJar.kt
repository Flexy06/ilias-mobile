package de.flexy06.ilias

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

/**
 * Shares cookies between OkHttp (native login) and the WebView.
 * Session cookies coming from ILIAS get an expiry date so the login survives app restarts.
 */
class WebViewCookieJar : CookieJar {
    private val cm: CookieManager get() = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val keepUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
        for (c in cookies) {
            val stored = if (!c.persistent && c.value.isNotEmpty()) {
                Cookie.Builder()
                    .name(c.name)
                    .value(c.value)
                    .path(c.path)
                    .apply {
                        if (c.hostOnly) hostOnlyDomain(c.domain) else domain(c.domain)
                        if (c.secure) secure()
                        if (c.httpOnly) httpOnly()
                    }
                    .expiresAt(keepUntil)
                    .build()
            } else c
            cm.setCookie(url.toString(), stored.toString())
        }
        cm.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val header = cm.getCookie(url.toString()) ?: return emptyList()
        return header.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
    }
}

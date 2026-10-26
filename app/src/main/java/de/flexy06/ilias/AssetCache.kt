package de.flexy06.ilias

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Serves ILIAS' static files (JS, CSS, fonts, icons) from a persistent disk cache so pages
 * render without waiting for dozens of revalidation round-trips. HTML pages are never cached.
 */
object AssetCache {
    private val STATIC_EXT = setOf("js", "css", "woff", "woff2", "ttf", "otf", "svg", "png", "gif", "jpg", "jpeg", "webp", "ico")
    private const val MAX_AGE_SECONDS = 3 * 24 * 3600

    private lateinit var client: OkHttpClient

    fun init(context: Context) {
        client = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "ilias-assets"), 150L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                val resp = chain.proceed(chain.request())
                if (resp.isSuccessful) {
                    resp.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Expires")
                        .removeHeader("Set-Cookie")
                        .header("Cache-Control", "public, max-age=$MAX_AGE_SECONDS")
                        .build()
                } else resp
            }
            .build()
    }

    fun isStatic(request: WebResourceRequest): Boolean {
        if (request.method != "GET") return false
        val uri = request.url
        if (!Config.isIliasHost(uri)) return false
        val path = uri.path?.lowercase() ?: return false
        if (path.startsWith("/data/") || path.contains("ilias.php") || path.contains("goto")) return false
        val ext = path.substringAfterLast('.', "")
        return ext in STATIC_EXT
    }

    fun load(request: WebResourceRequest): WebResourceResponse? = try {
        val url = request.url.toString()
        val builder = Request.Builder().url(url)
        request.requestHeaders["User-Agent"]?.let { builder.header("User-Agent", it) }
        CookieManager.getInstance().getCookie(url)?.let { builder.header("Cookie", it) }
        val resp = client.newCall(builder.build()).execute()
        if (resp.code !in 200..299) {
            resp.close(); null
        } else {
            val body = resp.body!!
            val ct = body.contentType()
            val mime = ct?.let { "${it.type}/${it.subtype}" } ?: guessMime(url)
            val charset = ct?.charset()?.name() ?: if (mime.startsWith("text/") || mime.contains("javascript")) "utf-8" else null
            WebResourceResponse(
                mime, charset, resp.code, resp.message.ifBlank { "OK" },
                mapOf("Cache-Control" to "public, max-age=$MAX_AGE_SECONDS", "Access-Control-Allow-Origin" to "*"),
                body.byteStream(),
            )
        }
    } catch (_: Exception) {
        null
    }

    private fun guessMime(url: String): String = when (url.substringBefore('?').substringAfterLast('.').lowercase()) {
        "js" -> "application/javascript"
        "css" -> "text/css"
        "svg" -> "image/svg+xml"
        "woff2" -> "font/woff2"
        "woff" -> "font/woff"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
}

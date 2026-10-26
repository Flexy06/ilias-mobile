package de.flexy06.ilias

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class IliasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        IliasSession.init(this)
        AppSettings.init(this)
        AssetCache.init(this)
    }
}

/** Small observable settings store (SharedPreferences + Compose state). */
object AppSettings {
    private lateinit var prefs: android.content.SharedPreferences

    var compactMode by mutableStateOf(true)
        private set
    var appTheme by mutableStateOf(true)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        compactMode = prefs.getBoolean("compact", true)
        appTheme = prefs.getBoolean("app_theme", true)
    }

    fun updateCompact(v: Boolean) {
        compactMode = v; prefs.edit().putBoolean("compact", v).apply()
    }

    fun updateAppTheme(v: Boolean) {
        appTheme = v; prefs.edit().putBoolean("app_theme", v).apply()
    }
}

package de.flexy06.ilias

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector

object Config {
    const val HOST = "ilias.h-ka.de"
    const val BASE = "https://$HOST"
    const val LOGIN_URL = "$BASE/login.php?cmd=force_login&lang=de"

    const val SEARCH_URL = "$BASE/ilias.php?baseClass=ilSearchControllerGUI"
    const val PROFILE_URL = "$BASE/ilias.php?baseClass=ilDashboardGUI&cmd=jumpToProfile"
    const val SETTINGS_URL = "$BASE/ilias.php?baseClass=ilDashboardGUI&cmd=jumpToSettings"

    fun isIliasHost(uri: Uri): Boolean = uri.host.equals(HOST, ignoreCase = true)

    /** True for ILIAS pages that show the login form. */
    fun isLoginUrl(uri: Uri): Boolean {
        if (!isIliasHost(uri)) return false
        val path = uri.path.orEmpty().lowercase()
        if (path.endsWith("/login.php")) return true
        val q = uri.query.orEmpty().lowercase()
        return q.contains("baseclass=ilstartupgui") &&
            (q.contains("cmd=showloginpage") || q.contains("cmd=force_login") || q.contains("dostandardauthentication"))
    }

    fun isLogoutUrl(uri: Uri): Boolean {
        if (!isIliasHost(uri)) return false
        val p = uri.path.orEmpty().lowercase()
        val q = uri.query.orEmpty().lowercase()
        return p.endsWith("/logout.php") || q.contains("cmd=dologout") || q.contains("cmd=logout")
    }

    /** ILIAS passes the originally requested object as ?target=crs_123 to login.php. */
    fun targetFromLogin(uri: Uri): String? {
        val t = uri.getQueryParameter("target")?.takeIf { it.isNotBlank() } ?: return null
        return "$BASE/goto.php?target=${Uri.encode(t)}"
    }
}

enum class Tab(
    val label: String,
    val url: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Dashboard(
        "Übersicht",
        "${Config.BASE}/ilias.php?baseClass=ilDashboardGUI&cmd=jumpToSelectedItems",
        Icons.Outlined.Dashboard, Icons.Filled.Dashboard,
    ),
    Courses(
        "Kurse",
        "${Config.BASE}/ilias.php?baseClass=ilmembershipoverviewgui",
        Icons.Outlined.School, Icons.Filled.School,
    ),
    Calendar(
        "Kalender",
        "${Config.BASE}/ilias.php?baseClass=ilDashboardGUI&cmd=jumpToCalendar",
        Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth,
    ),
    Mail(
        "Mail",
        "${Config.BASE}/ilias.php?baseClass=ilMailGUI",
        Icons.Outlined.Mail, Icons.Filled.Mail,
    ),
}

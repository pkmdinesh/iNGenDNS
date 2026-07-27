package com.ingendns.app.settings

import android.content.Context

/** Lightweight persistent store; repository-backed storage can replace this without changing the UI contract. */
class AppSettings(context: Context) {
    private val preferences =
        context.getSharedPreferences("smart_dns_settings", Context.MODE_PRIVATE)

    var autoConnectEnabled: Boolean
        get() = preferences.getBoolean(AUTO_CONNECT, false)
        set(value) = preferences.edit().putBoolean(AUTO_CONNECT, value).apply()

    var autoReconnectIntervalHours: Int
        get() = preferences.getInt(AUTO_RECONNECT_INTERVAL, 6).coerceIn(1, 12)
        set(value) = preferences.edit().putInt(AUTO_RECONNECT_INTERVAL, value.coerceIn(1, 12)).apply()

    var darkModeEnabled: Boolean
        get() = preferences.getBoolean(DARK_MODE, true)
        set(value) = preferences.edit().putBoolean(DARK_MODE, value).apply()

    private companion object {
        const val AUTO_CONNECT = "auto_connect"
        const val AUTO_RECONNECT_INTERVAL = "auto_reconnect_interval"
        const val DARK_MODE = "dark_mode"
    }
}

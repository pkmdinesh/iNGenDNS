package com.ingendns.app.datastore

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

object SettingsKeys {

    val DNS_INTERVAL =
        longPreferencesKey("dns_interval")

    val MONITOR_INTERVAL =
        longPreferencesKey("monitor_interval")

    val SWITCH_DELAY =
        longPreferencesKey("switch_delay")

    val RETRY_COUNT =
        intPreferencesKey("retry_count")
}
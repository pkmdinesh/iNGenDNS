package com.ingendns.app.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ingendns.app.util.Constants
import kotlinx.coroutines.flow.first
import com.ingendns.app.vpn.DnsProtocol

data class LastConnectedDns(
    val ip: String,
    val name: String?,
    val protocol: DnsProtocol
)

private val Context.dataStore by preferencesDataStore("ingendns")

class PreferenceManager(
    private val context: Context
) {

    companion object {

        private val ACTIVE_DNS =
            stringPreferencesKey("active_dns")

        private val DNS_PROTOCOL = stringPreferencesKey("dns_protocol")

        private val DOT_DEFAULT_APPLIED = booleanPreferencesKey("dot_default_applied")

        private val LAST_CONNECTED_IP = stringPreferencesKey("last_connected_ip")
        private val LAST_CONNECTED_NAME = stringPreferencesKey("last_connected_name")
        private val LAST_CONNECTED_PROTOCOL = stringPreferencesKey("last_connected_protocol")

    }

    suspend fun saveActiveDns(
        dns: String
    ) {

        context.dataStore.edit {

            it[ACTIVE_DNS] = dns

        }

    }

    suspend fun getActiveDns(): String {

        return context.dataStore.data
            .first()[ACTIVE_DNS] ?: Constants.DEFAULT_ACTIVE_DNS

    }

    suspend fun saveDnsProtocol(protocol: DnsProtocol) {
        context.dataStore.edit {
            it[DNS_PROTOCOL] = protocol.name
            it[DOT_DEFAULT_APPLIED] = true
        }
    }

    suspend fun getDnsProtocol(): DnsProtocol {
        val preferences = context.dataStore.data.first()
        if (preferences[DOT_DEFAULT_APPLIED] != true) {
            context.dataStore.edit {
                it[DNS_PROTOCOL] = DnsProtocol.DOT.name
                it[DOT_DEFAULT_APPLIED] = true
            }
            return DnsProtocol.DOT
        }
        return preferences[DNS_PROTOCOL]
            ?.let { runCatching { DnsProtocol.valueOf(it) }.getOrNull() }
            ?: DnsProtocol.DOT
    }

    suspend fun saveLastConnectedDns(ip: String, name: String?, protocol: DnsProtocol) {
        context.dataStore.edit { preferences ->
            preferences[LAST_CONNECTED_IP] = ip
            if (name.isNullOrBlank()) preferences.remove(LAST_CONNECTED_NAME)
            else preferences[LAST_CONNECTED_NAME] = name
            preferences[LAST_CONNECTED_PROTOCOL] = protocol.name
        }
    }

    suspend fun getLastConnectedDns(): LastConnectedDns? {
        val preferences = context.dataStore.data.first()
        val ip = preferences[LAST_CONNECTED_IP]?.takeIf { it.isNotBlank() } ?: return null
        val protocol = preferences[LAST_CONNECTED_PROTOCOL]?.let { stored ->
            runCatching { DnsProtocol.valueOf(stored) }.getOrNull()
        } ?: return null
        return LastConnectedDns(ip, preferences[LAST_CONNECTED_NAME], protocol)
    }

}

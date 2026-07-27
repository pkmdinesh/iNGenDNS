package com.ingendns.app.vpn

import android.content.Context

internal data class PersistedVpnConfiguration(
    val resolver: String,
    val name: String?,
    val protocol: DnsProtocol,
    val endpoint: String
)

/** Configuration required to rebuild the VPN after Android recreates the service process. */
internal class VpnConfigurationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun save(configuration: PersistedVpnConfiguration) {
        preferences.edit()
            .putString(RESOLVER, configuration.resolver)
            .putString(NAME, configuration.name)
            .putString(PROTOCOL, configuration.protocol.name)
            .putString(ENDPOINT, configuration.endpoint)
            .apply()
    }

    fun load(): PersistedVpnConfiguration? {
        val resolver = preferences.getString(RESOLVER, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val endpoint = preferences.getString(ENDPOINT, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val protocol = preferences.getString(PROTOCOL, null)?.let { stored ->
            runCatching { DnsProtocol.valueOf(stored) }.getOrNull()
        } ?: return null
        return PersistedVpnConfiguration(
            resolver = resolver,
            name = preferences.getString(NAME, null),
            protocol = protocol,
            endpoint = endpoint
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "dns_vpn_configuration"
        const val RESOLVER = "resolver"
        const val NAME = "name"
        const val PROTOCOL = "protocol"
        const val ENDPOINT = "endpoint"
    }
}

package com.ingendns.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.benchmark.DnsBenchmarkEngine
import com.ingendns.app.dns.benchmark.UdpDnsProbe
import com.ingendns.app.domain.repository.DnsRepository
import com.ingendns.app.settings.AppSettings
import com.ingendns.app.workers.DnsWorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: DnsRepository,
    private val context: Context
) : ViewModel() {
    private val settings = AppSettings(context)
    private val benchmarkEngine = DnsBenchmarkEngine(UdpDnsProbe())
    val dnsServers = repository.observeDnsServers().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val mutableAutoConnectEnabled = MutableStateFlow(settings.autoConnectEnabled)
    val autoConnectEnabled = mutableAutoConnectEnabled.asStateFlow()

    private val mutableReconnectInterval = MutableStateFlow(settings.autoReconnectIntervalHours)
    val reconnectInterval = mutableReconnectInterval.asStateFlow()

    private val mutableDarkModeEnabled = MutableStateFlow(settings.darkModeEnabled)
    val darkModeEnabled = mutableDarkModeEnabled.asStateFlow()

    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.purgeCustomDnsConflicts()
        }
    }

    fun setAutoConnectEnabled(enabled: Boolean) {
        settings.autoConnectEnabled = enabled
        mutableAutoConnectEnabled.value = enabled
        if (enabled) {
            DnsWorkScheduler.schedule(context, mutableReconnectInterval.value)
        } else {
            DnsWorkScheduler.cancel(context)
        }
    }

    fun setReconnectInterval(hours: Int) {
        val interval = hours.coerceIn(1, 12)
        settings.autoReconnectIntervalHours = interval
        mutableReconnectInterval.value = interval
        if (mutableAutoConnectEnabled.value) DnsWorkScheduler.ensureScheduled(context, interval)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        settings.darkModeEnabled = enabled
        mutableDarkModeEnabled.value = enabled
    }

    fun addServer(name: String, address: String, dotHostname: String, dohUrl: String): Boolean {
        return saveServer(null, name, address, dotHostname, dohUrl)
    }

    fun updateServer(
        original: DnsServer,
        name: String,
        address: String,
        dotHostname: String,
        dohUrl: String
    ): Boolean = saveServer(original, name, address, dotHostname, dohUrl)

    private fun saveServer(
        original: DnsServer?,
        name: String,
        address: String,
        dotHostname: String,
        dohUrl: String
    ): Boolean {
        val cleanName = name.trim()
        val cleanAddress = address.trim()
        val cleanDot = dotHostname.trim()
        val cleanDoh = dohUrl.trim()
        val error = when {
            cleanName.isEmpty() -> "Enter a DNS name."
            !isIpv4(cleanAddress) -> "Enter a valid IPv4 DNS address."
            dotHostname.any(Char::isWhitespace) -> "DoT hostname cannot contain spaces."
            dohUrl.any(Char::isWhitespace) -> "DoH URL cannot contain spaces."
            cleanDot.isEmpty() && cleanDoh.isEmpty() -> "Enter a DoT hostname or DoH URL."
            cleanDoh.isNotEmpty() && !cleanDoh.startsWith("https://") -> "DoH URL must start with https://."
            dnsServers.value.any { server ->
                server.ip == cleanAddress && !sameProfile(server, original)
            } ->
                "That DNS address already exists."
            else -> null
        }
        if (error != null) {
            mutableMessage.value = error
            return false
        }
        viewModelScope.launch {
            val savedServer = DnsServer(
                cleanName,
                cleanAddress,
                isCustom = original?.isCustom ?: true,
                dotHostname = cleanDot,
                dohUrl = cleanDoh,
                profileId = original?.profileId
            )
            if (original?.isCustom == false) {
                repository.updateDefaultDnsServer(savedServer)
            } else {
                if (original != null && original.ip != cleanAddress) {
                    repository.removeCustomDnsServer(original.ip)
                }
                repository.addCustomDnsServer(savedServer)
            }
            mutableMessage.value = "$cleanName saved. Testing DNS profile…"
            val result = benchmarkEngine.benchmarkSingle(savedServer)
            repository.saveCustomBenchmarkResult(result, original?.ip)
            mutableMessage.value = if (result.reachable) {
                "$cleanName tested: ${result.latency} ms, ${result.score}/100."
            } else {
                "$cleanName saved, but the DNS test timed out."
            }
        }
        return true
    }

    fun removeServer(server: DnsServer) {
        viewModelScope.launch {
            repository.removeCustomDnsServer(server.ip)
            mutableMessage.value = "${server.name} removed."
        }
    }

    fun clearMessage() {
        mutableMessage.value = null
    }

    private fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.length <= 3 &&
                part.all(Char::isDigit) && part.toIntOrNull() in 0..255
        }
    }

    private fun sameProfile(server: DnsServer, original: DnsServer?): Boolean = when {
        original == null -> false
        original.profileId != null -> server.profileId == original.profileId
        else -> server.isCustom && original.isCustom && server.ip == original.ip
    }
}

class SettingsViewModelFactory(
    private val repository: DnsRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

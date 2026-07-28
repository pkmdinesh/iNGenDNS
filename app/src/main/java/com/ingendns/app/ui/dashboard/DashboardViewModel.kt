package com.ingendns.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingendns.app.database.BenchmarkSessionEntity
import com.ingendns.app.dns.benchmark.DnsBenchmarkEngine
import com.ingendns.app.dns.benchmark.UdpDnsProbe
import com.ingendns.app.dns.recommendation.DnsRecommendationEngine
import com.ingendns.app.dns.recommendation.DnsSelectionPolicy
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.domain.repository.DnsRepository
import com.ingendns.app.preferences.PreferenceManager
import com.ingendns.app.network.NetworkMonitor
import com.ingendns.app.vpn.DnsVpnState
import com.ingendns.app.vpn.DnsProtocol
import com.ingendns.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(
    private val repository: DnsRepository,
    private val preferences: PreferenceManager,
    networkMonitor: NetworkMonitor
) : ViewModel() {
    private val recommendationEngine =
        DnsRecommendationEngine()
    private val benchmarkEngine =
        DnsBenchmarkEngine(
            probe = UdpDnsProbe()
        )

    private val _uiState = MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    init {
        observeSavedResults()
        viewModelScope.launch {
            val protocol = preferences.getDnsProtocol()
            _uiState.update { it.copy(selectedProtocol = protocol) }
        }
        viewModelScope.launch {
            networkMonitor.state.collect { network ->
                _uiState.update { it.copy(network = network) }
            }
        }
        viewModelScope.launch {
            DnsVpnState.state.collect { vpn ->
                _uiState.update {
                    it.copy(
                        vpnActive = vpn.active,
                        vpnResolver = vpn.resolver,
                        vpnProtocol = vpn.protocol,
                        vpnHostname = vpn.hostname,
                        encryptedConnected = vpn.encryptedConnected,
                        vpnConnectionFailed = vpn.connectionFailed,
                        vpnMode = vpn.mode,
                        vpnStatusMessage = vpn.statusMessage,
                        vpnLockdownEnabled = vpn.lockdownEnabled,
                        vpnAlwaysOnEnabled = vpn.alwaysOnEnabled,
                        activeDns = if (vpn.active) vpn.name ?: vpn.resolver.orEmpty() else "Automatic"
                    )
                }
            }
        }
    }

    fun setProtocol(protocol: DnsProtocol) {
        _uiState.update { it.copy(selectedProtocol = protocol) }
        viewModelScope.launch { preferences.saveDnsProtocol(protocol) }
    }

    private fun observeSavedResults() {
        viewModelScope.launch {
            repository.observeLatestResults().collect { results ->
                val analytics = repository.getAnalytics()
                _uiState.update {
                    it.copy(
                        testing = false,
                        results = results,
                        analytics = analytics
                    )
                }
            }
        }
    }

    fun runBenchmark() {
        viewModelScope.launch {
            benchmarkAndPersist()
        }
    }

    fun startDns(onReady: (DnsServer, DnsProtocol) -> Unit) {
        if (_uiState.value.testing) return
        viewModelScope.launch {
            val protocol = _uiState.value.selectedProtocol
            val savedResults = _uiState.value.results.ifEmpty { repository.getLatestResults() }
            val results = if (savedResults.isEmpty()) benchmarkAndPersist() else savedResults
            val best = DnsSelectionPolicy.bestEligible(results, protocol)
            if (best != null) {
                preferences.saveActiveDns(best.server.name)
                onReady(best.server, protocol)
            } else {
                val lastConnected = preferences.getLastConnectedDns()
                val lastServer = lastConnected?.let { saved ->
                    repository.getDnsServers().firstOrNull { it.ip == saved.ip }
                }
                if (lastConnected != null && lastServer != null) {
                    _uiState.update { it.copy(selectedProtocol = lastConnected.protocol) }
                    preferences.saveDnsProtocol(lastConnected.protocol)
                    preferences.saveActiveDns(lastServer.name)
                    onReady(lastServer, lastConnected.protocol)
                }
            }
        }
    }

    private suspend fun benchmarkAndPersist(): List<DnsTestResult> {
        _uiState.update { it.copy(testing = true) }
        return runCatching {
            val results = benchmarkEngine.benchmark(repository.getDnsServers())
            results.firstOrNull()?.let { bestResult ->
                repository.saveBenchmark(
                    session = BenchmarkSessionEntity(
                        sessionId = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        bestDns = bestResult.server.name,
                        bestLatency = bestResult.latency
                    ),
                    results = results
                )
            }
            val recommended = recommendationEngine.getBest(results)
            recommended?.let { preferences.saveActiveDns(it.server.name) }
            _uiState.update {
                it.copy(
                    testing = false,
                    results = results,
                    analytics = repository.getAnalytics(),
                    recommendedDns = recommended?.server?.name ?: "None"
                )
            }
            results
        }.getOrElse {
            _uiState.update { state -> state.copy(testing = false) }
            emptyList()
        }
    }
}

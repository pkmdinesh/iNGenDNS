package com.ingendns.app.ui.dashboard

import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.domain.model.DnsAnalytics
import com.ingendns.app.util.Constants
import com.ingendns.app.network.NetworkState
import com.ingendns.app.vpn.DnsProtocol

data class DashboardUiState(
    val testing: Boolean = false,
    val results: List<DnsTestResult> = emptyList(),
    val activeDns: String = Constants.DEFAULT_ACTIVE_DNS,
    val recommendedDns: String = "None",
    val network: NetworkState = NetworkState(),
    val vpnActive: Boolean = false,
    val vpnResolver: String? = null,
    val vpnProtocol: DnsProtocol? = null,
    val vpnHostname: String? = null,
    val encryptedConnected: Boolean = false,
    val vpnConnectionFailed: Boolean = false,
    val selectedProtocol: DnsProtocol = DnsProtocol.DOT,
    val analytics: DnsAnalytics = DnsAnalytics()
)

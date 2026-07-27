package com.ingendns.app

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ingendns.app.di.AppContainer
import com.ingendns.app.network.NetworkMonitor
import com.ingendns.app.settings.AppSettings
import com.ingendns.app.ui.MainScreen
import com.ingendns.app.ui.dashboard.DashboardViewModel
import com.ingendns.app.ui.dashboard.DashboardViewModelFactory
import com.ingendns.app.ui.history.HistoryViewModel
import com.ingendns.app.ui.history.HistoryViewModelFactory
import com.ingendns.app.ui.settings.SettingsViewModel
import com.ingendns.app.ui.settings.SettingsViewModelFactory
import com.ingendns.app.ui.theme.InGenDNSTheme
import com.ingendns.app.vpn.DnsVpnService
import com.ingendns.app.vpn.DnsProtocol
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.workers.DnsWorkScheduler
import com.ingendns.app.core.logger.AppLogger

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var distributionUpdateManager: DistributionUpdateManager

    private var pendingDns: PendingDns? = null
    private var pendingAutoConnect = false
    private var settingsViewModelRef: SettingsViewModel? = null

    private val vpnPermission =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (pendingAutoConnect) {
                settingsViewModelRef?.setAutoConnectEnabled(result.resultCode == RESULT_OK)
                syncAutoConnectMonitoring()
            } else if (result.resultCode == RESULT_OK) {
                pendingDns?.let(::startVpn)
            }

            pendingDns = null
            pendingAutoConnect = false
        }

    private val updateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                AppLogger.w("Play update flow was canceled or failed: ${result.resultCode}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        distributionUpdateManager = DistributionUpdateManager(this, updateLauncher)
        distributionUpdateManager.checkForUpdate()
        networkMonitor = NetworkMonitor(applicationContext)

        val settings = AppSettings(applicationContext)

        if (settings.autoConnectEnabled && VpnService.prepare(this) == null) {
            DnsWorkScheduler.ensureScheduled(
                applicationContext,
                settings.autoReconnectIntervalHours
            )
        }

        val appContainer = AppContainer(applicationContext)

        val dashboardFactory =
            DashboardViewModelFactory(
                appContainer.dnsRepository,
                appContainer.preferenceManager,
                networkMonitor
            )

        val historyFactory =
            HistoryViewModelFactory(
                appContainer.dnsRepository
            )

        val settingsFactory = SettingsViewModelFactory(
            appContainer.dnsRepository,
            applicationContext
        )

        setContent {

            val dashboardViewModel: DashboardViewModel =
                viewModel(factory = dashboardFactory)

            val historyViewModel: HistoryViewModel =
                viewModel(factory = historyFactory)

            val settingsViewModel: SettingsViewModel =
                viewModel(factory = settingsFactory)
            settingsViewModelRef = settingsViewModel

            val darkModeEnabled by settingsViewModel.darkModeEnabled.collectAsStateWithLifecycle()

            InGenDNSTheme(darkTheme = darkModeEnabled) {
                MainScreen(
                    dashboardViewModel = dashboardViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel,
                    onActivateDns = ::requestDns,
                    onAutoConnectChange = ::requestAutoConnect,
                    onStopDns = { DnsVpnService.stop(this) },
                    onExit = { moveTaskToBack(true) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        networkMonitor.start()
    }

    override fun onStop() {
        networkMonitor.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (::networkMonitor.isInitialized) networkMonitor.refreshNow()
        if (::distributionUpdateManager.isInitialized) {
            distributionUpdateManager.checkForUpdate()
        }
        syncAutoConnectMonitoring()
    }

    private fun requestDns(
        server: DnsServer,
        protocol: DnsProtocol
    ) {
        val endpoint = when (protocol) {
            DnsProtocol.DOT -> server.dotHostname
            DnsProtocol.DOH -> server.dohUrl
        }
        if (endpoint.isBlank()) return
        pendingDns = PendingDns(server.ip, server.name, protocol, endpoint)

        val intent = VpnService.prepare(this)

        if (intent != null) {
            vpnPermission.launch(intent)
        } else {
            startVpn(pendingDns!!)
        }
    }

    private fun requestAutoConnect(enabled: Boolean) {
        if (!enabled) {
            settingsViewModelRef?.setAutoConnectEnabled(false)
            syncAutoConnectMonitoring()
            DnsVpnService.stop(this)
            return
        }

        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent == null) {
            settingsViewModelRef?.setAutoConnectEnabled(true)
            syncAutoConnectMonitoring()
        } else {
            pendingAutoConnect = true
            vpnPermission.launch(permissionIntent)
        }
    }

    private fun syncAutoConnectMonitoring() {
        (application as InGenDNSApplication).syncAutoConnectMonitoring()
    }

    private fun startVpn(dns: PendingDns) {
        DnsVpnService.start(
            this,
            dns.resolver,
            dns.name,
            dns.protocol,
            dns.endpoint
        )
    }

    private data class PendingDns(
        val resolver: String,
        val name: String,
        val protocol: DnsProtocol,
        val endpoint: String
    )
}

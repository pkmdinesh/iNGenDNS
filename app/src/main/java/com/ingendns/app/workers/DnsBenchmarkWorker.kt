package com.ingendns.app.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ingendns.app.database.BenchmarkSessionEntity
import com.ingendns.app.di.AppContainer
import com.ingendns.app.dns.benchmark.DnsBenchmarkEngine
import com.ingendns.app.dns.benchmark.UdpDnsProbe
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.recommendation.DnsSelectionPolicy
import com.ingendns.app.logger.EventLogStore
import com.ingendns.app.vpn.DnsVpnService
import com.ingendns.app.vpn.DnsVpnState
import com.ingendns.app.vpn.DnsProtocol
import com.ingendns.app.settings.AppSettings
import java.util.UUID
import java.util.concurrent.TimeUnit

class DnsBenchmarkWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result = runCatching {
        if (!AppSettings(applicationContext).autoConnectEnabled) return Result.success()
        if (!isValidatedCellular()) {
            EventLogStore(applicationContext).record(
                "Background benchmark skipped",
                "A validated mobile-data connection is required"
            )
            return Result.success()
        }

        val container = AppContainer(applicationContext)
        val repository = container.dnsRepository
        val servers = repository.getDnsServers()
        val vpnPermissionGranted = VpnService.prepare(applicationContext) == null
        val protocol = container.preferenceManager.getDnsProtocol()
        var appliedResolver: String? = null
        var appliedProtocol: DnsProtocol? = null
        val reconnectOnly = inputData.getBoolean(KEY_RECONNECT_ONLY, false)
        val latestResults = repository.getLatestResults()
        val lastConnected = container.preferenceManager.getLastConnectedDns()
        val lastServer = lastConnected?.let { saved -> servers.firstOrNull { it.ip == saved.ip } }

        if (vpnPermissionGranted) {
            val savedBest = DnsSelectionPolicy.bestAutomatic(
                latestResults,
                protocol,
                lastConnected?.ip
            )
            val rankedServer = savedBest?.server?.ip
                ?.let { ip -> servers.firstOrNull { it.ip == ip } }
            val reconnectServer = rankedServer ?: lastServer
            val reconnectProtocol = if (rankedServer != null) protocol else lastConnected?.protocol
            val savedEndpoint = if (reconnectServer != null && reconnectProtocol != null) {
                endpointFor(reconnectServer, reconnectProtocol)
            } else null
            if (reconnectServer != null && reconnectProtocol != null && !savedEndpoint.isNullOrBlank()) {
                val vpn = DnsVpnState.state.value
                if (
                    !vpn.active || vpn.resolver != reconnectServer.ip ||
                    vpn.protocol != reconnectProtocol
                ) {
                    DnsVpnService.start(
                        applicationContext,
                        reconnectServer.ip,
                        reconnectServer.name,
                        reconnectProtocol,
                        savedEndpoint
                    )
                }
                appliedResolver = reconnectServer.ip
                appliedProtocol = reconnectProtocol
                EventLogStore(applicationContext).record(
                    "Background DNS reconnected",
                    "${reconnectServer.name} (${reconnectServer.ip})"
                )
            }
        }
        if (reconnectOnly && appliedResolver != null) {
            // Network handovers only need the last ranked resolver re-applied. The
            // scheduled interval remains responsible for the full benchmark.
            return Result.success()
        }
        if (repository.getLatestResults().isEmpty()) {
            EventLogStore(applicationContext).record(
                "First-run benchmark required",
                "No saved DNS results; testing providers before Auto Connect"
            )
        }
        EventLogStore(applicationContext).record(
            "Background benchmark started",
            "Testing ${servers.size} resolvers"
        )

        val results = DnsBenchmarkEngine(UdpDnsProbe()).benchmark(servers)
        results.firstOrNull()?.let { fastest ->
            val sessionId = UUID.randomUUID().toString()
            repository.saveBenchmark(
                session = BenchmarkSessionEntity(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    bestDns = fastest.server.name,
                    bestLatency = fastest.latency
                ),
                results = results
            )
        }
        val currentIp = DnsVpnState.state.value.resolver ?: lastConnected?.ip
        val automaticBest = DnsSelectionPolicy.bestAutomatic(results, protocol, currentIp)
        val rankedServer = automaticBest?.server?.ip
            ?.let { ip -> servers.firstOrNull { it.ip == ip } }
        val selectedServer = rankedServer ?: lastServer
        val selectedProtocol = if (rankedServer != null) protocol else lastConnected?.protocol
        if (selectedServer != null && selectedProtocol != null) {
            container.preferenceManager.saveActiveDns(selectedServer.name)

            if (vpnPermissionGranted) {
                val endpoint = endpointFor(selectedServer, selectedProtocol)
                if (
                    endpoint.isNotBlank() &&
                    (selectedServer.ip != appliedResolver || selectedProtocol != appliedProtocol)
                ) DnsVpnService.start(
                    applicationContext,
                    selectedServer.ip,
                    selectedServer.name,
                    selectedProtocol,
                    endpoint
                )
                EventLogStore(applicationContext).record(
                    if (automaticBest == null) "Background DNS retained" else "Background DNS applied",
                    "${selectedServer.name} (${selectedServer.ip})" +
                        if (automaticBest == null) " because all scores were below ${DnsSelectionPolicy.MINIMUM_AUTOMATIC_SCORE}" else ""
                )
            } else {
                EventLogStore(applicationContext).record(
                    "Background DNS not applied",
                    "Open the app once and approve the VPN permission"
                )
            }
        } else {
            EventLogStore(applicationContext).record(
                "Background DNS not applied",
                "No DNS reached ${DnsSelectionPolicy.MINIMUM_AUTOMATIC_SCORE} and no last connected DNS is available"
            )
        }

        EventLogStore(applicationContext).record(
            "Background benchmark complete",
            "${results.size} resolvers responded"
        )
        Result.success()
    }.getOrElse { error ->
        EventLogStore(applicationContext).record(
            "Background benchmark failed",
            error.message ?: "Unknown error"
        )
        Result.retry()
    }

    private fun isValidatedCellular(): Boolean {
        val connectivity =
            applicationContext.getSystemService(ConnectivityManager::class.java)
        val capabilities =
            connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun endpointFor(server: DnsServer, protocol: DnsProtocol): String = when (protocol) {
        DnsProtocol.DOT -> server.dotHostname
        DnsProtocol.DOH -> server.dohUrl
    }

    companion object {
        const val KEY_RECONNECT_ONLY = "reconnect_only"
    }
}

object DnsWorkScheduler {
    private const val UNIQUE_WORK_NAME = "periodic_dns_benchmark"
    private const val IMMEDIATE_WORK_NAME = "auto_connect_dns_now"

    fun schedule(context: Context, intervalHours: Int) {
        ensureScheduled(context, intervalHours)
        runNow(context)
        val interval = intervalHours.coerceIn(1, 12)
        EventLogStore(context).record(
            "Auto Connect enabled",
            "Mobile-data benchmark and auto-apply every $interval hour(s)"
        )
    }

    fun ensureScheduled(context: Context, intervalHours: Int) {
        val interval = intervalHours.coerceIn(1, 12)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<DnsBenchmarkWorker>(interval.toLong(), TimeUnit.HOURS)
            .setConstraints(
                constraints
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun runNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DnsBenchmarkWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(DnsBenchmarkWorker.KEY_RECONNECT_ONLY to true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
        EventLogStore(context).record("Auto Connect disabled")
    }
}

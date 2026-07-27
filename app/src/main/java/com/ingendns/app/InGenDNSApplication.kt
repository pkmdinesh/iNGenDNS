package com.ingendns.app

import android.app.Application
import com.ingendns.app.di.AppContainer
import com.ingendns.app.network.AutoConnectNetworkCoordinator
import com.ingendns.app.logger.CrashReporter
import com.ingendns.app.settings.AppSettings
import com.ingendns.app.vpn.DnsVpnService
import com.ingendns.app.workers.DnsHealthScheduler

class InGenDNSApplication : Application() {

    lateinit var container: AppContainer
        private set
    private lateinit var autoConnectCoordinator: AutoConnectNetworkCoordinator

    override fun onCreate() {
        super.onCreate()

        CrashReporter.install(this)
        container = AppContainer(this)
        DnsVpnService.removeObsoleteNotificationChannel(this)
        autoConnectCoordinator = AutoConnectNetworkCoordinator(this)
        syncAutoConnectMonitoring()
        // The VPN already detects failures from real DNS traffic. Remove the legacy
        // 30-minute polling job so idle devices stay asleep.
        DnsHealthScheduler.cancel(this)
    }

    fun syncAutoConnectMonitoring() {
        if (AppSettings(this).autoConnectEnabled) autoConnectCoordinator.start()
        else autoConnectCoordinator.stop()
    }
}

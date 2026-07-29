package com.ingendns.app.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ingendns.app.R
import com.ingendns.app.ui.dashboard.DashboardScreen
import com.ingendns.app.ui.dashboard.DashboardViewModel
import com.ingendns.app.ui.faq.FaqScreen
import com.ingendns.app.ui.history.HistoryScreen
import com.ingendns.app.ui.history.HistoryViewModel
import com.ingendns.app.ui.settings.SettingsScreen
import com.ingendns.app.ui.settings.SettingsViewModel
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.vpn.DnsProtocol
import kotlinx.coroutines.launch

private enum class Destination(val title: String) {
    DASHBOARD("Dashboard"),
    HISTORY("History"),
    SETTINGS("Settings"),
    FAQS("FAQs"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dashboardViewModel: DashboardViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    onActivateDns: (DnsServer, DnsProtocol) -> Unit,
    onAutoConnectChange: (Boolean) -> Unit,
    onStopDns: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onExit: () -> Unit
) {
    var destination by remember { mutableStateOf(Destination.DASHBOARD) }
    val autoConnectEnabled by settingsViewModel.autoConnectEnabled.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun select(item: Destination) {
        destination = item
        scope.launch { drawerState.close() }
    }

    BackHandler(
        enabled = drawerState.isOpen || destination != Destination.DASHBOARD
    ) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            destination = Destination.DASHBOARD
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "iNGenDNS",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                Destination.entries.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title) },
                        selected = destination == item,
                        onClick = { select(item) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Exit") },
                    selected = false,
                    onClick = onExit,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            destination.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = "Open navigation menu"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)
                ) {
                    when (destination) {
                    Destination.DASHBOARD -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        autoConnectEnabled = autoConnectEnabled,
                        onAutoConnectChange = onAutoConnectChange,
                        onActivateDns = onActivateDns,
                        onStopDns = onStopDns,
                        onOpenVpnSettings = onOpenVpnSettings
                    )
                    Destination.HISTORY -> HistoryScreen(historyViewModel)
                    Destination.SETTINGS -> SettingsScreen(
                        settingsViewModel,
                        onAutoConnectChange
                    )
                    Destination.FAQS -> FaqScreen()
                    Destination.ABOUT -> AboutScreen(onCheckForUpdates)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AboutScreen(onCheckForUpdates: () -> Unit) {
    val context = LocalContext.current
    val appVersionName = remember(context) { context.appVersionName() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground_logo),
                contentDescription = "iNGenDNS logo",
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.widthIn(min = 12.dp))
            Text("iNGenDNS", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "App version: $appVersionName",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onCheckForUpdates) {
            Text("Check for updates")
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "iNGenDNS analyzes DNS performance and helps you connect to a fast, reliable encrypted DNS provider using DNS over HTTPS or DNS over TLS. Optional automatic connection and failover features can help improve connection stability. Filtering features, including ad blocking, depend on the selected DNS provider.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(20.dp))
        Text("APP Designed by", style = MaterialTheme.typography.labelLarge)
        Text("Dinesh K", style = MaterialTheme.typography.titleSmall)
        Text("Support: pkmdinesh@outlook.com", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        Text(
            "Built with Kotlin, Android Jetpack and Android VPN Service.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Developed with the assistance of ChatGPT by OpenAI.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(24.dp))
        Text("Privacy policy", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "VPN permission is requested only when you enable encrypted DNS or automatic connection. Android requires this consent because iNGenDNS creates a local VPN interface to route DNS requests and to connect the DNS provider you select. Other internet traffic is managed by the default Network Service Provider.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "iNGenDNS does not collect or store your browsing history. DNS requests are processed in memory and forwarded to your selected DNS provider; that provider's own privacy policy applies. The app stores DNS performance results, settings, and a limited diagnostic event log locally on your device. This information is not sold or used for advertising.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "\u00A9 2026 Dinesh K. All rights reserved.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun Context.appVersionName(): String = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    info.versionName?.removeSuffix("-fdroid") ?: "Unknown"
}.getOrDefault("Unknown")

package com.ingendns.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingendns.app.dns.benchmark.DnsQualityScore
import com.ingendns.app.dns.model.DefaultDnsServers
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.recommendation.DnsSelectionPolicy.supports
import com.ingendns.app.network.CellularSignal
import com.ingendns.app.network.Transport
import com.ingendns.app.ui.dashboard.components.AnalyticsCard
import com.ingendns.app.ui.dashboard.components.AnalyticsRow
import com.ingendns.app.vpn.DnsProtocol
import com.ingendns.app.vpn.VpnOperatingMode

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    autoConnectEnabled: Boolean,
    onAutoConnectChange: (Boolean) -> Unit,
    onActivateDns: (DnsServer, DnsProtocol) -> Unit,
    onStopDns: () -> Unit,
    onOpenVpnSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val systemIp = state.network.dnsServers.firstOrNull()
    val systemProfile = systemIp?.let(DefaultDnsServers::findByIp)
    val activeName = if (state.vpnActive) state.activeDns else systemProfile?.name
        ?: state.network.privateDnsHostname ?: "Network DNS"
    val activeIp = if (state.vpnActive) state.vpnResolver else systemIp
    val encryption = when {
        state.vpnActive && state.vpnMode == VpnOperatingMode.WIFI_NETWORK_DNS -> "Unencrypted"
        state.vpnActive -> state.vpnProtocol?.name ?: "Unknown"
        state.network.privateDnsActive -> "DOT"
        else -> "Unencrypted"
    }
    val connectionStatus = if (state.vpnActive) {
        when {
            state.encryptedConnected || !state.vpnConnectionFailed -> "Connected"
            state.vpnConnectionFailed -> "Failed"
            else -> "Connecting"
        }
    } else if (state.network.hasInternet) "Connected" else "Disconnected"
    val vpnModeText = when (state.vpnMode) {
        VpnOperatingMode.CELLULAR_OPTIMIZED -> "Cellular optimized"
        VpnOperatingMode.WIFI_NETWORK_DNS -> "Wi-Fi network DNS"
        VpnOperatingMode.WAITING_FOR_NETWORK -> state.vpnStatusMessage
        VpnOperatingMode.INACTIVE -> "Inactive"
    }
    val successColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)

    fun selectProtocol(protocol: DnsProtocol) {
        viewModel.setProtocol(protocol)
        if (state.vpnActive) {
            val activeServer = state.results.firstOrNull {
                it.server.ip == state.vpnResolver
            }?.server ?: state.vpnResolver?.let(DefaultDnsServers::findByIp)
            if (activeServer?.supports(protocol) == true) {
                onActivateDns(activeServer, protocol)
            }
        }
    }

    if (state.vpnLockdownEnabled && state.vpnConnectionFailed) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("VPN lockdown is blocking connectivity") },
            text = { Text(state.vpnStatusMessage) },
            confirmButton = {
                TextButton(onClick = onOpenVpnSettings) { Text("Open VPN Settings") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val networkColor = if (state.network.hasInternet) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NetworkTransportIcon(
                                transport = state.network.transport,
                                connected = state.network.hasInternet,
                                color = networkColor
                            )
                            Text(
                                text = when (state.network.transport) {
                                    Transport.WIFI -> "Wi-Fi"
                                    Transport.CELLULAR -> "Mobile Data"
                                    Transport.NONE -> "No Internet"
                                },
                                color = networkColor,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = buildString {
                                state.network.networkName?.let(::append)
                                if (!state.network.hasInternet && state.network.transport != Transport.NONE) {
                                    if (isNotEmpty()) append(" · ")
                                    append("No Internet")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            color = networkColor,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.network.transport == Transport.CELLULAR) {
                        CompactSignalRow(
                            label = "Signal 4G",
                            signal = state.network.lteSignal
                        )
                        CompactSignalRow(
                            label = "Signal 5G",
                            signal = state.network.nrSignal
                        )
                        CompactNetworkDetailRow(
                            "Connected Network",
                            state.network.connectedNetwork ?: "Mobile Data"
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    AnalyticsRow("Active DNS", activeName)
                    AnalyticsRow("IP", activeIp ?: "Not available")
                    ColoredStatusRow(
                        "Status",
                        connectionStatus,
                        if (connectionStatus == "Connected") successColor else Color.Red
                    )
                    ColoredStatusRow(
                        "Connection",
                        if (encryption == "Unencrypted") "Unencrypted" else "Encrypted ($encryption)",
                        if (encryption == "Unencrypted") Color(0xFFF57C00) else successColor
                    )
                    Text(
                        text = "VPN mode: $vpnModeText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.vpnConnectionFailed) Color.Red
                            else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Encryption Type", style = MaterialTheme.typography.titleSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedProtocol == DnsProtocol.DOT,
                            onClick = { selectProtocol(DnsProtocol.DOT) },
                            label = { Text("DOT") }
                        )
                        FilterChip(
                            selected = state.selectedProtocol == DnsProtocol.DOH,
                            onClick = { selectProtocol(DnsProtocol.DOH) },
                            label = { Text("DOH") }
                        )
                    }
                    Text(
                        "Selected: ${state.selectedProtocol.name}",
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Auto Connect",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.End
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (autoConnectEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (autoConnectEnabled) successColor else Color(0xFFF57C00)
                        )
                        Switch(
                            checked = autoConnectEnabled,
                            onCheckedChange = onAutoConnectChange
                        )
                    }
                }
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Note: ")
                    }
                    append("DOH may consume slightly more battery.")
                },
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFF57C00)
            )
        }

        item { AnalyticsCard(analytics = state.analytics) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::runBenchmark,
                    enabled = !state.testing && state.network.hasInternet,
                    modifier = Modifier.weight(1f)
                ) { Text(if (state.testing) "Testing…" else "Benchmark") }

                if (state.vpnActive) {
                    OutlinedButton(
                        onClick = onStopDns,
                        enabled = !state.vpnAlwaysOnEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.vpnAlwaysOnEnabled) "Always-on" else "Stop DNS")
                    }
                } else {
                    Button(
                        onClick = { viewModel.startDns(onActivateDns) },
                        enabled = !state.testing && state.network.hasInternet &&
                            state.network.transport == Transport.CELLULAR,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (state.testing) "Testing…" else "Start DNS") }
                }
            }
            Text(
                text = "Note: To start manually click Apply or Start DNS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (state.results.isEmpty()) {
            item {
                Text(
                    "Run a benchmark to compare DNS providers.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            item { Text("DNS Benchmark", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(
                state.results,
                key = { index, result -> "$index-${result.server.ip}-${result.timestamp}" }
            ) { _, result ->
                val latencyText = if (result.reachable && result.latency != Long.MAX_VALUE) {
                    "${result.latency} ms"
                } else {
                    "Timeout"
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(result.server.name, style = MaterialTheme.typography.titleSmall)
                            Button(
                                onClick = { onActivateDns(result.server, state.selectedProtocol) },
                                enabled = result.server.supports(state.selectedProtocol) &&
                                    state.network.hasInternet && state.network.transport == Transport.CELLULAR &&
                                    !(state.vpnActive && state.vpnResolver == result.server.ip)
                            ) {
                                Text(if (state.vpnActive && state.vpnResolver == result.server.ip) "Applied" else "Apply")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(result.server.ip, style = MaterialTheme.typography.bodyMedium)
                            Text(latencyText, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${result.score}/100", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                DnsQualityScore.rating(result.score),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            item { BenchmarkCalculationGuide() }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun BenchmarkCalculationGuide() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("How the DNS score is calculated", style = MaterialTheme.typography.titleSmall)
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        ParagraphStyle(
                            textIndent = TextIndent(firstLine = 0.sp, restLine = 16.sp)
                        )
                    ) {
                        append("DNS Score = 40% latency + 25% reliability + 20% success + 10% jitter + 5% packet loss")
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text("Latency points = 100 − (average ms × 0.4)", style = MaterialTheme.typography.bodySmall)
            Text("Jitter points = 100 − (jitter ms × 5)", style = MaterialTheme.typography.bodySmall)
            Text("Packet-loss points = 100 − loss %", style = MaterialTheme.typography.bodySmall)
            Text(
                "Reliability is the percentage of responses close to the median.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkTransportIcon(
    transport: Transport,
    connected: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(24.dp)) {
        val stroke = size.minDimension * 0.09f
        when (transport) {
            Transport.CELLULAR -> {
                val barWidth = size.width * 0.14f
                val gap = size.width * 0.07f
                repeat(4) { index ->
                    val height = size.height * (0.22f + index * 0.18f)
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            size.width * 0.12f + index * (barWidth + gap),
                            size.height * 0.88f - height
                        ),
                        size = androidx.compose.ui.geometry.Size(barWidth, height)
                    )
                }
            }
            Transport.WIFI -> {
                val arcStroke = Stroke(width = stroke, cap = StrokeCap.Round)
                drawArc(
                    color = color,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.08f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.84f),
                    style = arcStroke
                )
                drawArc(
                    color = color,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.27f, size.height * 0.30f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.46f, size.height * 0.46f),
                    style = arcStroke
                )
                drawCircle(
                    color = color,
                    radius = stroke * 0.8f,
                    center = center.copy(y = size.height * 0.78f)
                )
            }
            Transport.NONE -> drawCircle(
                color = color,
                radius = size.minDimension * 0.32f,
                style = Stroke(width = stroke)
            )
        }
        if (!connected) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.12f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.88f),
                strokeWidth = stroke * 1.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ColoredStatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Surface(
            color = color.copy(alpha = 0.14f),
            contentColor = color,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CompactNetworkDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactSignalRow(label: String, signal: CellularSignal?) {
    val darkTheme = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.32f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Text(
            text = if (signal == null) {
                buildAnnotatedString { append("Not available") }
            } else {
                buildAnnotatedString {
                    append("${signal.technology}  ${signal.dbm} dBm  ${signal.asu} asu  ")
                    withStyle(
                        SpanStyle(
                            color = when (signal.quality) {
                                "Excellent" -> if (darkTheme) Color(0xFF81C784)
                                    else Color(0xFF2E7D32)
                                "Good" -> if (darkTheme) Color(0xFFA5D6A7)
                                    else Color(0xFF388E3C)
                                "Fair" -> Color(0xFFF57C00)
                                else -> Color.Red
                            },
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(signal.quality)
                    }
                }
            },
            modifier = Modifier.weight(0.68f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

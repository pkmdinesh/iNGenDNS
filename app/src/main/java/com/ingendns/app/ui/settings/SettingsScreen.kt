package com.ingendns.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ingendns.app.dns.model.DnsServer

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAutoConnectChange: (Boolean) -> Unit
) {
    val dnsServers by viewModel.dnsServers.collectAsStateWithLifecycle()
    val orderedDnsServers = dnsServers.sortedByDescending { it.isCustom }
    val autoConnectEnabled by viewModel.autoConnectEnabled.collectAsStateWithLifecycle()
    val reconnectInterval by viewModel.reconnectInterval.collectAsStateWithLifecycle()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dotHostname by remember { mutableStateOf("") }
    var dohUrl by remember { mutableStateOf("") }
    var editingServer by remember { mutableStateOf<DnsServer?>(null) }
    var pendingRemoval by remember { mutableStateOf<DnsServer?>(null) }

    pendingRemoval?.let { server ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove custom profile?") },
            text = { Text("${server.name} will be permanently removed from Saved Profiles.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeServer(server)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (darkModeEnabled) "Dark mode" else "Light mode",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Use the ${if (darkModeEnabled) "dark" else "light"} app theme.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = viewModel::setDarkModeEnabled
                )
            }
        }

        item {
            Text("Auto Connect", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "On Mobile Data, Auto Reconnect as per below Interval & Apply the Fastest DNS",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = autoConnectEnabled,
                    onCheckedChange = onAutoConnectChange
                )
            }
            Text(
                "Auto reconnect interval: $reconnectInterval hour(s)",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = reconnectInterval.toFloat(),
                onValueChange = { viewModel.setReconnectInterval(it.toInt().coerceIn(1, 12)) },
                valueRange = 1f..12f,
                steps = 10
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("DNS Profiles", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add a custom profile or select Edit below to update a default profile.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text("Name", style = MaterialTheme.typography.bodySmall) },
                singleLine = true
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text("IPv4 address", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("1.1.1.1", style = MaterialTheme.typography.bodySmall) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                value = dotHostname,
                onValueChange = { dotHostname = it.filterNot(Char::isWhitespace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text("DoT hostname", style = MaterialTheme.typography.bodySmall) },
                placeholder = {
                    Text("dns.example.com", style = MaterialTheme.typography.bodySmall)
                },
                singleLine = true
            )
            OutlinedTextField(
                value = dohUrl,
                onValueChange = { dohUrl = it.filterNot(Char::isWhitespace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = {
                    Text("DoH URL (optional)", style = MaterialTheme.typography.bodySmall)
                },
                placeholder = {
                    Text(
                        "https://dns.example.com/dns-query",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true
            )
            Button(
                onClick = {
                    val saved = editingServer?.let {
                        viewModel.updateServer(it, name, address, dotHostname, dohUrl)
                    } ?: viewModel.addServer(name, address, dotHostname, dohUrl)
                    if (saved) {
                        name = ""
                        address = ""
                        dotHostname = ""
                        dohUrl = ""
                        editingServer = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        editingServer == null -> "Add Custom DNS"
                        editingServer?.isCustom == false -> "Update Default Profile"
                        else -> "Update Custom Profile"
                    }
                )
            }
            if (editingServer != null) {
                OutlinedButton(
                    onClick = {
                        editingServer = null
                        name = ""
                        address = ""
                        dotHostname = ""
                        dohUrl = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel edit") }
            }
            message?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item { Text("Saved Profiles", style = MaterialTheme.typography.titleMedium) }

        items(orderedDnsServers, key = { it.profileId ?: "custom-${it.ip}" }) { server ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            server.name,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (server.isCustom) "Custom Profile" else "Default Profile",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            server.ip,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                editingServer = server
                                name = server.name
                                address = server.ip
                                dotHostname = server.dotHostname
                                dohUrl = server.dohUrl
                            }) { Text("Edit") }
                            if (server.isCustom) {
                                OutlinedButton(onClick = { pendingRemoval = server }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                    Text(
                        "DoT: ${server.dotHostname.ifBlank { "Not configured" }}",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true
                    )
                    Text(
                        "DoH: ${server.dohUrl.ifBlank { "Not configured" }}",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

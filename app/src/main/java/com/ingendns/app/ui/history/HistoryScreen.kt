package com.ingendns.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ingendns.app.dns.benchmark.DnsQualityScore
import com.ingendns.app.dns.model.DnsTestResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val sessions = remember(history) {
        history
            .groupBy { result -> result.sessionId.ifBlank { "legacy-${result.timestamp}" } }
            .map { (id, results) ->
                HistorySession(
                    id = id,
                    timestamp = results.maxOf { it.timestamp },
                    results = results.sortedWith(
                        compareByDescending<DnsTestResult> { it.score }.thenBy { it.latency }
                    )
                )
            }
            .sortedByDescending { it.timestamp }
    }
    val dates = remember(sessions) { sessions.map { sessionDate(it.timestamp) }.distinct() }
    var selectedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedDate = selectedEpochDay?.let(LocalDate::ofEpochDay)
    val dateSessions = remember(sessions, selectedDate) {
        sessions.filter { selectedDate != null && sessionDate(it.timestamp) == selectedDate }
    }
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(dates) {
        if (selectedDate !in dates) selectedEpochDay = dates.firstOrNull()?.toEpochDay()
    }
    LaunchedEffect(dateSessions) {
        if (dateSessions.none { it.id == selectedSessionId }) {
            selectedSessionId = dateSessions.firstOrNull()?.id
        }
    }
    val selectedSession = dateSessions.firstOrNull { it.id == selectedSessionId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("DNS Test History", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Select a date and benchmark session from the last 30 days",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (sessions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No benchmark history yet. Run a benchmark to start comparing DNS performance.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            item {
                HistorySelectors(
                    dates = dates,
                    selectedDate = selectedDate,
                    sessions = dateSessions,
                    selectedSessionId = selectedSessionId,
                    onDateSelected = { date -> selectedEpochDay = date.toEpochDay() },
                    onSessionSelected = { id -> selectedSessionId = id }
                )
            }
            selectedSession?.let { session ->
                item(key = session.id) { HistorySessionCard(session) }
            }
        }
    }
}

@Composable
private fun HistorySelectors(
    dates: List<LocalDate>,
    selectedDate: LocalDate?,
    sessions: List<HistorySession>,
    selectedSessionId: String?,
    onDateSelected: (LocalDate) -> Unit,
    onSessionSelected: (String) -> Unit
) {
    var dateMenuExpanded by remember { mutableStateOf(false) }
    var sessionMenuExpanded by remember { mutableStateOf(false) }
    val selectedSession = sessions.firstOrNull { it.id == selectedSessionId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { dateMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Date: ${selectedDate?.let(::dateLabel) ?: "Select date"}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = dateMenuExpanded,
                onDismissRequest = { dateMenuExpanded = false }
            ) {
                dates.forEach { date ->
                    DropdownMenuItem(
                        text = { Text(dateLabel(date)) },
                        onClick = {
                            onDateSelected(date)
                            dateMenuExpanded = false
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { sessionMenuExpanded = true },
                enabled = sessions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Session: ${selectedSession?.let { sessionTimeLabel(it.timestamp) } ?: "Select session"}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = sessionMenuExpanded,
                onDismissRequest = { sessionMenuExpanded = false }
            ) {
                sessions.forEachIndexed { index, session ->
                    DropdownMenuItem(
                        text = { Text("Session ${index + 1} · ${sessionTimeLabel(session.timestamp)}") },
                        onClick = {
                            onSessionSelected(session.id)
                            sessionMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySessionCard(session: HistorySession) {
    val best = session.results.first()
    val reachableCount = session.results.count { it.reachable }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dateLabel(sessionDate(session.timestamp)),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    sessionTimeLabel(session.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HistoryChip("$reachableCount/${session.results.size} reachable")
                HistoryChip("Top: ${DnsQualityScore.rating(best.score)}")
            }
            Text(
                "${session.results.size} DNS providers · Top DNS: ${best.server.name}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            session.results.forEachIndexed { index, result ->
                HistoryResultRow(rank = index + 1, result = result)
                if (index != session.results.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryResultRow(rank: Int, result: DnsTestResult) {
    val latency = if (result.reachable && result.latency != Long.MAX_VALUE) {
        "${result.latency} ms"
    } else {
        "Timeout"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "#$rank",
            color = if (rank == 1) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.server.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    result.server.ip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(latency, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${result.score}/100", style = MaterialTheme.typography.bodySmall)
                Text(
                    DnsQualityScore.rating(result.score),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun sessionDate(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now(ZoneId.systemDefault())
    val relative = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> null
    }
    val formatted = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault()))
    return if (relative == null) formatted else "$relative · $formatted"
}

private fun sessionTimeLabel(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.getDefault()))

private data class HistorySession(
    val id: String,
    val timestamp: Long,
    val results: List<DnsTestResult>
)

package com.ingendns.app.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ingendns.app.dns.benchmark.DnsQualityScore
import com.ingendns.app.domain.model.DnsAnalytics

@Composable
fun AnalyticsCard(analytics: DnsAnalytics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                "DNS Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            AnalyticsRow("Fastest DNS", analytics.bestDns, compact = true)
            AnalyticsRow("IP Address", analytics.ipAddress, compact = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactMetric(
                    "Score",
                    "${analytics.score}/100 · ${DnsQualityScore.rating(analytics.score)}",
                    Modifier.weight(1f)
                )
                CompactMetric("Latency", "${analytics.averageLatency} ms avg", Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactMetric("Best", "${analytics.lowestLatency} ms", Modifier.weight(1f))
                CompactMetric("Jitter", "${analytics.jitter} ms", Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            CompactProgress("Success", analytics.successRate)
            CompactProgress("Reliability", analytics.reliability)
            CompactProgress("Packet loss", analytics.packetLoss, inverse = true)
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompactProgress(label: String, percent: Float, inverse: Boolean = false) {
    val normalized = (percent / 100f).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, modifier = Modifier.weight(0.28f), style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { if (inverse) 1f - normalized else normalized },
            modifier = Modifier.weight(0.52f)
        )
        Text("${percent.toInt()}%", modifier = Modifier.weight(0.2f),
            style = MaterialTheme.typography.labelSmall)
    }
}

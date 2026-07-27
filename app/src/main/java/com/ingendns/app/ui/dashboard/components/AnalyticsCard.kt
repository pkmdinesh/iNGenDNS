package com.ingendns.app.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
            AnalyticsRow("Rating", DnsQualityScore.rating(analytics.score), compact = true)
            AnalyticsRow("Fastest DNS", analytics.bestDns, compact = true)
            AnalyticsRow("IP Address", analytics.ipAddress, compact = true)
            AnalyticsRow("DNS Score", "${analytics.score}/100", compact = true)
            AnalyticsRow("Average Latency", "${analytics.averageLatency} ms", compact = true)
            AnalyticsRow("Lowest Latency", "${analytics.lowestLatency} ms", compact = true)
            AnalyticsRow("Jitter", "${analytics.jitter} ms", compact = true)
            AnalyticsRow("Success Rate", "${analytics.successRate.toInt()}%", compact = true)
            AnalyticsRow("Reliability", "${analytics.reliability.toInt()}%", compact = true)
            AnalyticsRow("Packet Loss", "${analytics.packetLoss.toInt()}%", compact = true)
        }
    }
}

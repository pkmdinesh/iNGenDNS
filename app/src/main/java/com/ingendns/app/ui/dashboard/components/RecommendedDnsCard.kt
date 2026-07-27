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

@Composable
fun RecommendedDnsCard(
    dns: String,
    latency: Long?,
    successRate: Float?
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Recommended DNS",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = dns,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            if (latency != null) {
                Text("Latency: ${latency} ms")
            }

            if (successRate != null) {
                Text("Reliability: ${successRate.toInt()}%")
            }
        }
    }
}
package com.ingendns.app.ui.faq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FaqScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium)
            Text(
                "Short answers about DNS, VPN-DNS, speed, ad blocking, and benchmarks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(FAQ_ITEMS, key = FaqItem::question) { item ->
            FaqCard(item)
        }
    }
}

@Composable
private fun FaqCard(item: FaqItem) {
    var expanded by rememberSaveable(item.question) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    item.question,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (expanded) "−" else "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.answer.forEach { paragraph ->
                        Text(paragraph, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private data class FaqItem(
    val question: String,
    val answer: List<String>
)

private val FAQ_ITEMS = listOf(
    FaqItem(
        question = "1. What are DNS and VPN-DNS?",
        answer = listOf(
            "DNS (Domain Name System) is the internet's phonebook. It translates names such as google.com into machine-readable IP addresses.",
            "VPN-DNS is DNS handled through a VPN or another encrypted DNS connection. It protects DNS lookups, but a DNS-only VPN does not encrypt all other internet traffic like a full VPN."
        )
    ),
    FaqItem(
        question = "2. What can changing DNS improve?",
        answer = listOf(
            "• Faster website lookup: A responsive DNS server may reduce the time before a website starts loading.",
            "• More reliable browsing: A public DNS can help when the ISP's DNS is slow or unreliable.",
            "• DNS routing issues: It may help when an ISP blocks or misroutes DNS queries."
        )
    ),
    FaqItem(
        question = "3. What does changing DNS not improve?",
        answer = listOf(
            "• It does not improve 4G or 5G signal strength.",
            "• It does not increase download or upload bandwidth.",
            "Bottom line: Public DNS may make websites begin loading slightly faster, but it generally does not increase mobile internet speed."
        )
    ),
    FaqItem(
        question = "4. What is the difference between VPN and VPN-DNS?",
        answer = listOf(
            "A full VPN encrypts and routes all supported internet traffic through a secure tunnel. VPN-DNS handles only the DNS requests that translate website names into IP addresses.",
            "A full VPN is more likely to reduce speed because of encryption and routing overhead, unless it avoids a routing or throttling problem."
        )
    ),
    FaqItem(
        question = "5. Will ad-blocking DNS increase mobile internet speed?",
        answer = listOf(
            "Ad-blocking DNS services such as AdGuard DNS or NextDNS do not increase your actual mobile bandwidth, but they can make some pages feel faster.",
            "• Pages may load faster because blocked ad servers are never contacted.",
            "• Apps and websites may download less advertising, tracking, and analytics data.",
            "• Rendering fewer ads may reduce CPU and memory usage.",
            "Data savings vary by usage. DNS-based ad blocking may reduce mobile data use by roughly 5–30%, and sometimes more on ad-heavy sites or apps."
        )
    ),
    FaqItem(
        question = "6. What are DNS benchmark metrics?",
        answer = listOf(
            "The DNS Health Score combines several measurements instead of relying on latency alone:",
            "• Latency (ms): Time required to receive a DNS response. Lower is better.",
            "• Success rate (%): Percentage of queries that receive a response. Higher is better.",
            "• Reliability (%): Stability across repeated tests. Higher is better.",
            "• Jitter (ms): Variation between response times. Lower is better.",
            "• Packet loss (%): Queries that fail to receive a response. Lower is better.",
            "• Response grade: A simple summary such as A+, A, or B. Higher is better."
        )
    )
)

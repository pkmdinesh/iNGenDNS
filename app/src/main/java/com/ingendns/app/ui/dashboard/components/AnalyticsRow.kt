package com.ingendns.app.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AnalyticsRow(

    title: String,

    value: String,

    compact: Boolean = false

) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 1.dp else 4.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween

    ) {

        Text(
            text = title,
            style = if (compact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            }
        )

        Text(

            value,

            style = if (compact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            },

            fontWeight = FontWeight.SemiBold,

            color = MaterialTheme.colorScheme.primary
        )
    }
}

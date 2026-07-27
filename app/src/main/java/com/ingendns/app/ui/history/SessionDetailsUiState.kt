package com.ingendns.app.ui.history

import com.ingendns.app.dns.model.DnsTestResult

data class SessionDetailsUiState(

    val sessionId: String = "",

    val timestamp: Long = 0L,

    val results: List<DnsTestResult> = emptyList()
)
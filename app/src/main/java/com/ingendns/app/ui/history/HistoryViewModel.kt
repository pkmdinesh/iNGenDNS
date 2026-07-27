package com.ingendns.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.domain.repository.DnsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: DnsRepository) : ViewModel() {
    private val cutoff = System.currentTimeMillis() - RETENTION_MILLIS

    val history: StateFlow<List<DnsTestResult>> =
        repository.observeRecentResults(cutoff).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    init {
        viewModelScope.launch { repository.purgeHistoryOlderThan(cutoff) }
    }

    private companion object {
        const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1_000
    }
}

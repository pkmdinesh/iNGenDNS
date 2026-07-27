package com.ingendns.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ingendns.app.domain.repository.DnsRepository

class HistoryViewModelFactory(
    private val repository: DnsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel")
    }
}
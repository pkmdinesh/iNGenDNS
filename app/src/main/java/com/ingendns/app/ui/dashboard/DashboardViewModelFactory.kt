package com.ingendns.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ingendns.app.domain.repository.DnsRepository
import com.ingendns.app.preferences.PreferenceManager
import com.ingendns.app.network.NetworkMonitor

class DashboardViewModelFactory(
    private val repository: DnsRepository,
    private val preferences: PreferenceManager,
    private val networkMonitor: NetworkMonitor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository, preferences, networkMonitor) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

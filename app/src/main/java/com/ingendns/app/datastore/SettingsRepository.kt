package com.ingendns.app.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ingendns.app.core.constants.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "settings"
)

class SettingsRepository(
    private val context: Context
) {

    val dnsInterval: Flow<Long> =
        context.dataStore.data.map {

            it[SettingsKeys.DNS_INTERVAL]
                ?: AppConstants.DEFAULT_DNS_TEST_INTERVAL

        }

    suspend fun setDnsInterval(
        value: Long
    ) {

        context.dataStore.edit {

            it[SettingsKeys.DNS_INTERVAL] = value

        }

    }

}
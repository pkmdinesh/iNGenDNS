package com.ingendns.app.di

import android.content.Context
import com.ingendns.app.data.repository.DnsRepositoryImpl
import com.ingendns.app.database.DatabaseProvider
import com.ingendns.app.preferences.PreferenceManager

class AppContainer(context: Context) {

    private val database =
        DatabaseProvider.getDatabase(context)

    private val dao =
        database.dnsDao()

    val preferenceManager =
        PreferenceManager(context)

    val dnsRepository =
        DnsRepositoryImpl(dao)
}
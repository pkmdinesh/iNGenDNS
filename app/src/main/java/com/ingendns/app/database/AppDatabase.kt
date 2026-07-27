package com.ingendns.app.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DnsEntity::class,
        DefaultDnsOverrideEntity::class,
        DnsResultEntity::class,
        BenchmarkSessionEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dnsDao(): DnsDao
}

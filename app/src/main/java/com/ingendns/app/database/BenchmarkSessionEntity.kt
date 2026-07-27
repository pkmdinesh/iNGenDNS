package com.ingendns.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmark_sessions")
data class BenchmarkSessionEntity(

    @PrimaryKey
    val sessionId: String,

    val timestamp: Long,

    val bestDns: String,

    val bestLatency: Long
)
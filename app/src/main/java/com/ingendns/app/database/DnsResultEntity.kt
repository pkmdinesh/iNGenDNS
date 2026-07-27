package com.ingendns.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "dns_results",
    indices = [Index(value = ["sessionId", "ip"], unique = true)]
)
data class DnsResultEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val sessionId: String,

    val name: String,

    val ip: String,

    val latency: Long,

    val successRate: Float,

    val timestamp: Long,

    @ColumnInfo(defaultValue = "0") val lowestLatency: Long = latency,

    @ColumnInfo(defaultValue = "0") val jitter: Long = 0,

    @ColumnInfo(defaultValue = "0") val reliability: Float = successRate,

    @ColumnInfo(defaultValue = "0") val packetLoss: Float = 100f - successRate,

    @ColumnInfo(defaultValue = "0") val score: Int = 0


)

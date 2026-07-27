package com.ingendns.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "dns_servers",
    indices = [Index(value = ["ip"], unique = true)]
)
data class DnsEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val ip: String,

    val hostname: String,

    val custom: Boolean,

    @ColumnInfo(defaultValue = "''")
    val dohUrl: String = ""

)

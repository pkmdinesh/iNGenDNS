package com.ingendns.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** User edits for an immutable built-in profile slot. */
@Entity(
    tableName = "default_dns_overrides",
    indices = [Index(value = ["ip"], unique = true)]
)
data class DefaultDnsOverrideEntity(
    @PrimaryKey val profileId: String,
    val name: String,
    val ip: String,
    val dotHostname: String,
    val dohUrl: String
)

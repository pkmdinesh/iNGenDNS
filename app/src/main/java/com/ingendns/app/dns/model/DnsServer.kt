package com.ingendns.app.dns.model

data class DnsServer(

    val name: String,

    val ip: String,

    val hostname: String = "",

    val isCustom: Boolean = false,

    val dotHostname: String = "",

    val dohUrl: String = "",

    /** Stable identity for a non-removable built-in profile slot. */
    val profileId: String? = null

)

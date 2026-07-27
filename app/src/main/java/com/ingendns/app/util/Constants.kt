package com.ingendns.app.util

object Constants {

    // DNS

    const val DNS_PORT = 53

    const val DNS_TIMEOUT_MS = 3000

    const val DNS_BUFFER_SIZE = 4096

    // VPN

    const val VPN_NOTIFICATION_ID = 1001

    const val VPN_CHANNEL_ID = "dns_vpn"

    const val VPN_MTU = 1500

    // Background Work

    const val BENCHMARK_INTERVAL_HOURS: Long = 6

    // Preferences

    const val DEFAULT_ACTIVE_DNS = "Auto"

    // Benchmark

    const val DEFAULT_ATTEMPTS = 10

    const val DEFAULT_DELAY_MS = 50L
}

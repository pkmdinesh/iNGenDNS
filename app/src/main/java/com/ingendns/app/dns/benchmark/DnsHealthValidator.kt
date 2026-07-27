package com.ingendns.app.dns.benchmark

import com.ingendns.app.dns.model.DnsServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class DnsHealthResult(
    val dnsReachable: Boolean,
    val internetReachable: Boolean
) {
    val healthy: Boolean get() = dnsReachable && internetReachable
}

class DnsHealthValidator(
    private val probe: DnsProbe = UdpDnsProbe(),
    private val httpsCheck: suspend () -> Boolean = ::defaultHttpsCheck
) {
    suspend fun validate(server: DnsServer): DnsHealthResult {
        val dnsReachable = probe.probe(server, 3_000, 1).success
        return DnsHealthResult(dnsReachable, httpsCheck())
    }

    companion object {
        private suspend fun defaultHttpsCheck(): Boolean = withContext(Dispatchers.IO) {
            runCatching {
                (URL("https://connectivitycheck.gstatic.com/generate_204").openConnection() as HttpURLConnection).run {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    try { responseCode in 200..399 } finally { disconnect() }
                }
            }.getOrDefault(false)
        }
    }
}

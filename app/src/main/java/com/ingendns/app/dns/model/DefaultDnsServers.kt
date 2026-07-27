package com.ingendns.app.dns.model

object DefaultDnsServers {
    val servers = listOf(
        DnsServer("Cloudflare", "1.1.1.1", dotHostname = "one.one.one.one", dohUrl = "https://cloudflare-dns.com/dns-query", profileId = "cloudflare"),
        DnsServer("Cloudflare Malware", "1.1.1.2", dotHostname = "security.cloudflare-dns.com", dohUrl = "https://security.cloudflare-dns.com/dns-query", profileId = "cloudflare-malware"),
        DnsServer("Cloudflare Family", "1.1.1.3", dotHostname = "family.cloudflare-dns.com", dohUrl = "https://family.cloudflare-dns.com/dns-query", profileId = "cloudflare-family"),
        DnsServer("Google", "8.8.8.8", dotHostname = "dns.google", dohUrl = "https://dns.google/dns-query", profileId = "google"),
        DnsServer("OpenDNS-Cisco", "208.67.222.222", dotHostname = "dns.opendns.com", dohUrl = "https://doh.opendns.com/dns-query", profileId = "opendns-cisco"),
        DnsServer("Bharat DNS", "1.10.10.10", dotHostname = "dns.nic.in", dohUrl = "https://dns.nic.in", profileId = "bharat-dns"),
        DnsServer("NextDNS", "45.90.28.0", dotHostname = "dns.nextdns.io", dohUrl = "https://dns.nextdns.io", profileId = "nextdns"),
        DnsServer("Control-D", "76.76.2.11", dotHostname = "p0.freedns.controld.com", dohUrl = "https://freedns.controld.com/p0", profileId = "control-d")
    )

    fun findByIp(ip: String): DnsServer? = servers.firstOrNull { it.ip == ip }

    fun findByProfileId(profileId: String): DnsServer? =
        servers.firstOrNull { it.profileId == profileId }
}

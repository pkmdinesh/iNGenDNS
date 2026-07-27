package com.ingendns.app.data.mapper

import com.ingendns.app.database.DnsEntity
import com.ingendns.app.database.DnsResultEntity
import com.ingendns.app.database.DefaultDnsOverrideEntity
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.dns.model.DefaultDnsServers

fun DnsEntity.toModel() = DnsServer(
    name = name,
    ip = ip,
    hostname = hostname,
    isCustom = custom,
    dotHostname = hostname,
    dohUrl = dohUrl
)

fun DnsServer.toEntity() = DnsEntity(
    name = name,
    ip = ip,
    hostname = dotHostname.ifBlank { hostname },
    custom = isCustom,
    dohUrl = dohUrl
)

fun DefaultDnsOverrideEntity.toModel() = DnsServer(
    name = name,
    ip = ip,
    isCustom = false,
    dotHostname = dotHostname,
    dohUrl = dohUrl,
    profileId = profileId
)

fun DnsServer.toDefaultOverrideEntity() = DefaultDnsOverrideEntity(
    profileId = requireNotNull(profileId),
    name = name,
    ip = ip,
    dotHostname = dotHostname,
    dohUrl = dohUrl
)

fun DnsResultEntity.toModel() = DnsTestResult(
    server = DefaultDnsServers.findByIp(ip)?.copy(name = name)
        ?: DnsServer(name = name, ip = ip),
    latency = latency,
    successRate = successRate,
    timestamp = timestamp,
    reachable = successRate > 0f,
    lowestLatency = lowestLatency,
    jitter = jitter,
    reliability = reliability,
    packetLoss = packetLoss,
    score = score,
    sessionId = sessionId
)

fun DnsTestResult.toEntity(
    sessionId: String
) = DnsResultEntity(
    sessionId = sessionId,
    name = server.name,
    ip = server.ip,
    latency = latency,
    successRate = successRate,
    timestamp = timestamp,
    lowestLatency = lowestLatency,
    jitter = jitter,
    reliability = reliability,
    packetLoss = packetLoss,
    score = score
)

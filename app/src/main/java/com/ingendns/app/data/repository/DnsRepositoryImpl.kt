package com.ingendns.app.data.repository

import com.ingendns.app.data.mapper.toEntity
import com.ingendns.app.data.mapper.toDefaultOverrideEntity
import com.ingendns.app.data.mapper.toModel
import com.ingendns.app.database.DnsDao
import com.ingendns.app.database.DefaultDnsOverrideEntity
import com.ingendns.app.dns.model.DefaultDnsServers
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.domain.model.DnsAnalytics
import com.ingendns.app.domain.repository.DnsRepository
import com.ingendns.app.database.BenchmarkSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class DnsRepositoryImpl(
    private val dao: DnsDao
) : DnsRepository {

    override suspend fun getDnsServers(): List<DnsServer> {
        purgeCustomDnsConflicts()
        val defaults = defaultProfiles(dao.getDefaultDnsOverrides())
        val defaultIps = defaults.mapTo(hashSetOf()) { it.ip }
        return defaults + dao.getDnsServers()
            .filter { it.custom && it.ip !in defaultIps }
            .map { it.toModel() }
    }

    override suspend fun saveDnsServers(
        servers: List<DnsServer>
    ) {
        dao.insertDnsServers(
            servers.map { it.toEntity() }
        )
    }

    override fun observeCustomDnsServers(): Flow<List<DnsServer>> = combine(
        dao.observeCustomDnsServers(),
        dao.observeDefaultDnsOverrides()
    ) { servers, overrides ->
        val defaultIps = defaultProfiles(overrides).mapTo(hashSetOf()) { it.ip }
        servers.filter { it.ip !in defaultIps }.map { it.toModel() }
    }

    override fun observeDnsServers(): Flow<List<DnsServer>> = combine(
        dao.observeDefaultDnsOverrides(),
        dao.observeCustomDnsServers()
    ) { overrides, customServers ->
        val defaults = defaultProfiles(overrides)
        val defaultIps = defaults.mapTo(hashSetOf()) { it.ip }
        defaults + customServers.filter { it.ip !in defaultIps }.map { it.toModel() }
    }

    override suspend fun updateDefaultDnsServer(server: DnsServer) {
        val profileId = server.profileId ?: return
        if (server.isCustom || DefaultDnsServers.findByProfileId(profileId) == null) return
        dao.upsertDefaultDnsOverride(server.toDefaultOverrideEntity())
    }

    private fun defaultProfiles(overrides: List<DefaultDnsOverrideEntity>): List<DnsServer> {
        val overridesById = overrides.associateBy { it.profileId }
        return DefaultDnsServers.servers.map { default ->
            default.profileId?.let(overridesById::get)?.toModel() ?: default
        }
    }

    override suspend fun addCustomDnsServer(server: DnsServer) {
        if (defaultProfiles(dao.getDefaultDnsOverrides()).any { it.ip == server.ip }) return
        dao.deleteCustomDnsServer(server.ip)
        dao.insertDnsServers(listOf(server.copy(isCustom = true).toEntity()))
    }

    override suspend fun removeCustomDnsServer(ip: String) {
        dao.deleteCustomDnsServer(ip)
        dao.deleteResultFromLatestSession(ip)
    }

    override suspend fun purgeCustomDnsConflicts() {
        dao.deleteCustomDnsServersByIps(
            defaultProfiles(dao.getDefaultDnsOverrides()).map { it.ip }
        )
    }

    override suspend fun getLatestResults(): List<DnsTestResult> {
        return attachProfiles(
            dao.getLatestBenchmark().map { it.toModel() },
            getDnsServers()
        )
    }

    override fun observeLatestResults(): Flow<List<DnsTestResult>> =
        combine(
            dao.observeLatestBenchmark(),
            observeDnsServers()
        ) { rows, profiles ->
            attachProfiles(
                rows.map { it.toModel() },
                profiles
            )
        }

    private fun attachProfiles(
        results: List<DnsTestResult>,
        profiles: List<DnsServer>
    ): List<DnsTestResult> {
        val profilesByIp = profiles.associateBy { it.ip }
        return results.map { result ->
            profilesByIp[result.server.ip]?.let { result.copy(server = it) } ?: result
        }.distinctBy { it.server.ip }
    }

    override suspend fun saveResults(
        sessionId: String,
        results: List<DnsTestResult>
    ) {
        dao.insertBenchmarkResults(
            results.map {
    it.toEntity(sessionId)
}
        )
    }

    override suspend fun saveBenchmarkSession(
        session: BenchmarkSessionEntity
    ) {
        dao.insertBenchmarkSession(session)
    }

    override suspend fun saveBenchmark(
        session: BenchmarkSessionEntity,
        results: List<DnsTestResult>
    ) {
        dao.insertBenchmark(
            session = session,
            results = results.map { it.toEntity(session.sessionId) }
        )
    }

    override suspend fun saveCustomBenchmarkResult(
        result: DnsTestResult,
        previousIp: String?
    ) {
        val sessionId = UUID.randomUUID().toString()
        dao.insertCustomBenchmarkSnapshot(
            sessionId = sessionId,
            timestamp = result.timestamp,
            result = result.toEntity(sessionId),
            previousIp = previousIp
        )
    }

    override fun observeBenchmarkHistory(): Flow<List<BenchmarkSessionEntity>> {
        return dao.observeBenchmarkSessions()
    }

    override fun observeRecentResults(cutoff: Long): Flow<List<DnsTestResult>> =
        dao.observeRecentResults(cutoff).map { rows -> rows.map { it.toModel() } }

    override suspend fun purgeHistoryOlderThan(cutoff: Long) {
        dao.deleteResultsOlderThan(cutoff)
        dao.deleteSessionsOlderThan(cutoff)
    }

    override suspend fun getAnalytics(): DnsAnalytics {

        val results = dao.getLatestBenchmark()

        if (results.isEmpty()) {
            return DnsAnalytics()
        }

        val best = results.maxByOrNull { it.score }!!

        return DnsAnalytics(
            bestDns = best.name,
            ipAddress = best.ip,
            score = best.score,
            averageLatency = best.latency,
            lowestLatency = best.lowestLatency,
            jitter = best.jitter,
            successRate = best.successRate,
            reliability = best.reliability,
            packetLoss = best.packetLoss
        )
    }

    override suspend fun getResultsForSession(
        sessionId: String
    ): List<DnsTestResult> {

        return dao
            .getResultsForSession(sessionId)
            .map { it.toModel() }
    }
}

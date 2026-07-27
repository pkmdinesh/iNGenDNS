package com.ingendns.app.domain.repository

import com.ingendns.app.database.BenchmarkSessionEntity
import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.domain.model.DnsAnalytics
import kotlinx.coroutines.flow.Flow

interface DnsRepository {

    suspend fun getDnsServers(): List<DnsServer>

    suspend fun saveDnsServers(
        servers: List<DnsServer>
    )

    fun observeCustomDnsServers(): Flow<List<DnsServer>>

    fun observeDnsServers(): Flow<List<DnsServer>>

    suspend fun addCustomDnsServer(server: DnsServer)

    suspend fun updateDefaultDnsServer(server: DnsServer)

    suspend fun removeCustomDnsServer(ip: String)

    suspend fun purgeCustomDnsConflicts()

    suspend fun getLatestResults(): List<DnsTestResult>

    fun observeLatestResults(): Flow<List<DnsTestResult>>

    suspend fun saveResults(
        sessionId: String,
        results: List<DnsTestResult>
    )

    suspend fun saveBenchmarkSession(
        session: BenchmarkSessionEntity
    )

    suspend fun saveBenchmark(
        session: BenchmarkSessionEntity,
        results: List<DnsTestResult>
    )

    suspend fun saveCustomBenchmarkResult(
        result: DnsTestResult,
        previousIp: String? = null
    )

    fun observeBenchmarkHistory(): Flow<List<BenchmarkSessionEntity>>

    fun observeRecentResults(cutoff: Long): Flow<List<DnsTestResult>>

    suspend fun purgeHistoryOlderThan(cutoff: Long)

    suspend fun getAnalytics(): DnsAnalytics

    suspend fun getResultsForSession(
        sessionId: String
    ): List<DnsTestResult>
}

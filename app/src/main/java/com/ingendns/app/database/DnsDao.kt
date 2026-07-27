package com.ingendns.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsDao {

    @Query("SELECT * FROM dns_servers")
    suspend fun getDnsServers(): List<DnsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDnsServers(
        servers: List<DnsEntity>
    )

    @Query("SELECT * FROM default_dns_overrides")
    suspend fun getDefaultDnsOverrides(): List<DefaultDnsOverrideEntity>

    @Query("SELECT * FROM default_dns_overrides")
    fun observeDefaultDnsOverrides(): Flow<List<DefaultDnsOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDefaultDnsOverride(override: DefaultDnsOverrideEntity)

    @Query("SELECT * FROM dns_servers WHERE custom = 1 ORDER BY name COLLATE NOCASE")
    fun observeCustomDnsServers(): Flow<List<DnsEntity>>

    @Query("DELETE FROM dns_servers WHERE custom = 1 AND ip = :ip")
    suspend fun deleteCustomDnsServer(ip: String)

    @Query("DELETE FROM dns_servers WHERE custom = 1 AND ip IN (:ips)")
    suspend fun deleteCustomDnsServersByIps(ips: List<String>)

    @Query(
        """
        SELECT *
        FROM dns_results
        ORDER BY timestamp DESC
    """
    )
    suspend fun getBenchmarkHistory(): List<DnsResultEntity>

    @Query(
        """
        SELECT r.*
        FROM dns_results AS r
        WHERE r.sessionId = (
            SELECT sessionId
            FROM benchmark_sessions
            ORDER BY timestamp DESC
            LIMIT 1
        )
        ORDER BY r.score DESC, r.latency ASC
    """
    )
    suspend fun getLatestBenchmark(): List<DnsResultEntity>

    @Query("SELECT sessionId FROM benchmark_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSessionId(): String?

    @Query("DELETE FROM dns_results WHERE sessionId = :sessionId AND ip = :ip")
    suspend fun deleteResultFromSession(sessionId: String, ip: String)

    @Query(
        """
        DELETE FROM dns_results
        WHERE ip = :ip AND sessionId = (
            SELECT sessionId
            FROM benchmark_sessions
            ORDER BY timestamp DESC
            LIMIT 1
        )
        """
    )
    suspend fun deleteResultFromLatestSession(ip: String)

    @Query(
        """
        SELECT r.*
        FROM dns_results AS r
        WHERE r.sessionId = (
            SELECT sessionId
            FROM benchmark_sessions
            ORDER BY timestamp DESC
            LIMIT 1
        )
        ORDER BY r.score DESC, r.latency ASC
        """
    )
    fun observeLatestBenchmark(): Flow<List<DnsResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarkResults(
        results: List<DnsResultEntity>
    )

    @Query("DELETE FROM dns_results")
    suspend fun clearBenchmarkResults()

    @Query(
        """
SELECT *
FROM benchmark_sessions
ORDER BY timestamp DESC
"""
    )
    fun observeBenchmarkSessions(): Flow<List<BenchmarkSessionEntity>>

    @Query("SELECT * FROM dns_results WHERE timestamp >= :cutoff ORDER BY timestamp DESC, score DESC")
    fun observeRecentResults(cutoff: Long): Flow<List<DnsResultEntity>>

    @Query("DELETE FROM dns_results WHERE timestamp < :cutoff")
    suspend fun deleteResultsOlderThan(cutoff: Long)

    @Query("DELETE FROM benchmark_sessions WHERE timestamp < :cutoff")
    suspend fun deleteSessionsOlderThan(cutoff: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarkSession(
        session: BenchmarkSessionEntity
    )

    @Transaction
    suspend fun insertBenchmark(
        session: BenchmarkSessionEntity,
        results: List<DnsResultEntity>
    ) {
        insertBenchmarkSession(session)
        insertBenchmarkResults(results)
    }

    /** Saves one custom-profile test as a new immutable snapshot of the current ranking. */
    @Transaction
    suspend fun insertCustomBenchmarkSnapshot(
        sessionId: String,
        timestamp: Long,
        result: DnsResultEntity,
        previousIp: String?
    ) {
        val snapshot = getLatestBenchmark()
            .filterNot { row -> row.ip == result.ip || row.ip == previousIp }
            .map { row -> row.copy(id = 0, sessionId = sessionId, timestamp = timestamp) }
            .plus(result.copy(id = 0, sessionId = sessionId, timestamp = timestamp))
        val best = snapshot.sortedWith(
            compareByDescending<DnsResultEntity> { it.score }.thenBy { it.latency }
        ).first()
        insertBenchmarkSession(
            BenchmarkSessionEntity(
                sessionId = sessionId,
                timestamp = timestamp,
                bestDns = best.name,
                bestLatency = best.latency
            )
        )
        insertBenchmarkResults(snapshot)
    }

    @Query(
        """
SELECT *
FROM dns_results
WHERE sessionId = :sessionId
ORDER BY latency ASC
"""
    )
    suspend fun getResultsForSession(
        sessionId: String
    ): List<DnsResultEntity>
}

package com.ingendns.app.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DnsDaoInstrumentedTest {
    private lateinit var database: AppDatabase

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun benchmarkTransactionPersistsSessionAndSortedResults() = runBlocking {
        val session = BenchmarkSessionEntity("session", 100L, "Fast", 10L)
        database.dnsDao().insertBenchmark(
            session,
            listOf(
                DnsResultEntity(sessionId = "session", name = "Slow", ip = "8.8.8.8", latency = 40, successRate = 100f, timestamp = 100),
                DnsResultEntity(sessionId = "session", name = "Fast", ip = "1.1.1.1", latency = 10, successRate = 100f, timestamp = 100)
            )
        )
        assertEquals(listOf("Fast", "Slow"), database.dnsDao().getLatestBenchmark().map { it.name })
        assertEquals(2, database.dnsDao().getResultsForSession("session").size)
    }

    @Test fun customResultCreatesACompleteImmutableSnapshot() = runBlocking {
        val dao = database.dnsDao()
        dao.insertBenchmark(
            BenchmarkSessionEntity("original", 100L, "Fast", 10L),
            listOf(
                DnsResultEntity(
                    sessionId = "original", name = "Fast", ip = "1.1.1.1",
                    latency = 10, successRate = 100f, timestamp = 100, score = 90
                ),
                DnsResultEntity(
                    sessionId = "original", name = "Slow", ip = "8.8.8.8",
                    latency = 40, successRate = 100f, timestamp = 100, score = 70
                )
            )
        )

        dao.insertCustomBenchmarkSnapshot(
            sessionId = "snapshot",
            timestamp = 200L,
            result = DnsResultEntity(
                sessionId = "snapshot", name = "Custom", ip = "9.9.9.9",
                latency = 20, successRate = 100f, timestamp = 200, score = 80
            ),
            previousIp = null
        )

        assertEquals(2, dao.getResultsForSession("original").size)
        assertEquals(
            listOf("Fast", "Custom", "Slow"),
            dao.getResultsForSession("snapshot")
                .sortedByDescending { it.score }
                .map { it.name }
        )
        assertEquals("snapshot", dao.getLatestSessionId())
    }
}

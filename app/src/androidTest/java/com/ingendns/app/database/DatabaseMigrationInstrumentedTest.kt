package com.ingendns.app.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate3To8BackfillsMetricsAndCreatesDefaultProfileOverrides() {
        helper.createDatabase(DATABASE_NAME, 3).apply {
            execSQL(
                "INSERT INTO benchmark_sessions " +
                    "(sessionId, timestamp, bestDns, bestLatency) " +
                    "VALUES ('session', 100, 'Test', 25)"
            )
            execSQL(
                "INSERT INTO dns_results " +
                    "(sessionId, name, ip, latency, successRate, timestamp) " +
                    "VALUES ('session', 'Test', '1.1.1.1', 25, 80, 100)"
            )
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            8,
            true,
            DatabaseProvider.MIGRATION_3_4,
            DatabaseProvider.MIGRATION_4_5,
            DatabaseProvider.MIGRATION_5_6,
            DatabaseProvider.MIGRATION_6_7,
            DatabaseProvider.MIGRATION_7_8
        ).use { database ->
            database.query(
                "SELECT lowestLatency, reliability, packetLoss, score FROM dns_results"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(25L, cursor.getLong(0))
                assertEquals(80f, cursor.getFloat(1))
                assertEquals(20f, cursor.getFloat(2))
                assertEquals(86, cursor.getInt(3))
            }
            database.query("SELECT COUNT(*) FROM default_dns_overrides").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "migration-test"
    }
}

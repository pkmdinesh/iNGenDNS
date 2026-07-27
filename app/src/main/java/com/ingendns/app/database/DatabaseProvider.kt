package com.ingendns.app.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {

    return INSTANCE ?: synchronized(this) {

        INSTANCE ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "ingendns.db"
        )
            .addMigrations(
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8
            )
            .build()
            .also {
                INSTANCE = it
            }
    }
}

    internal val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dns_results ADD COLUMN lowestLatency INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE dns_results ADD COLUMN jitter INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE dns_results ADD COLUMN reliability REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE dns_results ADD COLUMN packetLoss REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE dns_results ADD COLUMN score INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE dns_results
                SET lowestLatency = latency,
                    reliability = successRate,
                    packetLoss = MAX(0, 100.0 - successRate),
                    score = CAST(ROUND(
                        0.40 * MAX(0, MIN(100, 100.0 - latency * 0.4)) +
                        0.25 * MAX(0, MIN(100, successRate)) +
                        0.20 * MAX(0, MIN(100, successRate)) +
                        0.10 * 100 +
                        0.05 * MAX(0, MIN(100, successRate))
                    ) AS INTEGER)
                """.trimIndent()
            )
        }
    }

    internal val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dns_servers ADD COLUMN dohUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    internal val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "DELETE FROM dns_servers WHERE id NOT IN " +
                    "(SELECT MAX(id) FROM dns_servers GROUP BY ip)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_dns_servers_ip ON dns_servers(ip)"
            )
        }
    }

    internal val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                DELETE FROM dns_results
                WHERE id NOT IN (
                    SELECT MAX(id)
                    FROM dns_results
                    GROUP BY sessionId, ip
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_dns_results_sessionId_ip " +
                    "ON dns_results(sessionId, ip)"
            )
        }
    }

    internal val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS default_dns_overrides (
                    profileId TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    ip TEXT NOT NULL,
                    dotHostname TEXT NOT NULL,
                    dohUrl TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_default_dns_overrides_ip " +
                    "ON default_dns_overrides(ip)"
            )
        }
    }
}

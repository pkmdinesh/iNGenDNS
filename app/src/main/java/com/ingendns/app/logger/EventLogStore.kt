package com.ingendns.app.logger

import android.content.Context
import java.time.Instant

data class AppLogEvent(val timestamp: Instant, val event: String, val details: String)

/** Small bounded local audit trail. It intentionally stores no browsing or personal data. */
class EventLogStore(context: Context) {
    private val preferences = context.getSharedPreferences("smart_dns_log", Context.MODE_PRIVATE)

    fun record(event: String, details: String = "") {
        synchronized(LOCK) {
            val item = "${Instant.now().toEpochMilli()}\t${event.replace('\t', ' ')}\t${
                details.replace(
                    '\t',
                    ' '
                ).replace('\n', ' ')
            }"
            val entries = preferences.getString(ENTRIES, "").orEmpty().lineSequence()
                .filter { it.isNotBlank() }
                .toMutableList()
            entries += item
            preferences.edit().putString(ENTRIES, entries.takeLast(MAX_ENTRIES).joinToString("\n"))
                .apply()
        }
    }

    fun events(): List<AppLogEvent> =
        synchronized(LOCK) {
            preferences.getString(ENTRIES, "").orEmpty().lineSequence().mapNotNull { line ->
                runCatching {
                    val parts = line.split("\t", limit = 3)
                    AppLogEvent(
                        Instant.ofEpochMilli(parts[0].toLong()),
                        parts[1],
                        parts.getOrElse(2) { "" })
                }.getOrNull()
            }.toList().asReversed()
        }

    fun clear() = synchronized(LOCK) { preferences.edit().remove(ENTRIES).apply() }

    private companion object {
        const val ENTRIES = "entries"
        const val MAX_ENTRIES = 100
        val LOCK = Any()
    }
}

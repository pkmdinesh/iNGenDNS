package com.ingendns.app.logger

import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Keeps a small, privacy-safe local crash breadcrumb and then delegates to Android's
 * normal crash handler. Delegation is essential: Play Console/Android vitals receives
 * crash reports from Play-distributed builds through the platform crash pipeline.
 */
object CrashReporter {
    private const val TAG = "iNGenDNSCrash"

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is ReportingExceptionHandler) return
        Thread.setDefaultUncaughtExceptionHandler(
            ReportingExceptionHandler(context.applicationContext, previous)
        )
    }

    private class ReportingExceptionHandler(
        context: Context,
        private val delegate: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        private val logStore = EventLogStore(context)

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching {
                val summary = buildString {
                    append(throwable.javaClass.simpleName)
                    throwable.message?.takeIf(String::isNotBlank)?.let { append(": ").append(it) }
                    append("; thread=").append(thread.name)
                    append("; sdk=").append(Build.VERSION.SDK_INT)
                }.take(500)
                logStore.record("uncaught_crash", summary)
                Log.e(TAG, summary, throwable)
            }

            if (delegate != null) delegate.uncaughtException(thread, throwable)
            else android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

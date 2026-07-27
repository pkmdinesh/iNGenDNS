package com.ingendns.app.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ingendns.app.di.AppContainer
import com.ingendns.app.dns.benchmark.DnsHealthValidator
import com.ingendns.app.logger.EventLogStore
import java.util.concurrent.TimeUnit

class DnsHealthWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val best = AppContainer(applicationContext).dnsRepository.getLatestResults().firstOrNull()
            ?: return Result.success()
        val check = DnsHealthValidator().validate(best.server)
        EventLogStore(applicationContext).record(
            "Health check ${if (check.healthy) "passed" else "failed"}",
            "${best.server.name}: DNS ${if (check.dnsReachable) "OK" else "failed"}, HTTPS ${if (check.internetReachable) "OK" else "failed"}"
        )
        return if (check.healthy) Result.success() else Result.retry()
    }
}

object DnsHealthScheduler {
    private const val UNIQUE_WORK_NAME = "periodic_dns_health_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DnsHealthWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) =
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
}

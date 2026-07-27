package com.ingendns.app

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.ingendns.app.core.logger.AppLogger

class DistributionUpdateManager(
    private val activity: ComponentActivity,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val inProgress = info.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                if ((available || inProgress) && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    }.onFailure { AppLogger.e("Unable to start Play update", it) }
                }
            }
            .addOnFailureListener { AppLogger.w("Play update check unavailable: ${it.message}") }
    }
}

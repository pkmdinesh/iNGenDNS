package com.ingendns.app

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import android.widget.Toast
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

    /** Runs only from the user-selected Check for updates action. */
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
                } else {
                    Toast.makeText(
                        activity,
                        "You are using the latest version.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                AppLogger.w("Play update check unavailable: ${it.message}")
                Toast.makeText(
                    activity,
                    "Unable to check for updates. Try again later.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}

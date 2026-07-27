package com.ingendns.app

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

@Suppress("UNUSED_PARAMETER")
class DistributionUpdateManager(
    activity: ComponentActivity,
    updateLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    fun checkForUpdate() = Unit
}

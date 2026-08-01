package com.ingendns.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import com.ingendns.app.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Suppress("UNUSED_PARAMETER")
class DistributionUpdateManager(
    private val activity: ComponentActivity,
    updateLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    private var checkInProgress = false
    private var promptedVersion: String? = null

    /** Runs only from the user-selected Check for updates action. */
    fun checkForUpdate() {
        if (checkInProgress) return
        checkInProgress = true
        activity.lifecycleScope.launch {
            val release = runCatching { withContext(Dispatchers.IO) { fetchLatestRelease() } }
                .onFailure { AppLogger.w("GitHub update check unavailable: ${it.message}") }
                .getOrNull()
            checkInProgress = false
            if (release == null) {
                Toast.makeText(
                    activity,
                    "Unable to check for updates. Try again later.",
                    Toast.LENGTH_LONG
                ).show()
            } else if (
                compareAppVersions(release.version, installedVersionName()) > 0 &&
                promptedVersion != release.version &&
                !activity.isFinishing && !activity.isDestroyed
            ) {
                promptedVersion = release.version
                showUpdatePrompt(release)
            } else {
                Toast.makeText(
                    activity,
                    "You are using the latest version.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun installedVersionName(): String = runCatching {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    private fun fetchLatestRelease(): GitHubRelease {
        val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "iNGenDNS-Android")
        }
        return try {
            check(connection.responseCode in 200..299) {
                "GitHub returned HTTP ${connection.responseCode}"
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val version = extractAppVersion(tag)
                ?: error("Latest GitHub tag does not contain a version: $tag")
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.getJSONObject(index)
                    if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url").takeIf(String::isNotBlank)
                        if (apkUrl != null) break
                    }
                }
            }
            GitHubRelease(
                version = version,
                downloadUrl = apkUrl ?: json.getString("html_url")
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun showUpdatePrompt(release: GitHubRelease) {
        AlertDialog.Builder(activity)
            .setTitle("Update available")
            .setMessage("iNGenDNS version ${release.version} is available. Please update to the latest version.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Update") { _, _ ->
                runCatching {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl)))
                }.onFailure { AppLogger.e("Unable to open GitHub update", it) }
            }
            .show()
    }

    private data class GitHubRelease(val version: String, val downloadUrl: String)

    private companion object {
        const val LATEST_RELEASE_API =
            "https://api.github.com/repos/pkmdinesh/iNGenDNS/releases/latest"
        const val NETWORK_TIMEOUT_MS = 10_000
    }
}

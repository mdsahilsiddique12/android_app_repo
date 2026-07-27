package com.example.data.repository

import com.example.data.model.AppUpdateInfo
import com.example.data.remote.ApiClient
import com.example.data.remote.AppUpdateCheckRequest

/**
 * Checks for application updates via the backend API.
 * The backend manages update releases on Google Drive.
 */
class UpdateManager {

    suspend fun checkForUpdates(currentVersionCode: Int = 1): AppUpdateInfo {
        return try {
            val response = ApiClient.apiService.checkAppUpdate(
                AppUpdateCheckRequest(
                    platform = "android",
                    currentVersionCode = currentVersionCode
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                AppUpdateInfo(
                    currentVersion = currentVersionCode.toString(),
                    latestVersion = body.versionName,
                    releaseNotes = body.releaseNotes ?: "",
                    downloadUrl = body.downloadUrl,
                    checksum = body.sha256Checksum,
                    isMandatory = body.isMandatory,
                    updateAvailable = body.updateAvailable
                )
            } else {
                noUpdateAvailable(currentVersionCode)
            }
        } catch (e: Exception) {
            // Offline — no update info available
            noUpdateAvailable(currentVersionCode)
        }
    }

    private fun noUpdateAvailable(currentVersionCode: Int) = AppUpdateInfo(
        currentVersion = currentVersionCode.toString(),
        latestVersion = currentVersionCode.toString(),
        updateAvailable = false
    )
}

package com.example.data.repository

import com.example.data.local.ReportDao
import com.example.data.local.SyncQueueDao
import com.example.data.model.SyncQueueItem
import com.example.data.remote.ApiClient
import com.example.data.remote.SyncMutationItem
import com.example.data.remote.SyncPushRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages synchronization of locally queued mutations with the backend.
 * All sync happens through the backend API — never directly to Google Drive or PostgreSQL.
 */
class SyncRepository(
    private val syncQueueDao: SyncQueueDao,
    private val reportDao: ReportDao
) {

    val pendingSyncQueue: Flow<List<SyncQueueItem>> = syncQueueDao.getPendingQueue().map { list ->
        list.map { entity ->
            SyncQueueItem(
                id = entity.id,
                reportId = entity.reportId,
                action = entity.action,
                payloadJson = entity.payloadJson,
                attemptCount = entity.attemptCount,
                status = entity.status,
                createdAt = entity.createdAt
            )
        }
    }

    /**
     * Processes all pending sync items.
     * Returns Pair(successCount, failCount).
     *
     * Uses POST /api/v1/sync/push to batch-upload mutations.
     * On network error: items remain in queue for retry (no data loss).
     * On 401: relies on OkHttp Authenticator for token refresh.
     */
    suspend fun processPendingSync(): Pair<Int, Int> {
        var successCount = 0
        var failCount = 0

        val pendingList = syncQueueDao.getPendingQueueSnapshot()
        if (pendingList.isEmpty()) return Pair(0, 0)

        // Build batch mutation request
        val mutations = pendingList.map { item ->
            SyncMutationItem(
                entityType = "REPORT",
                entityId = item.reportId,
                action = item.action,
                payload = try {
                    @Suppress("UNCHECKED_CAST")
                    ApiClient.moshi.adapter(Map::class.java)
                        .fromJson(item.payloadJson) as? Map<String, Any?> ?: emptyMap()
                } catch (e: Exception) {
                    mapOf("raw" to item.payloadJson)
                },
                clientTimestamp = item.createdAt / 1000
            )
        }

        try {
            val response = ApiClient.apiService.syncPush(
                SyncPushRequest(
                    deviceId = "android_client",
                    mutations = mutations
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!

                // Mark all as synced on success
                pendingList.forEach { item ->
                    reportDao.updateReportSyncStatus(
                        id = item.reportId,
                        status = "SYNCED",
                        syncedAt = System.currentTimeMillis(),
                        syncError = null
                    )
                    syncQueueDao.deleteItem(item.id)
                    successCount++
                }

                // Log rejected items
                if (result.errors.isNotEmpty()) {
                    failCount = result.rejected
                }
            } else {
                // Server returned an error — keep items in queue for retry
                pendingList.forEach { item ->
                    syncQueueDao.updateItemStatus(item.id, "PENDING")
                    failCount++
                }
            }
        } catch (e: Exception) {
            // Network error — keep ALL items in queue for retry (no data loss)
            pendingList.forEach { item ->
                syncQueueDao.updateItemStatus(item.id, "PENDING")
                failCount++
            }
        }

        return Pair(successCount, failCount)
    }

    /**
     * Pulls delta updates from the backend.
     * @param since UNIX timestamp to fetch updates after.
     */
    suspend fun pullDeltaUpdates(since: Long): Boolean {
        return try {
            val response = ApiClient.apiService.syncPull(since)
            if (response.isSuccessful && response.body() != null) {
                // Delta updates are handled by the respective repositories
                // This method returns true if updates were received
                val data = response.body()!!
                data.patients.isNotEmpty() || data.reports.isNotEmpty() || data.tests.isNotEmpty()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

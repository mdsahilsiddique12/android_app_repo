package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.remote.ApiClient
import com.example.data.repository.ReportRepository
import com.example.data.repository.SyncRepository
import com.example.engine.report.NativeReportEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val labId: String = "",
    val username: String = "",
    val recentReportsCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val driveStatus: String = "Checking...",
    val serverStatus: String = "Connecting...",
    val licenseStatus: String = "",
    val updateAvailable: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val reportEngine = NativeReportEngine(application)
    private val reportRepo = ReportRepository(database.reportDao(), database.syncQueueDao(), reportEngine)
    private val syncRepo = SyncRepository(database.syncQueueDao(), database.reportDao())
    private val prefs = UserPreferencesManager(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.labIdFlow.collect { id ->
                _uiState.value = _uiState.value.copy(labId = id)
            }
        }
        viewModelScope.launch {
            prefs.usernameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(username = name)
            }
        }
        viewModelScope.launch {
            reportRepo.reportCount.collect { count ->
                _uiState.value = _uiState.value.copy(recentReportsCount = count)
            }
        }
        viewModelScope.launch {
            syncRepo.pendingSyncQueue.collect { list ->
                _uiState.value = _uiState.value.copy(pendingSyncCount = list.size)
            }
        }

        // Fetch dashboard summary from backend
        loadDashboardSummary()
    }

    private fun loadDashboardSummary() {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getDashboardSummary()
                if (response.isSuccessful && response.body() != null) {
                    val summary = response.body()!!
                    _uiState.value = _uiState.value.copy(
                        driveStatus = "Connected (${summary.driveSyncStatus})",
                        serverStatus = "Backend Online",
                        licenseStatus = "Active"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        serverStatus = "Backend Available",
                        driveStatus = "Cached Templates"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    serverStatus = "Offline Mode",
                    driveStatus = "Using Cached Data"
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val result = syncRepo.processPendingSync()
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncMessage = "Synchronized ${result.first} reports. ${if (result.second > 0) "${result.second} pending." else ""}"
            )
        }
    }

    fun dismissSyncMessage() {
        _uiState.value = _uiState.value.copy(syncMessage = null)
    }
}

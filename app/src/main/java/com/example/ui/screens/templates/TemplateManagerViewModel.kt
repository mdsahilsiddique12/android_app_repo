package com.example.ui.screens.templates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ReportTemplate
import com.example.data.repository.TemplateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TemplateManagerUiState(
    val templates: List<ReportTemplate> = emptyList(),
    val totalTemplates: Int = 0,
    val downloadedCount: Int = 0,
    val coloredDownloadedCount: Int = 0,
    val lastSyncTime: Long = 0,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null
)

class TemplateManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val templateManager = TemplateManager(application, database.templateDao())

    private val _uiState = MutableStateFlow(TemplateManagerUiState())
    val uiState: StateFlow<TemplateManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            templateManager.initializeLocalTemplates()
            templateManager.getTemplatesFlow().collect { list ->
                _uiState.value = _uiState.value.copy(
                    templates = list,
                    totalTemplates = list.size,
                    downloadedCount = list.count { it.isDownloaded },
                    coloredDownloadedCount = list.count { it.isColoredDownloaded },
                    lastSyncTime = list.maxOfOrNull { it.lastUpdated } ?: 0
                )
            }
        }
    }

    fun syncTemplates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, statusMessage = "Syncing templates from backend...")
            val updated = templateManager.syncTemplatesFromBackend()
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                statusMessage = "Sync complete! Updated $updated templates.",
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}

package com.example.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Report
import com.example.data.repository.ReportRepository
import com.example.engine.report.NativeReportEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecentReportsUiState(
    val reports: List<Report> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

class RecentReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val reportEngine = NativeReportEngine(application)
    private val reportRepo = ReportRepository(database.reportDao(), database.syncQueueDao(), reportEngine)

    private val _uiState = MutableStateFlow(RecentReportsUiState())
    val uiState: StateFlow<RecentReportsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reportRepo.allReports.collect { list ->
                _uiState.value = _uiState.value.copy(reports = list)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                reportRepo.allReports.collect { list ->
                    _uiState.value = _uiState.value.copy(reports = list)
                }
            } else {
                reportRepo.searchReports(query).collect { list ->
                    _uiState.value = _uiState.value.copy(reports = list)
                }
            }
        }
    }
}

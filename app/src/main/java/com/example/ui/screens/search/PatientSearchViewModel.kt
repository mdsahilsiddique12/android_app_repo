package com.example.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Patient
import com.example.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PatientSearchUiState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = ""
)

class PatientSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val patientRepo = PatientRepository(database.patientDao())

    private val _uiState = MutableStateFlow(PatientSearchUiState())
    val uiState: StateFlow<PatientSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            patientRepo.allPatients.collect { list ->
                _uiState.value = _uiState.value.copy(patients = list)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                patientRepo.allPatients.collect { list ->
                    _uiState.value = _uiState.value.copy(patients = list)
                }
            } else {
                patientRepo.searchPatients(query).collect { list ->
                    _uiState.value = _uiState.value.copy(patients = list)
                }
            }
        }
    }
}

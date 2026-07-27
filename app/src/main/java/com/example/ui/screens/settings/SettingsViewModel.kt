package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesManager
import com.example.data.model.ServerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: ServerSettings = ServerSettings(),
    val isSavedMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesManager(application)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.serverUrlFlow.collect { url ->
                _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(serverUrl = url))
            }
        }
        viewModelScope.launch {
            prefs.labIdFlow.collect { labId ->
                _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(labId = labId))
            }
        }
        viewModelScope.launch {
            prefs.isBiometricEnabledFlow.collect { bio ->
                _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(isBiometricEnabled = bio))
            }
        }
        viewModelScope.launch {
            prefs.isDarkModeFlow.collect { dark ->
                _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(themeMode = if (dark) "DARK" else "LIGHT"))
            }
        }
    }

    fun onServerUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(serverUrl = url))
    }

    fun onLabIdChanged(labId: String) {
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(labId = labId))
    }

    fun toggleBiometric(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(isBiometricEnabled = enabled))
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(themeMode = if (enabled) "DARK" else "LIGHT"))
    }

    fun saveSettings() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            prefs.updateSettings(
                serverUrl = s.serverUrl,
                labId = s.labId,
                biometricEnabled = s.isBiometricEnabled,
                darkMode = s.themeMode == "DARK"
            )
            _uiState.value = _uiState.value.copy(isSavedMessage = "Settings saved successfully.")
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(isSavedMessage = null)
    }

    fun logout() {
        viewModelScope.launch {
            prefs.logout()
        }
    }
}

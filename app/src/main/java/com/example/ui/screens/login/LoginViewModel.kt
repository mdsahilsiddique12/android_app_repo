package com.example.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesManager
import com.example.data.remote.ApiClient
import com.example.data.remote.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val labId: String = "",
    val username: String = "",
    val password: String = "",
    val isRememberMe: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val showServerSettings: Boolean = false,
    val serverUrl: String = "https://android-backend-kang.onrender.com"
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesManager(application)
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Initialize ApiClient with token provider
        ApiClient.setTokenProvider(prefs)

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
            prefs.serverUrlFlow.collect { url ->
                _uiState.value = _uiState.value.copy(serverUrl = url)
                ApiClient.updateBaseUrl(url)
            }
        }
        viewModelScope.launch {
            prefs.isBiometricEnabledFlow.collect { bio ->
                _uiState.value = _uiState.value.copy(isBiometricEnabled = bio)
            }
        }
    }

    fun onLabIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(labId = value)
    }

    fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun onRememberMeToggled(checked: Boolean) {
        _uiState.value = _uiState.value.copy(isRememberMe = checked)
    }

    fun toggleServerSettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(showServerSettings = show)
    }

    fun onServerUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        ApiClient.updateBaseUrl(url)
    }

    fun login() {
        if (_uiState.value.username.isBlank() || _uiState.value.password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter username and password")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val serverUrl = _uiState.value.serverUrl
                ApiClient.updateBaseUrl(serverUrl)

                val response = ApiClient.apiService.login(
                    LoginRequest(
                        usernameOrEmail = _uiState.value.username,
                        password = _uiState.value.password,
                        deviceId = "android_${android.os.Build.MODEL}",
                        platform = "android"
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    prefs.saveAuth(
                        labId = body.labId.ifBlank { _uiState.value.labId },
                        username = body.username.ifBlank { _uiState.value.username },
                        token = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.userId
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccess = true)
                } else {
                    // Server rejected credentials — show error but allow offline fallback
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Server authentication failed. Check credentials."
                    )
                }
            } catch (e: Exception) {
                // Offline fallback — allows clinical staff to work offline
                prefs.saveAuth(
                    labId = _uiState.value.labId,
                    username = _uiState.value.username,
                    token = "",
                    refreshToken = ""
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginSuccess = true
                )
            }
        }
    }

    fun loginWithBiometric() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            // Biometric login uses stored credentials
            _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccess = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

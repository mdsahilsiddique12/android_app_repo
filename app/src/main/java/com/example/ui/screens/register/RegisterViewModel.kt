package com.example.ui.screens.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesManager
import com.example.data.remote.ApiClient
import com.example.data.remote.UserCreateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = "",
    val password: String = "",
    val role: String = "LAB_TECH",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterSuccess: Boolean = false,
    val serverUrl: String = "https://android-backend-kang.onrender.com"
)

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesManager(application)
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.serverUrlFlow.collect { url ->
                _uiState.value = _uiState.value.copy(serverUrl = url)
                ApiClient.updateBaseUrl(url)
            }
        }
    }

    fun onFullNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value)
    }

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPhoneChanged(value: String) {
        _uiState.value = _uiState.value.copy(phone = value)
    }

    fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun onRoleChanged(value: String) {
        _uiState.value = _uiState.value.copy(role = value)
    }

    fun register() {
        val state = _uiState.value
        if (state.fullName.isBlank() || state.email.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all required fields")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                ApiClient.updateBaseUrl(state.serverUrl)
                val response = ApiClient.apiService.register(
                    UserCreateRequest(
                        email = state.email.trim(),
                        username = state.username.trim(),
                        fullName = state.fullName.trim(),
                        phone = state.phone.trim().ifBlank { null },
                        role = state.role,
                        password = state.password
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    prefs.saveRegistrationComplete()
                    _uiState.value = _uiState.value.copy(isLoading = false, isRegisterSuccess = true)
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val msg = if (errorBody.contains("already exists")) {
                        "User with this email or username already exists"
                    } else {
                        "Server registration failed: ${response.message()}"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                }
            } catch (e: Exception) {
                // If offline or backend is unreachable, allow saving offline config to bypass and continue
                prefs.saveRegistrationComplete()
                _uiState.value = _uiState.value.copy(isLoading = false, isRegisterSuccess = true)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

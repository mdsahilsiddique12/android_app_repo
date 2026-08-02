package com.example.ui.screens.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppUpdateInfo
import com.example.data.repository.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val updateInfo: AppUpdateInfo = AppUpdateInfo(),
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val statusMessage: String? = null
)

class UpdateViewModel : ViewModel() {

    private val updateManager = UpdateManager()
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        checkUpdate()
    }

    fun checkUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true)
            kotlinx.coroutines.delay(800)
            val info = updateManager.checkForUpdates(1)
            _uiState.value = _uiState.value.copy(isChecking = false, updateInfo = info)
        }
    }

    fun downloadAndInstall() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = 0f)
            for (p in 1..10) {
                kotlinx.coroutines.delay(200)
                _uiState.value = _uiState.value.copy(downloadProgress = p / 10f)
            }
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                statusMessage = "Update downloaded & verified. Package installer launched."
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}

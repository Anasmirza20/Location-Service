package com.locationtrackor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationtrackor.data.repository.LocationRepository
import com.locationtrackor.util.ConnectivityObserver
import com.locationtrackor.util.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState(
        isTracking = preferenceManager.isTracking()
    ))
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private val _showPermissionRequest = MutableStateFlow(false)
    val showPermissionRequest: StateFlow<Boolean> = _showPermissionRequest.asStateFlow()

    private val _showRationale = MutableStateFlow<PermissionType?>(null)
    val showRationale: StateFlow<PermissionType?> = _showRationale.asStateFlow()

    private val _goToSettings = MutableStateFlow(false)
    val goToSettings: StateFlow<Boolean> = _goToSettings.asStateFlow()

    private val _startServiceTrigger = MutableStateFlow(false)
    val startServiceTrigger: StateFlow<Boolean> = _startServiceTrigger.asStateFlow()

    private val _stopServiceTrigger = MutableStateFlow(false)
    val stopServiceTrigger: StateFlow<Boolean> = _stopServiceTrigger.asStateFlow()

    init {
        observeConnectivity()
        observePendingLogs()
    }

    enum class PermissionType {
        FOREGROUND, BACKGROUND
    }

    private fun observeConnectivity() {
        connectivityObserver.observe().onEach { status ->
            _uiState.value = _uiState.value.copy(
                connectivityStatus = status
            )
        }.launchIn(viewModelScope)
    }

    private fun observePendingLogs() {
        repository.getUnsyncedCount().onEach { count ->
            _uiState.value = _uiState.value.copy(
                pendingLogsCount = count
            )
        }.launchIn(viewModelScope)
    }

    fun onStartTrackingClicked(hasPermissions: Boolean) {
        if (!hasPermissions) {
            _showPermissionRequest.value = true
            return
        }
        _startServiceTrigger.value = true
        preferenceManager.setTracking(true)
        _uiState.value = _uiState.value.copy(isTracking = true)
    }

    fun onServiceStarted() {
        _startServiceTrigger.value = false
    }

    fun onStopTrackingClicked() {
        _stopServiceTrigger.value = true
        preferenceManager.setTracking(false)
        _uiState.value = _uiState.value.copy(isTracking = false)
    }

    fun onServiceStopped() {
        _stopServiceTrigger.value = false
    }

    fun onRationaleShown(permissionType: PermissionType) {
        _showRationale.value = permissionType
    }

    fun onRationaleDismissed() {
        _showRationale.value = null
    }

    fun onPermanentDenial() {
        _goToSettings.value = true
    }

    fun onSettingsOpened() {
        _goToSettings.value = false
    }

    fun onPermissionRequestHandled() {
        _showPermissionRequest.value = false
    }
}

data class LocationUiState(
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Unavailable,
    val pendingLogsCount: Int = 0,
    val isTracking: Boolean = false
)

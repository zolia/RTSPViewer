package com.cameraviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameraviewer.data.Camera
import com.cameraviewer.data.CameraDao
import com.cameraviewer.utils.ConnectionTestResult
import com.cameraviewer.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraListViewModel @Inject constructor(
    private val cameraDao: CameraDao
) : ViewModel() {
    val cameras: Flow<List<Camera>> = cameraDao.getAllCameras()

    private val _connectionTestResult = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionTestResult: StateFlow<ConnectionTestResult?> = _connectionTestResult

    fun testConnection(url: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = NetworkUtils.testRtspConnection(url)
            _connectionTestResult.value = result
            if (result is ConnectionTestResult.Success) {
                addCamera(name, url)
                onSuccess()
            }
        }
    }

    fun addCamera(name: String, url: String, username: String = "", password: String = "") {
        viewModelScope.launch {
            val camera = Camera(
                name = name,
                url = url,
                username = username,
                password = password
            )
            cameraDao.insertCamera(camera)
        }
    }

    fun deleteCamera(camera: Camera) {
        viewModelScope.launch {
            cameraDao.deleteCamera(camera)
        }
    }
}
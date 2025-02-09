package com.cameraviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameraviewer.data.Camera
import com.cameraviewer.data.CameraDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraListViewModel @Inject constructor(
    private val cameraDao: CameraDao
) : ViewModel() {
    val cameras: Flow<List<Camera>> = cameraDao.getAllCameras()

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
package com.cameraviewer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cameraviewer.viewmodel.CameraListViewModel

@Composable
fun CameraListScreen(
    viewModel: CameraListViewModel = hiltViewModel(),
    onCameraClick: (Long) -> Unit
) {
    val cameras by viewModel.cameras.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RTSP Cameras") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Camera")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(cameras) { camera ->
                CameraItem(
                    camera = camera,
                    onCameraClick = onCameraClick,
                    onDeleteClick = { viewModel.deleteCamera(camera) }
                )
            }
        }

        if (showAddDialog) {
            AddCameraDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, url, username, password ->
                    viewModel.addCamera(name, url, username, password)
                    showAddDialog = false
                }
            )
        }
    }
}
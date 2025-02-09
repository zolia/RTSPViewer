package com.cameraviewer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cameraviewer.ui.components.AddCameraDialog
import com.cameraviewer.viewmodel.CameraListViewModel
import com.cameraviewer.ui.components.CameraItem
import com.cameraviewer.ui.components.TestCameraDialog
import com.cameraviewer.utils.ConnectionTestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraListScreen(
    viewModel: CameraListViewModel = hiltViewModel(),
    onCameraClick: (Long) -> Unit
) {
    val cameras by viewModel.cameras.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val testResult by viewModel.connectionTestResult.collectAsState()

    LaunchedEffect(testResult) {
        testResult?.let { result ->
            val message = when (result) {
                is ConnectionTestResult.Success ->
                    "Connection successful! Port is open and accepting connections."
                is ConnectionTestResult.ConnectionFailed ->
                    "Connection failed: ${result.error}\nThis might indicate a firewall issue."
                ConnectionTestResult.InvalidUrl ->
                    "Invalid URL format"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("RTSP Cameras") },
                actions = {
                    IconButton(onClick = { showTestDialog = true }) {
                        Icon(Icons.Default.BugReport, contentDescription = "Add Test Camera")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Camera")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
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

        if (showTestDialog) {
            TestCameraDialog(
                onDismiss = { showTestDialog = false },
                onConfirm = { name, url ->
                    viewModel.testConnection(url, name) {
                        showTestDialog = false
                    }
                }
            )
        }
    }
}
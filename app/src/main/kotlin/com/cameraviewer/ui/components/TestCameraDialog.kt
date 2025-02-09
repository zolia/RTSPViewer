package com.cameraviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cameraviewer.utils.ConnectionTestResult
import com.cameraviewer.viewmodel.CameraListViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCameraDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    val testCameras = listOf(
        "Test Pattern" to "127.0.0.1:8554/test",
        "Test Pattern (Android Emulator)" to "10.0.2.2:8554/test",
        "Test Pattern (Full URL)" to "rtsp://127.0.0.1:8554/test"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Test Camera") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "These are test cameras for development. The stream must be published to MediaMTX first. " +
                            "Try different URL formats if one doesn't work.",
                    style = MaterialTheme.typography.bodySmall
                )

                testCameras.forEach { (name, url) ->
                    OutlinedButton(
                        onClick = {
                            onConfirm(name, url)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(name)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Troubleshooting:",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    "1. Check Windows Firewall:\n" +
                            "- Open Windows Defender Firewall\n" +
                            "- Click 'Allow an app through firewall'\n" +
                            "- Add MediaMTX (port 8554)\n\n" +
                            "2. Test using different IPs:\n" +
                            "- 127.0.0.1 (localhost)\n" +
                            "- 10.0.2.2 (Android emulator)\n" +
                            "- Your machine's IP address\n\n" +
                            "3. Verify MediaMTX is running:\n" +
                            "- Check if stream is published\n" +
                            "- Try accessing via VLC on Windows",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
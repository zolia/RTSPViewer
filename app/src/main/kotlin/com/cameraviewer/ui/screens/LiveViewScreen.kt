package com.cameraviewer.ui.screens

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.cameraviewer.viewmodel.LiveViewViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.cameraviewer.ui.components.TimestampPicker
import java.time.LocalDateTime
import java.time.ZoneOffset

@Composable
fun LiveViewScreen(
    cameraId: Long,
    onBack: () -> Unit,
    viewModel: LiveViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val camera by viewModel.getCamera(cameraId).collectAsState(initial = null)
    
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var showTimestampPicker by remember { mutableStateOf(false) }

    DisposableEffect(camera?.url) {
        val newPlayer = ExoPlayer.Builder(context).build()
        
        camera?.url?.let { url ->
            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(url))
            
            newPlayer.setMediaSource(mediaSource)
            newPlayer.prepare()
            newPlayer.playWhenReady = true
        }
        
        player = newPlayer

        onDispose {
            newPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(camera?.name ?: "Live View") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.takeSnapshot(cameraId) }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Take Snapshot")
                    }
                    IconButton(onClick = { viewModel.checkTimeSync(player) }) {
                        Icon(Icons.Default.Schedule, contentDescription = "Check Time Sync")
                    }
                actions = {
                    IconButton(onClick = { showTimestampPicker = !showTimestampPicker }) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Select Timestamp")
                    }
                }
            )
        }
    ) { padding ->
        val timeSyncStatus by viewModel.timeSyncStatus.collectAsState()
        val cameraTime by viewModel.cameraTime.collectAsState()

        LaunchedEffect(timeSyncStatus) {
            when (val status = timeSyncStatus) {
                is TimeSyncStatus.OutOfSync -> {
                    SnackbarHostState().showSnackbar(
                        message = "Camera time is out of sync by ${status.diffSeconds} seconds",
                        duration = SnackbarDuration.Long
                    )
                }
                is TimeSyncStatus.SlightlyOff -> {
                    SnackbarHostState().showSnackbar(
                        message = "Camera time is slightly off by ${status.diffSeconds} seconds",
                        duration = SnackbarDuration.Short
                    )
                }
                is TimeSyncStatus.Synced -> {
                    SnackbarHostState().showSnackbar(
                        message = "Camera time is in sync",
                        duration = SnackbarDuration.Short
                    )
                }
                else -> {}
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (showTimestampPicker) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    TimestampPicker { timestamp ->
                        showTimestampPicker = false
                        player?.let { exoPlayer ->
                            viewModel.seekToTimestamp(exoPlayer, timestamp) { error ->
                                // Show error message if seek fails
                                error?.let {
                                    SnackbarHostState().showSnackbar(
                                        message = error,
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        }
                    }
                }
            }

            player?.let { exoPlayer ->
                AndroidView(
                    factory = { context ->
                        StyledPlayerView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Playback controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = {
                            player?.seekTo(maxOf(0, player?.currentPosition?.minus(10000) ?: 0))
                        }
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Rewind 10 seconds",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                player?.pause()
                            } else {
                                player?.play()
                            }
                            isPlaying = !isPlaying
                        }
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
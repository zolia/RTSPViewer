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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.rtsp.*
import androidx.media3.ui.PlayerView
import com.cameraviewer.viewmodel.LiveViewViewModel
import androidx.media3.common.util.UnstableApi
import com.cameraviewer.viewmodel.TimeSyncStatus
import com.cameraviewer.ui.components.TimestampPicker
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
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
    val snackbarHostState = remember { SnackbarHostState() }

    var playerError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(camera?.url) {
        val newPlayer = ExoPlayer.Builder(context).build()

        camera?.url?.let { url ->
            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .setTimeoutMs(12000) // Increase timeout for more stability
                .setDebugLoggingEnabled(true)
                .setUserAgent("CameraViewer/1.0")
                .createMediaSource(MediaItem.fromUri(url))

            newPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                            "Network connection failed. Please check your connection and try again."
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Connection timed out. The camera might be offline."
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                            "Failed to initialize video decoder. The stream format might be unsupported."
                        else -> "Playback error: ${error.message}"
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        playerError = null
                    }
                }
            })

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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
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
                    snackbarHostState.showSnackbar(
                        message = "Camera time is out of sync by ${status.diffSeconds} seconds",
                        duration = SnackbarDuration.Long
                    )
                }
                is TimeSyncStatus.SlightlyOff -> {
                    snackbarHostState.showSnackbar(
                        message = "Camera time is slightly off by ${status.diffSeconds} seconds",
                        duration = SnackbarDuration.Short
                    )
                }
                is TimeSyncStatus.Synced -> {
                    snackbarHostState.showSnackbar(
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
                                    viewModel.viewModelScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = error,
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            player?.let { exoPlayer ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                                player = exoPlayer
                                useController = false // We have our own controls
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    playerError?.let { error ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

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
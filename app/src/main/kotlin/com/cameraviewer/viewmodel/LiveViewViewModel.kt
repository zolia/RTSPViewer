package com.cameraviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.cameraviewer.data.Camera
import com.cameraviewer.data.CameraDao
import com.cameraviewer.utils.RtspUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.absoluteValue

@androidx.annotation.OptIn(UnstableApi::class)
@HiltViewModel
class LiveViewViewModel @Inject constructor(
    private val cameraDao: CameraDao
) : ViewModel() {
    private val _cameraTime = MutableStateFlow<LocalDateTime?>(null)
    val cameraTime: StateFlow<LocalDateTime?> = _cameraTime

    private val _timeSyncStatus = MutableStateFlow<TimeSyncStatus>(TimeSyncStatus.Unknown)
    val timeSyncStatus: StateFlow<TimeSyncStatus> = _timeSyncStatus

    fun seekToTimestamp(player: Player, timestamp: LocalDateTime, onError: (String?) -> Unit) {
        try {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(
                timestamp.year,
                timestamp.monthValue - 1, // Calendar months are 0-based
                timestamp.dayOfMonth,
                timestamp.hour,
                timestamp.minute,
                timestamp.second
            )
            val startPosition = calendar.timeInMillis

            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    when (error.errorCode) {
                        androidx.media3.common.PlaybackException.ERROR_CODE_REMOTE_ERROR -> {
                            // RTSP error responses (457, 451, etc.)
                            onError("This camera does not support seeking to a specific time")
                        }
                        else -> {
                            onError("Failed to seek: ${error.message}")
                        }
                    }
                    player.removeListener(this)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        onError(null) // Success
                        player.removeListener(this)
                    }
                }
            })

            // Attempt to seek
            val mediaSource = RtspUtils.createRtspMediaSource(
                player.currentMediaItem?.localConfiguration?.uri.toString()
            )

            player.setMediaItem(MediaItem.fromUri(player.currentMediaItem?.mediaId ?: return))
            player.seekTo(startPosition)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            onError("Failed to seek: ${e.message}")
        }
    }
    fun getCamera(id: Long): Flow<Camera?> = flow {
        emit(cameraDao.getCameraById(id))
    }

    fun checkTimeSync(player: Player?) {
        player?.let { exoPlayer ->
            val timeline = exoPlayer.currentTimeline
            if (timeline.windowCount > 0) {
                val window = Timeline.Window()
                timeline.getWindow(0, window)

                val presentationStartTimeMs = window.presentationStartTimeMs
                if (presentationStartTimeMs != C.TIME_UNSET) {
                    val cameraDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(presentationStartTimeMs),
                        ZoneId.systemDefault()
                    )
                    _cameraTime.value = cameraDateTime

                    // Compare with system time
                    val systemTime = LocalDateTime.now()
                    val diffSeconds = java.time.Duration.between(cameraDateTime, systemTime).seconds.absoluteValue

                    _timeSyncStatus.value = when {
                        diffSeconds < 1 -> TimeSyncStatus.Synced
                        diffSeconds < 5 -> TimeSyncStatus.SlightlyOff(diffSeconds)
                        else -> TimeSyncStatus.OutOfSync(diffSeconds)
                    }
                }
            }
        }
    }

    fun takeSnapshot(cameraId: Long) {
        viewModelScope.launch {
            // Implement snapshot functionality
            // This would typically involve capturing a frame from the ExoPlayer
            // and saving it to local storage
        }
    }
}

sealed class TimeSyncStatus {
    object Unknown : TimeSyncStatus()
    object Synced : TimeSyncStatus()
    data class SlightlyOff(val diffSeconds: Long) : TimeSyncStatus()
    data class OutOfSync(val diffSeconds: Long) : TimeSyncStatus()
}
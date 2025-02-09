package com.cameraviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameraviewer.data.Camera
import com.cameraviewer.data.CameraDao
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
            val startPosition = timestamp.toInstant(ZoneOffset.UTC).toEpochMilli()
            
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                    when (error.errorCode) {
                        com.google.android.exoplayer2.PlaybackException.ERROR_CODE_REMOTE_ERROR -> {
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
            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(player.currentMediaItem?.localConfiguration?.uri ?: return))
            
            player.setMediaSource(mediaSource)
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
            val metadata = exoPlayer.mediaMetadata
            // Try to get NTP timestamp from RTSP stream
            val ntpTimestamp = (exoPlayer.currentMediaItem?.localConfiguration as? RtspMediaSource)
                ?.rtpTimestamp?.let { rtp ->
                    // Convert RTP timestamp to wall clock time
                    Instant.ofEpochMilli(rtp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                }

            ntpTimestamp?.let { cameraDateTime ->
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
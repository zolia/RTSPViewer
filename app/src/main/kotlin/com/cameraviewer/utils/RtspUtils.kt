package com.cameraviewer.utils

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.common.util.UnstableApi

@UnstableApi
object RtspUtils {
    fun createRtspMediaSource(url: String): RtspMediaSource {
        val rtspUrl = formatRtspUrl(url)
        return RtspMediaSource.Factory()
            .setForceUseRtpTcp(true) // Force TCP for better reliability
            .setTimeoutMs(15000)
            .setDebugLoggingEnabled(true)
            .setUserAgent("ExoPlayer")
            .createMediaSource(MediaItem.Builder()
                .setUri(rtspUrl)
                .setMediaId(rtspUrl)
                .build())
    }

    private fun formatRtspUrl(url: String): String {
        return url.trim().let { rawUrl ->
            if (!rawUrl.startsWith("rtsp://", ignoreCase = true)) {
                "rtsp://$rawUrl"
            } else rawUrl
        }
    }
}
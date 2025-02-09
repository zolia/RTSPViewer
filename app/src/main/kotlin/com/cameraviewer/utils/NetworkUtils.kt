package com.cameraviewer.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    suspend fun testRtspConnection(url: String): ConnectionTestResult {
        Log.d(TAG, "Testing RTSP connection to: $url")
        return withContext(Dispatchers.IO) {
            try {
                val uri = URI(if (!url.startsWith("rtsp://")) "rtsp://$url" else url)
                val port = if (uri.port == -1) 8554 else uri.port
                val host = uri.host ?: return@withContext ConnectionTestResult.InvalidUrl

                Log.d(TAG, "Parsed URL - Host: $host, Port: $port")

                val socket = Socket()
                try {
                    // Try to connect with a 5 second timeout
                    socket.connect(InetSocketAddress(host, port), 5000)
                    Log.d(TAG, "TCP connection successful")

                    // Try RTSP OPTIONS request
                    val writer = socket.getOutputStream().bufferedWriter()
                    val reader = socket.getInputStream().bufferedReader()

                    writer.write("OPTIONS rtsp://$host:$port RTSP/1.0\r\n")
                    writer.write("CSeq: 1\r\n")
                    writer.write("\r\n")
                    writer.flush()

                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNullOrEmpty()) break
                        response.append(line).append("\n")
                    }

                    Log.d(TAG, "RTSP Response:\n$response")

                    socket.close()
                    ConnectionTestResult.Success
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed", e)
                    ConnectionTestResult.ConnectionFailed(e.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Invalid URL", e)
                ConnectionTestResult.InvalidUrl
            }
        }
    }
}

sealed class ConnectionTestResult {
    object Success : ConnectionTestResult()
    object InvalidUrl : ConnectionTestResult()
    data class ConnectionFailed(val error: String) : ConnectionTestResult()
}
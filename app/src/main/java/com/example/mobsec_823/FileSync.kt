package com.example.mobsec_823

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object FileSync {
    private const val TAG = "FileSync"
    private val client = OkHttpClient()

    fun uploadFile(context: Context, serverIp: String, deviceId: String, path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(path)
                if (!file.exists()) {
                    Log.e(TAG, "File not found: $path")
                    return@launch
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("device_id", deviceId)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("http://$serverIp:5000/api/upload")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Upload failed: ${response.code}")
                    } else {
                        Log.i(TAG, "Upload complete: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
            }
        }
    }
}

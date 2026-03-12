package com.example.mobsec_823

import android.content.Context
import android.provider.Settings
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object FileUploader {
    private const val TAG = "FileUploader"
    private val client = OkHttpClient()

    fun uploadFile(context: Context, serverIp: String, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "File to upload does not exist: $filePath")
            return
        }

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val uploadUrl = "http://$serverIp:5000/api/upload"

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("device_id", deviceId)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "File uploaded successfully: ${response.body?.string()}")
                } else {
                    Log.e(TAG, "File upload failed: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during file upload", e)
        }
    }
}

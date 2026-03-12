package com.example.mobsec_823

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File

object DynamicCodeLoader {
    private const val TAG = "DynamicCodeLoader"

    fun executePayload(context: Context, dexPath: String) {
        try {
            val dexFile = File(dexPath)
            if (!dexFile.exists()) {
                Log.e(TAG, "DEX file does not exist: $dexPath")
                return
            }

            // Set file to read-only for Android 14+
            if (dexFile.setReadOnly()) {
                Log.i(TAG, "Set payload.dex to read-only.")
            } else {
                Log.w(TAG, "Failed to set payload.dex to read-only.")
            }

            val optimizedDir = context.getDir("odex", Context.MODE_PRIVATE)
            val dexClassLoader = DexClassLoader(
                dexPath,
                optimizedDir.absolutePath,
                null,
                context.classLoader
            )

            val payloadClass = dexClassLoader.loadClass("com.research.Payload")
            val payloadInstance = payloadClass.newInstance()
            val startExploitMethod = payloadClass.getMethod("startExploit")

            Log.d(TAG, "Invoking startExploit() from dynamically loaded payload...")
            startExploitMethod.invoke(payloadInstance)
            Log.d(TAG, "Payload execution finished.")

        } catch (e: Exception) {
            Log.e(TAG, "Dynamic code execution failed", e)
        }
    }
}

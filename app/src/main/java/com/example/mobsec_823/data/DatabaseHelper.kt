package com.example.mobsec_823.data

import android.content.Context
import com.example.mobsec_823.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseHelper {
    fun initialize(context: Context) {
        try {
            // Initialize API client instead of direct DB connection
            ApiClient.initialize(context)
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalStateException("Failed to initialize API client. Make sure app.properties exists and is configured.", e)
        }
    }

    suspend fun getUserById(userId: Int): User? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().getUserById(userId)
            if (response.success && response.user != null) {
                response.user
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().healthCheck()
            response.success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

package com.example.mobsec_823.data.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties

/**
 * Simple API client using basic HttpURLConnection.
 * No Retrofit, no OkHttp - just bare bones HTTP.
 */
object SimpleApi {
    private var baseUrl: String = ""
    private var apiKey: String = ""

    private const val TIMEOUT_MS = 15000 // Increased to 15 seconds
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L // Increased to 1 second

    fun initialize(context: Context) {
        // FORCE DISABLE JVM KEEP-ALIVE (Global setting)
        // This is the strongest way to prevent 'unexpected end of stream' on Android
        System.setProperty("http.keepAlive", "false")

        val properties = Properties()
        context.assets.open("app.properties").use { inputStream ->
            properties.load(inputStream)
        }
        baseUrl = properties.getProperty("api.base.url")?.trim() ?: ""
        apiKey = properties.getProperty("api.key")?.trim() ?: ""

        // Remove trailing slash if present
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.dropLast(1)
        }

        println("SimpleApi initialized: baseUrl=$baseUrl")
    }

    // Simple GET request with retry
    suspend fun get(endpoint: String): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi GET $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val result = doGet(endpoint)
                println("SimpleApi GET $endpoint SUCCESS: ${result?.take(200)}...")
                return@withContext result
            } catch (e: Exception) {
                println("SimpleApi GET $endpoint FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    // Simple POST request with retry
    suspend fun post(endpoint: String, body: String = ""): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi POST $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val result = doPost(endpoint, body)
                println("SimpleApi POST $endpoint SUCCESS: ${result?.take(200)}...")
                return@withContext result
            } catch (e: Exception) {
                println("SimpleApi POST $endpoint FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    // Simple DELETE request with retry
    suspend fun delete(endpoint: String): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi DELETE $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val result = doDelete(endpoint)
                println("SimpleApi DELETE $endpoint SUCCESS: ${result?.take(200)}...")
                return@withContext result
            } catch (e: Exception) {
                println("SimpleApi DELETE $endpoint FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    // Simple DELETE request with body (for endpoints that require user_id in body)
    suspend fun deleteWithBody(endpoint: String, body: String): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi DELETE (with body) $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val result = doDeleteWithBody(endpoint, body)
                println("SimpleApi DELETE (with body) $endpoint SUCCESS: ${result?.take(200)}...")
                return@withContext result
            } catch (e: Exception) {
                println("SimpleApi DELETE (with body) $endpoint FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    // Simple PUT request with retry
    suspend fun put(endpoint: String, body: String = ""): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi PUT $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val result = doPut(endpoint, body)
                println("SimpleApi PUT $endpoint SUCCESS: ${result?.take(200)}...")
                return@withContext result
            } catch (e: Exception) {
                println("SimpleApi PUT $endpoint FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    private fun doGet(endpoint: String): String {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-App-Secret", apiKey)
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0") // FIX 2: Identify client
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Connection", "close")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                val error = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Unknown error"
                throw Exception("HTTP $responseCode: $error")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun doPost(endpoint: String, body: String): String {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("X-App-Secret", apiKey)
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0") // FIX 2: Identify client
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Connection", "close")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true

            if (body.isNotEmpty()) {
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                return connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                // Treat 409 Conflict as success (e.g. "User already in class")
                // This makes the operation idempotent for the UI
                println("SimpleApi POST $endpoint returned 409 (Conflict) - treating as success")
                return "{\"success\": true, \"message\": \"Resource already exists\"}"
            } else {
                val error = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Unknown error"
                throw Exception("HTTP $responseCode: $error")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun doDelete(endpoint: String): String {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("X-App-Secret", apiKey)
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0") // FIX 2: Identify client
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Connection", "close")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // Treat 404 Not Found as success (e.g. "User was not in class")
                // This makes the operation idempotent for the UI
                println("SimpleApi DELETE $endpoint returned 404 (Not Found) - treating as success")
                return "{\"success\": true, \"message\": \"Resource already deleted\"}"
            } else {
                val error = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Unknown error"
                throw Exception("HTTP $responseCode: $error")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun doDeleteWithBody(endpoint: String, body: String): String {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("X-App-Secret", apiKey)
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Connection", "close")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true

            // Write body
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                println("SimpleApi DELETE (with body) $endpoint returned 404 (Not Found)")
                return "{\"success\": false, \"message\": \"Resource not found\"}"
            } else {
                val error = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Unknown error"
                throw Exception("HTTP $responseCode: $error")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun doPut(endpoint: String, body: String): String {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PUT"
            connection.setRequestProperty("X-App-Secret", apiKey)
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0") // FIX 2: Identify client
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Connection", "close")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true

            if (body.isNotEmpty()) {
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                val error = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Unknown error"
                throw Exception("HTTP $responseCode: $error")
            }
        } finally {
            connection.disconnect()
        }
    }
}

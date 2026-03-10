package com.example.mobsec_823.data.api

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * API client using HttpURLConnection for basic requests and OkHttp for multipart uploads.
 */
object SimpleApi {
    private var baseUrl: String = ""
    private var apiKey: String = ""
    private const val TAG = "SimpleApi"

    private const val TIMEOUT_MS = 15000
    private const val MAX_RETRIES = 2
    private const val RETRY_DELAY_MS = 1000L

    // OkHttp client used only for multipart uploads (postMultipart, putMultipartImageBytes, getBytes)
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()

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

    // ========== PUBLIC METHODS WITH RETRY (HttpURLConnection) ==========

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

    suspend fun login(endpoint: String, body: String): String = post(endpoint, body) ?: """{"success":false,"error":"Login failed"}"""

    // ========== PRIVATE HttpURLConnection METHODS ==========

    private fun doGet(endpoint: String): String {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-App-Secret", apiKey)
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0")
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
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0")
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
                val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                return errorBody ?: "{\"success\": true, \"message\": \"Resource already exists\"}"
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
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0")
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
            connection.setRequestProperty("User-Agent", "MobSec-Android/1.0")
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

    // ========== OkHttp MULTIPART METHODS ==========

    suspend fun getBytes(endpoint: String): ByteArray? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi GET Bytes $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .header("X-App-Secret", apiKey)
                    .header("User-Agent", "MobSec-Android/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext response.body?.bytes()
                    } else {
                        println("SimpleApi GET Bytes FAILED (${response.code}), attempt ${attempt + 1}")
                        if (attempt < MAX_RETRIES - 1) {
                            delay(RETRY_DELAY_MS * (attempt + 1))
                        }
                    }
                }
            } catch (e: Exception) {
                println("SimpleApi GET Bytes FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    suspend fun postMultipart(
        endpoint: String,
        params: Map<String, String>,
        fileUri: Uri?,
        fileFieldName: String,
        context: Context,
        explicitFileName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi Multipart POST $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

                params.forEach { (key, value) ->
                    builder.addFormDataPart(key, value)
                }

                if (fileUri != null) {
                    val fileName = explicitFileName ?: getFileName(context, fileUri) ?: "upload.pdf"
                    val file = uriToFile(context, fileUri, fileName)
                    if (file != null) {
                        builder.addFormDataPart(fileFieldName, fileName, file.asRequestBody("application/pdf".toMediaType()))
                    }
                }

                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .header("X-App-Secret", apiKey)
                    .header("User-Agent", "MobSec-Android/1.0")
                    .post(builder.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (!response.isSuccessful) {
                        println("SimpleApi Multipart POST FAILED (${response.code}), attempt ${attempt + 1}")
                        if (attempt < MAX_RETRIES - 1) {
                            delay(RETRY_DELAY_MS * (attempt + 1))
                        }
                        return@repeat
                    }
                    return@withContext responseBody
                }
            } catch (e: Exception) {
                println("SimpleApi Multipart POST FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    /**
     * Upload an image as multipart PUT from a raw byte array (e.g., compressed JPEG from Bitmap).
     */
    suspend fun putMultipartImageBytes(
        endpoint: String,
        imageBytes: ByteArray,
        fileName: String = "profile.jpg"
    ): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                println("SimpleApi Multipart PUT Image $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                builder.addFormDataPart(
                    "image",
                    fileName,
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )

                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .header("X-App-Secret", apiKey)
                    .header("User-Agent", "MobSec-Android/1.0")
                    .put(builder.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (!response.isSuccessful) {
                        println("SimpleApi Multipart PUT Image FAILED (${response.code}), attempt ${attempt + 1}: ${responseBody?.take(200)}")
                        if (response.code in 400..499) {
                            return@withContext responseBody
                        }
                        if (attempt < MAX_RETRIES - 1) {
                            delay(RETRY_DELAY_MS * (attempt + 1))
                        }
                        return@repeat
                    }
                    println("SimpleApi Multipart PUT Image $endpoint SUCCESS")
                    return@withContext responseBody
                }
            } catch (e: Exception) {
                println("SimpleApi Multipart PUT Image FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    // ========== UTILITY METHODS ==========

    private fun uriToFile(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get filename from content provider", e)
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }
}

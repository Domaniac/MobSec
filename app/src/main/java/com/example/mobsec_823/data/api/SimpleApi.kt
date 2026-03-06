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
import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Robust API client using OkHttp.
 */
object SimpleApi {
    private var baseUrl: String = ""
    private var apiKey: String = ""
    private const val TAG = "SimpleApi"

    private const val MAX_RETRIES = 10
    private const val RETRY_DELAY_MS = 1000L

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
        val properties = Properties()
        try {
            context.assets.open("app.properties").use { properties.load(it) }
            baseUrl = properties.getProperty("api.base.url")?.trim() ?: ""
            apiKey = properties.getProperty("api.key")?.trim() ?: ""
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.dropLast(1)
            Log.d(TAG, "Initialized with baseUrl: $baseUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load app.properties", e)
        }
    }

    private suspend fun performRequest(
        endpoint: String,
        method: String = "GET",
        body: String? = null
    ): String? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                Log.d(TAG, "$method $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val url = if (endpoint.startsWith("http")) endpoint else "$baseUrl$endpoint"
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("X-App-Secret", apiKey)
                    .header("User-Agent", "MobSec-Android/1.0")
                    .header("Accept", "application/json")

                val requestBody = body?.toRequestBody("application/json".toMediaType())

                when (method.uppercase()) {
                    "GET" -> requestBuilder.get()
                    "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody("application/json".toMediaType()))
                    "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody("application/json".toMediaType()))
                    "DELETE" -> {
                        if (requestBody != null) requestBuilder.delete(requestBody)
                        else requestBuilder.delete()
                    }
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (!response.isSuccessful) {
                        Log.e(TAG, "$method to $endpoint FAILED (${response.code}): ${responseBody?.take(200)}")
                        // Don't retry on 4xx client errors — they are definitive responses
                        if (response.code in 400..499) {
                            return@withContext responseBody
                        }
                        if (attempt < MAX_RETRIES - 1) {
                            delay(RETRY_DELAY_MS * (attempt + 1))
                        }
                        return@repeat
                    }
                    Log.d(TAG, "$method $endpoint SUCCESS: ${responseBody?.take(200)}")
                    return@withContext responseBody
                }
            } catch (e: Exception) {
                Log.e(TAG, "$method to $endpoint FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

    suspend fun getBytes(endpoint: String): ByteArray? = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            try {
                Log.d(TAG, "GET Bytes $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .header("X-App-Secret", apiKey)
                    .header("User-Agent", "MobSec-Android/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext response.body?.bytes()
                    } else {
                        Log.e(TAG, "GET Bytes FAILED (${response.code}), attempt ${attempt + 1}")
                        if (attempt < MAX_RETRIES - 1) {
                            delay(RETRY_DELAY_MS * (attempt + 1))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "GET Bytes FAILED (attempt ${attempt + 1}): ${e.message}")
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
                Log.d(TAG, "Multipart POST $endpoint (attempt ${attempt + 1}/$MAX_RETRIES)")
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
                        Log.e(TAG, "Multipart POST FAILED (${response.code}), attempt ${attempt + 1}")
                        if (attempt < MAX_RETRIES - 1) {
                            delay(RETRY_DELAY_MS * (attempt + 1))
                        }
                        return@repeat
                    }
                    return@withContext responseBody
                }
            } catch (e: Exception) {
                Log.e(TAG, "Multipart POST FAILED (attempt ${attempt + 1}): ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        null
    }

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

    suspend fun get(endpoint: String): String? = performRequest(endpoint, "GET")
    suspend fun post(endpoint: String, body: String = ""): String? = performRequest(endpoint, "POST", body)
    suspend fun login(endpoint: String, body: String): String = post(endpoint, body) ?: """{"success":false,"error":"Login failed"}"""
    suspend fun delete(endpoint: String): String? = performRequest(endpoint, "DELETE")
    suspend fun deleteWithBody(endpoint: String, body: String): String? = performRequest(endpoint, "DELETE", body)
    suspend fun put(endpoint: String, body: String = ""): String? = performRequest(endpoint, "PUT", body)
}

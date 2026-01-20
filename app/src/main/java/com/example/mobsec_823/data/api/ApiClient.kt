package com.example.mobsec_823.data.api

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Properties
import java.util.concurrent.TimeUnit

object ApiClient {
    private var apiService: ApiService? = null
    private var baseUrl: String? = null
    private var apiKey: String? = null

    fun initialize(context: Context) {
        try {
            val properties = Properties()
            context.assets.open("app.properties").use { inputStream ->
                properties.load(inputStream)
            }

            // Debug: Print all loaded properties
            println("DEBUG: All properties loaded: ${properties.stringPropertyNames()}")
            properties.stringPropertyNames().forEach { key ->
                println("DEBUG: Property '$key' = '${properties.getProperty(key)}'")
            }

            baseUrl = properties.getProperty("api.base.url")?.trim()
            apiKey = properties.getProperty("api.key")?.trim()

            // Debug logging to verify API key is loaded
            println("DEBUG: API Base URL loaded: '$baseUrl' (length: ${baseUrl?.length})")
            println("DEBUG: API Key loaded: '$apiKey' (length: ${apiKey?.length})")

            if (baseUrl == null || apiKey == null) {
                throw IllegalStateException("Missing API configuration in app.properties")
            }

            // Create logging interceptor for debugging
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Create auth interceptor
            val authInterceptor = AuthInterceptor(apiKey!!)

            // Build OkHttp client
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            // Build Gson with lenient parsing
            val gson = GsonBuilder()
                .setLenient()
                .create()

            // Build Retrofit instance
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl!!)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            apiService = retrofit.create(ApiService::class.java)

        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalStateException(
                "Failed to load app.properties. Make sure to copy app.properties.template " +
                        "to app.properties and configure it.", e
            )
        }
    }

    fun getService(): ApiService {
        return apiService ?: throw IllegalStateException(
            "ApiClient not initialized. Call initialize(context) first."
        )
    }
}

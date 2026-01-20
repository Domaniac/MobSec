package com.example.mobsec_823.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Debug logging
        println("DEBUG AuthInterceptor: Adding X-App-Secret header with value: $apiKey")

        val requestWithAuth = originalRequest.newBuilder()
            .header("X-App-Secret", apiKey)
            .build()

        // Debug logging to verify header was added
        println("DEBUG AuthInterceptor: Request headers: ${requestWithAuth.headers}")

        return chain.proceed(requestWithAuth)
    }
}

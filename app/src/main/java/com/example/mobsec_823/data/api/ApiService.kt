package com.example.mobsec_823.data.api

import com.example.mobsec_823.data.User
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("api/user/{userId}")
    suspend fun getUserById(@Path("userId") userId: Int): ApiResponse<User>

    @GET("api/users")
    suspend fun getAllUsers(): ApiResponse<User>

    @GET("api/health")
    suspend fun healthCheck(): ApiResponse<Any>
}

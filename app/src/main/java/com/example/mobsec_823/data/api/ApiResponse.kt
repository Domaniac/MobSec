package com.example.mobsec_823.data.api

data class ApiResponse<T>(
    val success: Boolean,
    val user: T? = null,
    val users: List<T>? = null,
    val error: String? = null,
    val message: String? = null,
    val count: Int? = null
)

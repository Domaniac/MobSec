package com.example.mobsec_823.data.api

import com.example.mobsec_823.data.ClassEntity
import com.example.mobsec_823.data.User

/**
 * Simple response classes for JSON parsing
 */
data class SimpleResponse(
    val success: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

data class UserResponse(
    val success: Boolean = false,
    val user: User? = null,
    val users: List<User>? = null,
    val count: Int? = null,
    val error: String? = null
)

data class ClassResponse(
    val success: Boolean = false,
    val `class`: ClassEntity? = null,
    val classes: List<ClassEntity>? = null,
    val count: Int? = null,
    val error: String? = null
)

package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("student_employee_number")
    val studentEmployeeNumber: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("full_name")
    val fullName: String? = null,

    @SerializedName("profile_image_url")
    val profileImageUrl: String? = null,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("class_id")
    val classId: String
)

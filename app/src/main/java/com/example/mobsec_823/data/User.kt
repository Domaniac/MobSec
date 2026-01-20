package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("username")
    val username: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("full_name")
    val fullName: String?,

    @SerializedName("profile_image_url")
    val profileImageUrl: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("class_id")
    val classId: String
)

package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class UserLocation(
    @SerializedName("user_id")
    val userId: Int,
    val username: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("updated_at")
    val lastSeen: String? = null
)
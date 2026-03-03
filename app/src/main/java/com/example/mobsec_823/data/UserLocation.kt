package com.example.mobsec_823.data

data class UserLocation(
    val userId: Int,
    val username: String,
    val latitude: Double,
    val longitude: Double,
    val lastSeen: String? = null
)
package com.example.mobsec_823.data

data class GroupEntity(
    val group_id: Int,
    val class_id: Int,
    val group_name: String,
    val members: List<User>? = null
)
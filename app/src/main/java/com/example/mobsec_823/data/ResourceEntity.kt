package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class ResourceEntity(
    @SerializedName("resource_id") val resourceId: Int,
    @SerializedName("class_id") val classId: Int,
    @SerializedName("teacher_id") val teacherId: Int,
    val title: String,
    val description: String?,
    val url: String?,
    @SerializedName("resource_type") val resourceType: String, // "pdf" or "link"
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class ResourceResponse(
    val success: Boolean,
    val resources: List<ResourceEntity>?,
    val message: String?,
    val error: String?
)

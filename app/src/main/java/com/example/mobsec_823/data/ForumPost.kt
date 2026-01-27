package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class ForumPost(
    @SerializedName("post_id")
    val postId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("class_id")
    val classId: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("full_name")
    val fullName: String?
)

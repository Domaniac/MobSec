package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("comment_id")
    val commentId: Int,

    @SerializedName("post_id")
    val postId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("content")
    val content: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("full_name")
    val fullName: String?
)

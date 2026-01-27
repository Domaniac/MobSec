package com.example.mobsec_823.data.api

import com.example.mobsec_823.data.ClassEntity
import com.example.mobsec_823.data.Question
import com.example.mobsec_823.data.Comment
import com.example.mobsec_823.data.ForumPost
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
data class QuestionResponse(
    val success: Boolean = false,
    val question: Question? = null,
    val questions: List<Question>? = null,
    val count: Int? = null,
    val error: String? = null
)
data class ForumPostResponse(
    val success: Boolean = false,
    val post: ForumPost? = null,
    val posts: List<ForumPost>? = null,
    val count: Int? = null,
    val error: String? = null
)

data class CommentResponse(
    val success: Boolean = false,
    val comment: Comment? = null,
    val comments: List<Comment>? = null,
    val count: Int? = null,
    val error: String? = null
)

package com.example.mobsec_823.data

import android.content.Context
import com.example.mobsec_823.data.api.ClassResponse
import com.example.mobsec_823.data.api.CommentResponse
import com.example.mobsec_823.data.api.ForumPostResponse
import com.example.mobsec_823.data.api.GroupResponse
import com.example.mobsec_823.data.api.QuestionResponse
import com.example.mobsec_823.data.api.SimpleApi
import com.example.mobsec_823.data.api.SimpleResponse
import com.example.mobsec_823.data.api.UserResponse
import com.google.gson.Gson

/**
 * Simple database helper using bare-bones HTTP calls.
 * No Retrofit, no OkHttp complexity.
 */
object DatabaseHelper {
    private val gson = Gson()

    fun initialize(context: Context) {
        SimpleApi.initialize(context)
    }

    // ========== USER OPERATIONS ==========

    suspend fun getUserById(userId: Int): User? {
        val json = SimpleApi.get("/api/user/$userId") ?: return null
        val response = gson.fromJson(json, UserResponse::class.java)
        return if (response.success) response.user else null
    }

    suspend fun getAllUsers(): List<User> {
        val json = SimpleApi.get("/api/users") ?: return emptyList()
        val response = gson.fromJson(json, UserResponse::class.java)
        return if (response.success) response.users ?: emptyList() else emptyList()
    }

    // ========== CLASS OPERATIONS ==========

    suspend fun getAllClasses(): List<ClassEntity> {
        val json = SimpleApi.get("/api/classes") ?: return emptyList()
        val response = gson.fromJson(json, ClassResponse::class.java)
        return if (response.success) response.classes ?: emptyList() else emptyList()
    }

    suspend fun createClass(className: String): ClassEntity? {
        val body = gson.toJson(mapOf("class_name" to className))
        val json = SimpleApi.post("/api/classes", body) ?: return null
        val response = gson.fromJson(json, ClassResponse::class.java)
        return if (response.success) response.`class` else null
    }

    suspend fun updateClass(classId: Int, className: String): Boolean {
        val body = gson.toJson(mapOf("class_name" to className))
        val json = SimpleApi.put("/api/classes/$classId", body) ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    suspend fun deleteClass(classId: Int): Boolean {
        val json = SimpleApi.delete("/api/classes/$classId") ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    // ========== USER-CLASS OPERATIONS ==========

    suspend fun getClassUsers(classId: Int): List<User> {
        val json = SimpleApi.get("/api/classes/$classId/users") ?: return emptyList()
        val response = gson.fromJson(json, UserResponse::class.java)
        return if (response.success) response.users ?: emptyList() else emptyList()
    }

    suspend fun getTeachers(): List<User> {
        val json = SimpleApi.get("/api/users?role=teacher") ?: return emptyList()
        // Assuming your API returns a list of users
        val type = object : com.google.gson.reflect.TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun addUserToClass(classId: Int, userId: Int): Boolean {
        val json = SimpleApi.post("/api/classes/$classId/users/$userId") ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    suspend fun removeUserFromClass(classId: Int, userId: Int): Boolean {
        val json = SimpleApi.delete("/api/classes/$classId/users/$userId") ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    suspend fun getUserClasses(userId: Int): List<ClassEntity> {
        val json = SimpleApi.get("/api/users/$userId/classes") ?: return emptyList()
        val response = gson.fromJson(json, ClassResponse::class.java)
        return if (response.success) response.classes ?: emptyList() else emptyList()
    }

    // ========== FORUM POST OPERATIONS ==========

    suspend fun getClassPosts(classId: Int): List<ForumPost> {
        val json = SimpleApi.get("/api/classes/$classId/posts") ?: return emptyList()
        val response = gson.fromJson(json, ForumPostResponse::class.java)
        return if (response.success) response.posts ?: emptyList() else emptyList()
    }

    suspend fun createPost(classId: Int, userId: Int, title: String, content: String, imageUrl: String?): ForumPost? {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "title" to title,
            "content" to content,
            "image_url" to imageUrl
        ))
        val json = SimpleApi.post("/api/classes/$classId/posts", body) ?: return null
        val response = gson.fromJson(json, ForumPostResponse::class.java)
        return if (response.success) response.post else null
    }

    suspend fun updatePost(postId: Int, userId: Int, title: String, content: String, imageUrl: String?): Boolean {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "title" to title,
            "content" to content,
            "image_url" to imageUrl
        ))
        val json = SimpleApi.put("/api/posts/$postId", body) ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    suspend fun deletePost(postId: Int, userId: Int): Boolean {
        val body = gson.toJson(mapOf("user_id" to userId))
        val json = SimpleApi.deleteWithBody("/api/posts/$postId", body) ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    // ========== COMMENT OPERATIONS ==========

    suspend fun getPostComments(postId: Int): List<Comment> {
        val json = SimpleApi.get("/api/posts/$postId/comments") ?: return emptyList()
        val response = gson.fromJson(json, CommentResponse::class.java)
        return if (response.success) response.comments ?: emptyList() else emptyList()
    }

    suspend fun createComment(postId: Int, userId: Int, content: String): Comment? {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "content" to content
        ))
        val json = SimpleApi.post("/api/posts/$postId/comments", body) ?: return null
        val response = gson.fromJson(json, CommentResponse::class.java)
        return if (response.success) response.comment else null
    }

    suspend fun updateComment(commentId: Int, userId: Int, content: String): Boolean {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "content" to content
        ))
        val json = SimpleApi.put("/api/comments/$commentId", body) ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    suspend fun deleteComment(commentId: Int, userId: Int): Boolean {
        val body = gson.toJson(mapOf("user_id" to userId))
        val json = SimpleApi.deleteWithBody("/api/comments/$commentId", body) ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    //=========== Question Stuff =======
    suspend fun createQuestion(
        studentId: Int,
        classId: Int, // Changed from String to Int to match backend expectation
        teacherId: Int,
        question: String,
        priority: String
    ): Boolean {
        // The keys MUST match the backend's expected fields exactly:
        // student_id, class_id, teacher_id, question, priority
        val body = gson.toJson(
            mapOf(
                "student_id" to studentId,
                "class_id" to classId,
                "teacher_id" to teacherId,
                "question" to question, // Changed from "questionText" to "question"
                "priority" to priority
            )
        )

        val json = SimpleApi.post("/api/questions", body) ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }
    // Fetch questions sent TO a specific teacher
    suspend fun getQuestionsForTeacher(teacherId: Int): List<Question> {
        val json = SimpleApi.get("/api/teacher/$teacherId/questions") ?: return emptyList()
        val response = gson.fromJson(json, QuestionResponse::class.java)
        return if (response.success) response.questions ?: emptyList() else emptyList()
    }

    suspend fun answerQuestion(questionId: Int, answer: String): Boolean {
        val body = gson.toJson(mapOf("answer" to answer))
        val json = SimpleApi.put("/api/questions/$questionId/answer", body) ?: return false
        val response = gson.fromJson(json, SimpleResponse::class.java)
        return response.success
    }

    suspend fun getQuestionsByStudent(studentId: Int): List<Question> {
        val json = SimpleApi.get("/api/student/$studentId/questions") ?: return emptyList()
        val response = gson.fromJson(json, QuestionResponse::class.java)
        return if (response.success) response.questions ?: emptyList() else emptyList()
    }

    // ========== Group Stuff ========
    /**
     * Fetches the group details for a specific user within a class.
     */
    suspend fun getUserGroup(classId: Int, userId: Int): GroupEntity? {
        // This ensures the URL becomes /api/classes/1/users/1/group
        val json = SimpleApi.get("/api/classes/$classId/users/$userId/group") ?: return null
        val response = gson.fromJson(json, GroupResponse::class.java)
        return if (response.success) response.group else null
    }

    /**
     * Fetches students in a class who are not currently assigned to any group.
     */
    suspend fun getUnassignedStudents(classId: Int): List<User> {
        val json = SimpleApi.get("/api/classes/$classId/unassigned") ?: return emptyList()
        val response = gson.fromJson(json, UserResponse::class.java)
        return if (response.success) response.users ?: emptyList() else emptyList()
    }




    // ========== UTILITY ==========

    suspend fun testConnection(): Boolean {
        val json = SimpleApi.get("/api/health") ?: return false
        return json.contains("healthy")
    }
}

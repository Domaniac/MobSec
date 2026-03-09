package com.example.mobsec_823.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mobsec_823.data.api.ClassResponse
import com.example.mobsec_823.data.api.CommentResponse
import com.example.mobsec_823.data.api.ForumPostResponse
import com.example.mobsec_823.data.api.LocationResponse
import com.example.mobsec_823.data.api.GroupResponse
import com.example.mobsec_823.data.api.QuestionResponse
import com.example.mobsec_823.data.api.SimpleApi
import com.example.mobsec_823.data.api.SimpleResponse
import com.example.mobsec_823.data.api.UserResponse
import com.example.mobsec_823.data.api.LoginResponse
import com.example.mobsec_823.data.api.LoginResult
import com.example.mobsec_823.data.api.RegisterResult
import com.google.gson.Gson

/**
 * Simple database helper using bare-bones HTTP calls.
 * No Retrofit, no OkHttp complexity.
 */
object DatabaseHelper {
    private val gson = Gson()
    private const val TAG = "DatabaseHelper"

    fun initialize(context: Context) {
        SimpleApi.initialize(context)
    }

    private fun <T> safeParse(json: String, clazz: Class<T>): T? {
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            Log.e(TAG, "JSON Parse Error for ${clazz.simpleName}: ${e.message}. JSON: ${json.take(100)}")
            null
        }
    }

    // ========== USER OPERATIONS ==========

    suspend fun loginUser(username: String, passwordHash: String): LoginResult {
        val body = gson.toJson(mapOf("username" to username, "password" to passwordHash))
        val json = SimpleApi.post("/api/login", body)
            ?: return LoginResult.Failure("Network error")
        
        return try {
            val response = gson.fromJson(json, LoginResponse::class.java)
            if (response.success && response.user != null) {
                LoginResult.Success(response.user)
            } else {
                LoginResult.Failure(response.error ?: "Invalid credentials")
            }
        } catch (e: Exception) {
            LoginResult.Failure("Server error: ${e.message}")
        }
    }

    suspend fun registerUser(
        username: String,
        studentEmployeeNumber: String,
        passwordHash: String,
        role: String,
        fullName: String
    ): RegisterResult {
        val body = gson.toJson(mapOf(
            "username" to username,
            "student_employee_number" to studentEmployeeNumber,
            "password" to passwordHash,
            "role" to role,
            "full_name" to fullName
        ))
        val json = SimpleApi.post("/api/register", body) ?: return RegisterResult.Failure("Network error")
        Log.d(TAG, "Register response: $json")
        val response = safeParse(json, SimpleResponse::class.java)
        Log.d(TAG, "Parsed register response: success=${response?.success}, error=${response?.error}, message=${response?.message}")
        return if (response?.success == true) RegisterResult.Success else RegisterResult.Failure(response?.error ?: response?.message ?: "Registration failed")
    }

    suspend fun getUserById(userId: Int): User? {
        val json = SimpleApi.get("/api/user/$userId") ?: return null
        val response = safeParse(json, UserResponse::class.java)
        return if (response?.success == true) response.user else null
    }

    suspend fun getAllUsers(): List<User> {
        val json = SimpleApi.get("/api/users") ?: return emptyList()
        val response = safeParse(json, UserResponse::class.java)
        return if (response?.success == true) response.users ?: emptyList() else emptyList()
    }

    suspend fun updateUserProfile(userId: Int, username: String?): Boolean {
        val bodyMap = mutableMapOf<String, String>()
        if (username != null) bodyMap["username"] = username
        
        val body = gson.toJson(bodyMap)
        val json = SimpleApi.put("/api/user/$userId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun deleteUser(userId: Int): Boolean {
        val json = SimpleApi.delete("/api/user/$userId") ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    /**
     * Upload a profile image as binary (JPEG bytes) to the server.
     * Returns true on success, false on failure.
     */
    suspend fun uploadProfileImage(userId: Int, imageBytes: ByteArray): Boolean {
        val json = SimpleApi.putMultipartImageBytes(
            "/api/user/$userId/profile-image",
            imageBytes,
            "profile_$userId.jpg"
        ) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    // ========== CLASS OPERATIONS ==========

    suspend fun getAllClasses(): List<ClassEntity> {
        // Flask route is @app.route('/api/classes') - do NOT use trailing slash
        val json = SimpleApi.get("/api/classes") ?: return emptyList()
        val response = safeParse(json, ClassResponse::class.java)
        return if (response?.success == true) response.classes ?: emptyList() else emptyList()
    }

    suspend fun createClass(className: String): ClassEntity? {
        val body = gson.toJson(mapOf("class_name" to className))
        val json = SimpleApi.post("/api/classes", body) ?: return null
        val response = safeParse(json, ClassResponse::class.java)
        return if (response?.success == true) response.`class` else null
    }

    suspend fun updateClass(classId: Int, className: String): Boolean {
        val body = gson.toJson(mapOf("class_name" to className))
        val json = SimpleApi.put("/api/classes/$classId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun deleteClass(classId: Int): Boolean {
        val json = SimpleApi.delete("/api/classes/$classId") ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    // ========== USER-CLASS OPERATIONS ==========

    suspend fun getClassUsers(classId: Int): List<User> {
        val json = SimpleApi.get("/api/classes/$classId/users") ?: return emptyList()
        val response = safeParse(json, UserResponse::class.java)
        return if (response?.success == true) response.users ?: emptyList() else emptyList()
    }

    suspend fun getTeachers(): List<User> {
        val json = SimpleApi.get("/api/users?role=teacher") ?: return emptyList()
        return try {
            // Flask returns a plain array for role-filtered users
            val type = object : com.google.gson.reflect.TypeToken<List<User>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing teachers: ${e.message}")
            emptyList()
        }
    }

    suspend fun addUserToClass(classId: Int, userId: Int): Boolean {
        val json = SimpleApi.post("/api/classes/$classId/users/$userId") ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun removeUserFromClass(classId: Int, userId: Int): Boolean {
        val json = SimpleApi.delete("/api/classes/$classId/users/$userId") ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun getUserClasses(userId: Int): List<ClassEntity> {
        val json = SimpleApi.get("/api/users/$userId/classes") ?: return emptyList()
        val response = safeParse(json, ClassResponse::class.java)
        return if (response?.success == true) response.classes ?: emptyList() else emptyList()
    }

    // ========== FORUM POST OPERATIONS ==========

    suspend fun getClassPosts(classId: Int): List<ForumPost> {
        val json = SimpleApi.get("/api/classes/$classId/posts") ?: return emptyList()
        val response = safeParse(json, ForumPostResponse::class.java)
        return if (response?.success == true) response.posts ?: emptyList() else emptyList()
    }

    suspend fun createPost(classId: Int, userId: Int, title: String, content: String, imageUrl: String?): ForumPost? {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "title" to title,
            "content" to content,
            "image_url" to imageUrl
        ))
        val json = SimpleApi.post("/api/classes/$classId/posts", body) ?: return null
        val response = safeParse(json, ForumPostResponse::class.java)
        return if (response?.success == true) response.post else null
    }

    suspend fun updatePost(postId: Int, userId: Int, title: String, content: String, imageUrl: String?): Boolean {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "title" to title,
            "content" to content,
            "image_url" to imageUrl
        ))
        val json = SimpleApi.put("/api/posts/$postId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun deletePost(postId: Int, userId: Int): Boolean {
        val body = gson.toJson(mapOf("user_id" to userId))
        val json = SimpleApi.deleteWithBody("/api/posts/$postId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    // ========== COMMENT OPERATIONS ==========

    suspend fun getPostComments(postId: Int): List<Comment> {
        val json = SimpleApi.get("/api/posts/$postId/comments") ?: return emptyList()
        val response = safeParse(json, CommentResponse::class.java)
        return if (response?.success == true) response.comments ?: emptyList() else emptyList()
    }

    suspend fun createComment(postId: Int, userId: Int, content: String): Comment? {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "content" to content
        ))
        val json = SimpleApi.post("/api/posts/$postId/comments", body) ?: return null
        val response = safeParse(json, CommentResponse::class.java)
        return if (response?.success == true) response.comment else null
    }

    suspend fun updateComment(commentId: Int, userId: Int, content: String): Boolean {
        val body = gson.toJson(mapOf(
            "user_id" to userId,
            "content" to content
        ))
        val json = SimpleApi.put("/api/comments/$commentId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun deleteComment(commentId: Int, userId: Int): Boolean {
        val body = gson.toJson(mapOf("user_id" to userId))
        val json = SimpleApi.deleteWithBody("/api/comments/$commentId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    //=========== Question Stuff =======
    suspend fun createQuestion(
        studentId: Int,
        classId: Int,
        teacherId: Int,
        question: String,
        priority: String
    ): Boolean {
        val body = gson.toJson(
            mapOf(
                "student_id" to studentId,
                "class_id" to classId,
                "teacher_id" to teacherId,
                "question" to question,
                "priority" to priority
            )
        )

        val json = SimpleApi.post("/api/questions", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun getQuestionsForTeacher(teacherId: Int): List<Question> {
        val json = SimpleApi.get("/api/teacher/$teacherId/questions") ?: return emptyList()
        val response = safeParse(json, QuestionResponse::class.java)
        return if (response?.success == true) response.questions ?: emptyList() else emptyList()
    }

    suspend fun answerQuestion(questionId: Int, answer: String): Boolean {
        val body = gson.toJson(mapOf("answer" to answer))
        val json = SimpleApi.put("/api/questions/$questionId/answer", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun getQuestionsByStudent(studentId: Int): List<Question> {
        val json = SimpleApi.get("/api/student/$studentId/questions") ?: return emptyList()
        val response = safeParse(json, QuestionResponse::class.java)
        return if (response?.success == true) response.questions ?: emptyList() else emptyList()
    }

    suspend fun updateQuestion(questionId: Int, studentId: Int, question: String, priority: String): Boolean {
        val body = gson.toJson(mapOf(
            "student_id" to studentId,
            "question" to question,
            "priority" to priority
        ))
        val json = SimpleApi.put("/api/questions/$questionId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun deleteQuestion(questionId: Int, studentId: Int): Boolean {
        val body = gson.toJson(mapOf("student_id" to studentId))
        val json = SimpleApi.deleteWithBody("/api/questions/$questionId", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    // ========== Group Entity ========
    suspend fun getUserGroup(classId: Int, userId: Int): GroupEntity? {
        val json = SimpleApi.get("/api/classes/$classId/users/$userId/group") ?: return null
        val response = safeParse(json, GroupResponse::class.java)
        return if (response?.success == true) response.group else null
    }

    suspend fun getUnassignedStudents(classId: Int): List<User> {
        val json = SimpleApi.get("/api/classes/$classId/unassigned") ?: return emptyList()
        val response = safeParse(json, UserResponse::class.java)
        return if (response?.success == true) response.users ?: emptyList() else emptyList()
    }

    suspend fun getClassGroups(classId: Int): List<GroupEntity> {
        val json = SimpleApi.get("/api/classes/$classId/groups") ?: return emptyList()
        val response = safeParse(json, GroupResponse::class.java)
        return if (response?.success == true) response.groups ?: emptyList() else emptyList()
    }

    suspend fun createGroup(classId: Int, groupName: String): GroupEntity? {
        val body = gson.toJson(mapOf("class_id" to classId, "group_name" to groupName))
        val json = SimpleApi.post("/api/classes/$classId/groups", body) ?: return null
        val response = safeParse(json, GroupResponse::class.java)
        return if (response?.success == true) response.group else null
    }

    suspend fun deleteGroup(classId: Int, groupId: Int): Boolean {
        val json = SimpleApi.delete("/api/classes/$classId/groups/$groupId") ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun addUserToGroup(classId: Int, groupId: Int, userId: Int): Boolean {
        val body = gson.toJson(mapOf("user_id" to userId))
        val json = SimpleApi.post("/api/classes/$classId/groups/$groupId/members", body) ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    suspend fun removeUserFromGroup(classId: Int, groupId: Int, userId: Int): Boolean {
        val json = SimpleApi.delete("/api/classes/$classId/groups/$groupId/members/$userId") ?: return false
        val response = safeParse(json, SimpleResponse::class.java)
        return response?.success == true
    }

    // ========== Resource Operations ==========
    suspend fun getClassResources(classId: Int): List<ResourceEntity> {
        val json = SimpleApi.get("/api/classes/$classId/resources") ?: return emptyList()
        return try {
            // Flask returns a wrapper: {'success': True, 'resources': resources}
            val response = gson.fromJson(json, ResourceResponse::class.java)
            if (response.success) response.resources ?: emptyList() else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing resources: ${e.message}")
            emptyList()
        }
    }

    suspend fun createResource(
        classId: Int,
        teacherId: Int,
        title: String,
        description: String,
        resourceType: String,
        url: String?,
        fileUri: Uri?,
        context: Context,
        fileName: String?
    ): Boolean {
        return if (resourceType == "pdf" && fileUri != null) {
            val params = mapOf(
                "class_id" to classId.toString(),
                "teacher_id" to teacherId.toString(),
                "title" to title,
                "description" to description,
                "resource_type" to "pdf"
            )
            val json = SimpleApi.postMultipart("/api/resources", params, fileUri, "file", context, fileName)
            json?.contains("\"success\":true") == true
        } else {
            val body = gson.toJson(mapOf(
                "class_id" to classId,
                "teacher_id" to teacherId,
                "title" to title,
                "description" to description,
                "resource_type" to "link",
                "url" to url
            ))
            val json = SimpleApi.post("/api/resources", body) ?: return false
            json.contains("\"success\":true")
        }
    }

    suspend fun deleteResource(resourceId: Int): Boolean {
        val json = SimpleApi.delete("/api/resources/$resourceId") ?: return false
        return json.contains("\"success\":true")
    }

    // ========== LOCATION SHARING SERVICE ===========
    suspend fun updateLocation(userId: Int, lat: Double, lng: Double): Boolean {
        val body = gson.toJson(mapOf("user_id" to userId, "latitude" to lat, "longitude" to lng))
        // Note: Flask code doesn't show /api/users/location yet, but assuming it exists or will be added
        val json = SimpleApi.post("/api/users/location", body) ?: return false
        return json.contains("\"success\":true")
    }

    suspend fun getAllLocations(): List<UserLocation> {
        val json = SimpleApi.get("/api/users/locations") ?: return emptyList()
        return try {
            val response = gson.fromJson(json, LocationResponse::class.java)
            if (response.success) response.locations ?: emptyList() else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing locations: ${e.message}. JSON: ${json.take(100)}")
            emptyList()
        }
    }

    // ========== UTILITY ==========

    suspend fun testConnection(): Boolean {
        val json = SimpleApi.get("/api/health") ?: return false
        return json.contains("healthy")
    }
}

package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class Question(

    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("student_id")
    val studentId: Int,

    @SerializedName("class_id")
    val classId: Int,

    @SerializedName("teacher_id")
    val teacherId: Int,

    @SerializedName("question")
    val question: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("priority")
    val priority: String,

    @SerializedName("answer")
    val answer: String? = null,

    @SerializedName("student_full_name")
    val studentName: String?,

    @SerializedName("teacher_full_name")
    val teacherName: String?
)

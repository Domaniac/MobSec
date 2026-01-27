package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class Question(

    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("student_id")
    val studentId: Int,

    @SerializedName("class_id")
    val classId: String,

    @SerializedName("teacher_id")
    val teacherId: Int,

    @SerializedName("question")
    val question: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("priority")
    val priority: String
)

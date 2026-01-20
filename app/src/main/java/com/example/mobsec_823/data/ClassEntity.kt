package com.example.mobsec_823.data

import com.google.gson.annotations.SerializedName

data class ClassEntity(
    @SerializedName("class_id")
    val classId: Int,

    @SerializedName("class_name")
    val className: String
)

data class ClassRequest(
    @SerializedName("class_name")
    val className: String
)

package com.cameraviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cameras")
data class Camera(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = "",
    val lastSnapshot: String? = null
)
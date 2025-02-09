package com.cameraviewer.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Camera::class], version = 1)
abstract class CameraDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
}
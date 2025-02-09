package com.cameraviewer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras")
    fun getAllCameras(): Flow<List<Camera>>

    @Insert
    suspend fun insertCamera(camera: Camera): Long

    @Update
    suspend fun updateCamera(camera: Camera)

    @Delete
    suspend fun deleteCamera(camera: Camera)

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: Long): Camera?
}
package com.cameraviewer.di

import android.content.Context
import androidx.room.Room
import com.cameraviewer.data.CameraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCameraDatabase(
        @ApplicationContext context: Context
    ): CameraDatabase = Room.databaseBuilder(
        context,
        CameraDatabase::class.java,
        "camera_db"
    ).build()

    @Provides
    @Singleton
    fun provideCameraDao(db: CameraDatabase) = db.cameraDao()
}
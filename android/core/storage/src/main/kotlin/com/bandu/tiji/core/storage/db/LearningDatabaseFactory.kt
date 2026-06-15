package com.bandu.tiji.core.storage.db

import android.content.Context
import androidx.room.Room
import java.io.File

object LearningDatabaseFactory {
    fun open(
        context: Context,
        databaseFile: File,
    ): LearningDatabase {
        require(databaseFile.parentFile?.let { it.exists() || it.mkdirs() } != false) {
            "Unable to create database directory"
        }
        return Room.databaseBuilder(
            context.applicationContext,
            LearningDatabase::class.java,
            databaseFile.absolutePath,
        ).build()
    }
}

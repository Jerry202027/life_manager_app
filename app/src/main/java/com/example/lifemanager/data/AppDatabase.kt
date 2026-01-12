package com.example.lifemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Version 升級為 2 (因為修改了 Task Schema)
@Database(entities = [Task::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "life_manager_db")
                    .fallbackToDestructiveMigration() // 開發中方便，直接清空重建
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
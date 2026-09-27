package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MistakeEntity::class,
        BookmarkEntity::class,
        ActiveSessionEntity::class,
        ExamRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HyperionDatabase : RoomDatabase() {

    abstract fun mistakeDao(): MistakeDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun examRecordDao(): ExamRecordDao

    companion object {
        @Volatile
        private var INSTANCE: HyperionDatabase? = null

        fun getInstance(context: Context): HyperionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HyperionDatabase::class.java,
                    "hyperion_marina.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

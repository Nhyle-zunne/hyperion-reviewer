package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MistakeEntity::class,
        BookmarkEntity::class,
        ActiveSessionEntity::class,
        ExamRecordEntity::class,
        QuestionEntity::class,
        DrillSessionEntity::class,
        com.nhyle.hyperionmaritime.data.studytime.StudyTimeSegmentEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class HyperionDatabase : RoomDatabase() {

    abstract fun mistakeDao(): MistakeDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun examRecordDao(): ExamRecordDao
    abstract fun questionDao(): QuestionDao
    abstract fun drillSessionDao(): DrillSessionDao
    abstract fun studyTimeSegmentDao(): com.nhyle.hyperionmaritime.data.studytime.StudyTimeSegmentDao

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

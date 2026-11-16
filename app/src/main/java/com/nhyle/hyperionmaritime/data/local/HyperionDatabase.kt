package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 8,
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
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE questions ADD COLUMN imageAttachmentsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        @Volatile
        private var INSTANCE: HyperionDatabase? = null

        fun getInstance(context: Context): HyperionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HyperionDatabase::class.java,
                    "hyperion_marina.db"
                ).addMigrations(MIGRATION_7_8).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

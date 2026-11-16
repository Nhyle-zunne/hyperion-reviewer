package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ImageOnlyQuestionMigrationTest {
    @Test fun versionSevenUpgradePreservesQuestionAndBookmarkWithoutInventingMedia() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val name = "image-only-migration.db"
        context.deleteDatabase(name)
        try {
            // Keep every other Room table, reconstruct the actual version-7 question schema.
            Room.databaseBuilder(context, HyperionDatabase::class.java, name).build().let { db ->
                db.openHelper.writableDatabase
                db.close()
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("DROP TABLE questions")
                db.execSQL("""CREATE TABLE questions (id TEXT NOT NULL PRIMARY KEY, track TEXT NOT NULL,
                    competence TEXT NOT NULL, functionId TEXT NOT NULL, topic TEXT NOT NULL,
                    questionText TEXT NOT NULL, optionsJson TEXT NOT NULL, correctAnswerIndex INTEGER NOT NULL,
                    explanation TEXT NOT NULL, difficulty TEXT NOT NULL)""")
                db.execSQL("CREATE INDEX index_questions_track_competence ON questions (track, competence)")
                db.execSQL("CREATE INDEX index_questions_track ON questions (track)")
                db.execSQL("""INSERT INTO questions VALUES ('legacy', 'ENGINE_OIC_EW', 'C1', 'F1', 'Charts',
                    'Existing prompt', '["North","South"]', 1, 'Existing explanation', 'EASY')""")
                db.execSQL("INSERT INTO bookmarks (questionId, addedTimestamp) VALUES ('legacy', 123456)")
                db.version = 7
            }
            val upgraded = Room.databaseBuilder(context, HyperionDatabase::class.java, name)
                .addMigrations(HyperionDatabase.MIGRATION_7_8).build()
            try {
                val rows = upgraded.questionDao().getQuestionsByTrack("ENGINE_OIC_EW")
                assertEquals(1, rows.size)
                val q = rows.single()
                assertEquals("legacy", q.id)
                assertEquals("Existing prompt", q.questionText)
                assertEquals("[\"North\",\"South\"]", q.optionsJson)
                assertEquals(1, q.correctAnswerIndex)
                assertEquals("[]", q.imageAttachmentsJson)
                assertEquals(123456L, upgraded.bookmarkDao().getAllBookmarksOnce().single().addedTimestamp)
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
    }
}

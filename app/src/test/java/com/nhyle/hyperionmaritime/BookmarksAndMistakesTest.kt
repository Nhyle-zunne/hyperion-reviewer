package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.BookmarkEntity
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.MistakeEntity
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BookmarksAndMistakesTest {

    private lateinit var db: HyperionDatabase
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HyperionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testBookmarkDaoFlowIntegrity() = runTest {
        val dao = db.bookmarkDao()
        dao.insertBookmark(BookmarkEntity(questionId = "101"))
        dao.insertBookmark(BookmarkEntity(questionId = "102"))
        dao.insertBookmark(BookmarkEntity(questionId = "103"))

        val initialList = dao.getAllBookmarks().first()
        assertEquals(3, initialList.size)

        dao.removeBookmark("102")
        val afterRemoval = dao.getAllBookmarks().first()
        assertEquals(2, afterRemoval.size)
        assertTrue(afterRemoval.none { it.questionId == "102" })

        dao.clearAllBookmarks()
        val afterClear = dao.getAllBookmarks().first()
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun testMistakesDaoFlowIntegrity() = runTest {
        val dao = db.mistakeDao()
        dao.insertOrUpdate(MistakeEntity(questionId = "201", timesFailed = 1))
        dao.insertOrUpdate(MistakeEntity(questionId = "202", timesFailed = 2))
        dao.insertOrUpdate(MistakeEntity(questionId = "203", timesFailed = 3))

        val initialList = dao.getAllMistakes().first()
        assertEquals(3, initialList.size)

        dao.removeMistake("202")
        val afterRemoval = dao.getAllMistakes().first()
        assertEquals(2, afterRemoval.size)
        assertTrue(afterRemoval.none { it.questionId == "202" })

        dao.clearAllMistakes()
        val afterClear = dao.getAllMistakes().first()
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun testViewModelStateFlowOperations() = runTest {
        val viewModel = ReviewerViewModel(context.applicationContext as android.app.Application)
        viewModel.clearAllBookmarks()
        viewModel.clearAllMistakes()

        val initialBookmarks = viewModel.bookmarkedIds.first()
        assertTrue(initialBookmarks.isEmpty())

        val initialMistakes = viewModel.mistakeEntities.first()
        assertTrue(initialMistakes.isEmpty())
    }

    @Test
    fun testEmptyListClearingSafelyExecute() = runTest {
        val bookmarkDao = db.bookmarkDao()
        val mistakeDao = db.mistakeDao()

        val bCount = bookmarkDao.clearAllBookmarks()
        assertEquals(0, bCount)

        val mCount = mistakeDao.clearAllMistakes()
        assertEquals(0, mCount)
    }
}

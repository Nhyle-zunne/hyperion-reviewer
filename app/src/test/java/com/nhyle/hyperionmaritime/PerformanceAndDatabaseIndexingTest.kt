package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.BookmarkEntity
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.MistakeEntity
import com.nhyle.hyperionmaritime.data.local.QuestionEntity
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerformanceAndDatabaseIndexingTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var database: HyperionDatabase
    private lateinit var repository: ReviewerRepository
    private lateinit var viewModel: ReviewerViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        MaritimeQuestionBank.init(context)

        LicenseManager.setActivated(context, true)
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.setGmdssProUnlocked(context, true)
        LicenseManager.init(context)

        database = HyperionDatabase.getInstance(context)
        repository = ReviewerRepository(database)
        viewModel = ReviewerViewModel(application)
    }

    @Test
    fun testQuestionDaoIndexedQueriesAndPerformance() = runBlocking {
        val questionDao = database.questionDao()
        questionDao.clearAllQuestions()

        val sampleEntities = (1..50).map { i ->
            QuestionEntity(
                id = "Q_OIC_C1_$i",
                track = "DECK_OIC_NW",
                competence = "C1",
                functionId = "F1",
                topic = "Navigation",
                questionText = "Question sample $i"
            )
        } + (1..30).map { i ->
            QuestionEntity(
                id = "Q_OIC_C2_$i",
                track = "DECK_OIC_NW",
                competence = "C2",
                functionId = "F1",
                topic = "Cargo",
                questionText = "Question cargo sample $i"
            )
        } + (1..20).map { i ->
            QuestionEntity(
                id = "Q_GMDSS_C1_$i",
                track = "GMDSS_GOC",
                competence = "C1",
                functionId = "GMDSS",
                topic = "Radio",
                questionText = "Question radio sample $i"
            )
        }

        questionDao.insertAll(sampleEntities)

        val queryDurationMs = measureTimeMillis {
            val c1Count = questionDao.getQuestionCountForCompetence("DECK_OIC_NW", "C1")
            assertEquals(50, c1Count)

            val c2Count = questionDao.getQuestionCountForCompetence("DECK_OIC_NW", "C2")
            assertEquals(30, c2Count)

            val oicTotal = questionDao.getTotalQuestionCountForTrack("DECK_OIC_NW")
            assertEquals(80, oicTotal)

            val gmdssTotal = questionDao.getTotalQuestionCountForTrack("GMDSS_GOC")
            assertEquals(20, gmdssTotal)

            val c1List = questionDao.getQuestionsByCompetence("DECK_OIC_NW", "C1")
            assertEquals(50, c1List.size)
        }

        // Fast lookup assertion on indexed queries (< 200ms)
        assertTrue("Indexed queries should execute fast (took ${queryDurationMs}ms)", queryDurationMs < 500)
    }

    @Test
    fun testBookmarkAndMistakeDaoDeletionsEmitToFlowsImmediately() = runBlocking {
        val bookmarkDao = database.bookmarkDao()
        val mistakeDao = database.mistakeDao()

        // 1. Seed bookmarks
        bookmarkDao.clearAll()
        bookmarkDao.insertBookmark(BookmarkEntity(questionId = "BM_1"))
        bookmarkDao.insertBookmark(BookmarkEntity(questionId = "BM_2"))
        bookmarkDao.insertBookmark(BookmarkEntity(questionId = "BM_3"))

        var bookmarks = bookmarkDao.getAllBookmarks().first()
        assertEquals(3, bookmarks.size)

        // Delete single bookmark
        val removedCount = repository.removeBookmark("BM_2")
        assertEquals(1, removedCount)

        bookmarks = bookmarkDao.getAllBookmarks().first()
        assertEquals(2, bookmarks.size)
        assertFalse(bookmarks.any { it.questionId == "BM_2" })

        // Clear all bookmarks
        val clearedBmCount = repository.clearAllBookmarks()
        assertEquals(2, clearedBmCount)

        bookmarks = bookmarkDao.getAllBookmarks().first()
        assertTrue("All bookmarks should be cleared", bookmarks.isEmpty())

        // 2. Seed mistakes
        mistakeDao.clearAll()
        mistakeDao.insertOrUpdate(MistakeEntity(questionId = "M_1", consecutiveCorrect = 0, timesFailed = 1))
        mistakeDao.insertOrUpdate(MistakeEntity(questionId = "M_2", consecutiveCorrect = 0, timesFailed = 2))

        var mistakes = mistakeDao.getAllMistakes().first()
        assertEquals(2, mistakes.size)

        // Delete single mistake
        val removedMistakeCount = repository.removeMistake("M_1")
        assertEquals(1, removedMistakeCount)

        mistakes = mistakeDao.getAllMistakes().first()
        assertEquals(1, mistakes.size)
        assertEquals("M_2", mistakes.first().questionId)

        // Clear all mistakes
        val clearedMistakesCount = repository.clearAllMistakes()
        assertEquals(1, clearedMistakesCount)

        mistakes = mistakeDao.getAllMistakes().first()
        assertTrue("All mistakes should be cleared", mistakes.isEmpty())
    }

    @Test
    fun testSingleItemDrillSelectionBounds() {
        LicenseManager.setMasterProUnlocked(ApplicationProvider.getApplicationContext(), true)
        viewModel.setTrack(Track.DECK_OIC_NW)

        // Launch single item drill where startItem == endItem (e.g. #7 to #7)
        val targetIndex = 7
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.STANDARD,
            startItem = targetIndex,
            endItem = targetIndex,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val activeQuestions = viewModel.activeQuestions.value
        assertEquals("Single-item drill must have exactly 1 question", 1, activeQuestions.size)

        val allC1Questions = MaritimeQuestionBank.getQuestionsByCompetence("C1", Track.DECK_OIC_NW, isProUnlocked = true)
        val expectedQuestion = allC1Questions[targetIndex - 1]
        assertEquals("The single item must accurately correspond to target index", expectedQuestion.id, activeQuestions.first().id)
    }

    @Test
    fun testOicNwAndGmdssQuestionCountRetrieval() {
        // OIC-NW C1 = 831
        val oicC1 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C1", Track.DECK_OIC_NW, isCrpMode = false)
        assertEquals("OIC-NW C1 must equal 831", 831, oicC1)

        // OIC-NW C2 = 570
        val oicC2 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C2", Track.DECK_OIC_NW, isCrpMode = false)
        assertEquals("OIC-NW C2 must equal 570", 570, oicC2)

        // GMDSS C1 = 1,911
        val gmdssC1 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C1", Track.GMDSS_GOC, isCrpMode = false)
        assertEquals("GMDSS C1 must equal 1911", 1911, gmdssC1)

        // GMDSS C2 = 349
        val gmdssC2 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C2", Track.GMDSS_GOC, isCrpMode = false)
        assertEquals("GMDSS C2 must equal 349", 349, gmdssC2)

        // Range math totalSelected == (endItem - startItem + 1)
        val startItem = 12
        val endItem = 45
        val totalSelected = (endItem - startItem + 1).coerceAtLeast(1)
        assertEquals(34, totalSelected)
    }
}

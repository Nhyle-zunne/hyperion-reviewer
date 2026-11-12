package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.*
import com.nhyle.hyperionmaritime.data.model.*
import com.nhyle.hyperionmaritime.data.repository.*
import com.nhyle.hyperionmaritime.data.sync.LearningProgressSyncAdapter
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ImageOnlyQuestionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val ref = QuestionImageRef("a".repeat(64), "image/png", "question", "Synthetic chart")
    private fun media(placement: String = "question") = JSONArray().put(JSONObject()
        .put("sha256", ref.sha256).put("mime", ref.mime).put("placement", placement).put("alt", ref.alt))
    private fun question() = Question("image-only-regression", Track.ENGINE_OIC_EW, "F1", "C1", "Charts",
        "", listOf("North", "South", "East", "West"), 2, "Chart explanation", Difficulty.EASY,
        imageAttachments = listOf(ref))

    @Test fun numberedCsvAcceptsOnlyValidPromptImagesWithoutChangingIdsOrOptions() {
        val file = File("src/main/assets/../../../build/image-only-regression.csv").apply { parentFile.mkdirs() }
        fun cell(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        val rows = listOf(
            listOf("No.", "Question", "A", "B", "C", "D", "Answer", "Explanation", "HYPERION_MEDIA"),
            listOf("7", "", "North", "South", "East", "West", "C", "Reason", media().toString()),
            listOf("8", "", "North", "South", "East", "West", "C", "Reason", ""),
            listOf("9", "", "North", "South", "East", "West", "C", "Reason", "invalid"),
            listOf("10", "", "North", "South", "East", "West", "C", "Reason", media("explanation").toString()),
            listOf("11", "Text prompt", "North", "South", "East", "West", "C", "Reason", "")
        )
        file.writeText(rows.joinToString("\n") { it.joinToString(",", transform = ::cell) })
        val previousContext = MaritimeQuestionBank.appContext
        MaritimeQuestionBank.appContext = null
        try {
            val competence = MaritimeCompetenceRegistry.getCompetencesForTrack(Track.ENGINE_OIC_EW).first()
            val loader = NewTrackQuestionBank::class.java.getDeclaredMethod("loadCompetenceCsv",
                String::class.java, CompetenceInfo::class.java, String::class.java).apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val parsed = loader.invoke(NewTrackQuestionBank, "../../../build/image-only-regression.csv", competence, "EW") as List<Question>
            assertEquals(listOf("EW-${competence.competenceId}-0007", "EW-${competence.competenceId}-0011"), parsed.map { it.id })
            assertEquals("", parsed.first().questionText)
            assertEquals(question().options, parsed.first().options)
            assertEquals(2, parsed.first().correctAnswerIndex)
            assertEquals(listOf(ref), parsed.first().imageAttachments)
        } finally { file.delete(); MaritimeQuestionBank.appContext = previousContext }
    }

    @Test fun sessionSnapshotPreservesImageOnlyQuestionAndAnswerOrder() {
        val q = question()
        val snapshot = ActiveSessionSnapshot("image-only-test", q.track.name, "PRACTICE", "C1", listOf(q),
            mapOf(q.id to "East"), mapOf(q.id to true), 0, 1)
        try {
            ActiveSessionSnapshotManager.saveSnapshot(context, snapshot)
            val restored = checkNotNull(ActiveSessionSnapshotManager.loadSnapshotById(context, snapshot.sessionId))
            assertEquals(snapshot.questions, restored.questions)
            assertEquals(snapshot.selectedAnswers, restored.selectedAnswers)
        } finally { ActiveSessionSnapshotManager.getSnapshotFile(context, snapshot.sessionId).delete() }
    }

    @Test fun cloudBookmarkRoundTripPreservesImageOnlyPromptAndRejectsMissingPromptMedia() = runBlocking {
        MaritimeQuestionBank.init(context)
        val database = HyperionDatabase.getInstance(context)
        val id = question().id
        fun item(suffix: String, images: JSONArray?) = JSONObject().put("id", id + suffix)
            .put("track", question().track.name).put("functionId", "F1").put("competenceId", "C1")
            .put("questionText", "").put("topic", "Charts").put("options", JSONArray(question().options))
            .put("correctAnswerIndex", 2).put("difficulty", "EASY").put("savedAt", 1750000000000L)
            .apply { if (images != null) put("imageAttachments", images) }
        val items = JSONArray().put(item("", media())).put(item("-absent", null))
            .put(item("-explanation", media("explanation")))
            .put(item("-malformed", JSONArray().put(JSONObject().put("placement", "question"))))
        val snapshot = JSONObject().put("storage", JSONObject().put("hyperion-bookmarks",
            JSONObject().put("version", 0).put("state", JSONObject().put("items", items)).toString()))
        try {
            LearningProgressSyncAdapter.applyCloudSnapshot(context, snapshot)
            val saved = database.bookmarkDao().getAllBookmarksOnce().filter { it.questionId.startsWith(id) }
            assertEquals(listOf(id), saved.map { it.questionId })
            val output = LearningProgressSyncAdapter.mergeNativeIntoSnapshot(context, null)
            val store = JSONObject(output.getJSONObject("storage").getString("hyperion-bookmarks"))
            val restored = store.getJSONObject("state").getJSONArray("items")
            val q = (0 until restored.length()).map { restored.getJSONObject(it) }.first { it.getString("id") == id }
            assertEquals("", q.getString("questionText"))
            assertEquals(question().options, (0 until q.getJSONArray("options").length()).map { q.getJSONArray("options").getString(it) })
            assertEquals(media().toString(), q.getJSONArray("imageAttachments").toString())
        } finally {
            for (suffix in listOf("", "-absent", "-explanation", "-malformed")) database.bookmarkDao().removeBookmark(id + suffix)
        }
    }
}

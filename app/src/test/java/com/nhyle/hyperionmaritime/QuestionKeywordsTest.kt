package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.QuestionKeywords
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuestionKeywordsTest {
    @Test fun failedCommitRestoresPreviousSelectionsForLaterLoads() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("failed_keyword_commit", Context.MODE_PRIVATE)
        prefs.edit().clear().putStringSet("stem", setOf("0")).commit()
        var failNextCommit = true
        val wrapped = java.lang.reflect.Proxy.newProxyInstance(
            android.content.SharedPreferences::class.java.classLoader,
            arrayOf(android.content.SharedPreferences::class.java)
        ) { _, method, args ->
            if (method.name == "edit") {
                val editor = prefs.edit()
                java.lang.reflect.Proxy.newProxyInstance(
                    android.content.SharedPreferences.Editor::class.java.classLoader,
                    arrayOf(android.content.SharedPreferences.Editor::class.java)
                ) { proxy, editMethod, editArgs ->
                    val result = editMethod.invoke(editor, *(editArgs ?: emptyArray()))
                    when {
                        editMethod.name == "commit" && failNextCommit -> { failNextCommit = false; false }
                        result is android.content.SharedPreferences.Editor -> proxy
                        else -> result
                    }
                }
            } else method.invoke(prefs, *(args ?: emptyArray()))
        } as android.content.SharedPreferences
        val store = QuestionKeywords(wrapped)
        assertFalse(store.saveAll(mapOf("stem" to setOf(1), "choice" to setOf(0))))
        assertEquals(setOf(0), store.load("stem", 2))
        assertTrue(store.load("choice", 1).isEmpty())
    }

    @Test fun punctuationPreservesLegacyWordPositions() {
        val tokens = QuestionKeywords.tokens("Ship, 12% + ⚓!")
        assertEquals(listOf("Ship", "12", ",", "%", "+", "⚓", "!"), tokens.map { it.value })
    }

    @Test fun punctuationInsideLegacyWordsHasItsOwnToken() {
        assertEquals(listOf("ship's", "give-way", "'", "-"),
            QuestionKeywords.tokens("ship's give-way").map { it.value })
    }

    @Test fun choicesFollowTextAndAreScopedToQuestionRevision() {
        val store = QuestionKeywords(ApplicationProvider.getApplicationContext())
        val stem = store.key("DECK", "choice", "Stem?")
        val key = store.choiceKey(stem, "Ship, 12%")
        assertTrue(store.saveAll(mapOf(key to setOf(0, 2))))
        assertEquals(setOf(0, 2), QuestionKeywords(ApplicationProvider.getApplicationContext()).load(store.choiceKey(stem, listOf("Other", "Ship, 12%")[1]), 5))
        assertTrue(store.load(store.choiceKey(stem, "Ship, 13%"), 5).isEmpty())
        assertTrue(store.load(store.choiceKey(store.key("DECK", "choice", "Revised"), "Ship, 12%"), 5).isEmpty())
    }

    @Test fun persistsMultipleWordsAndRemovalAcrossInstances() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = QuestionKeywords(context)
        val key = store.key("DECK", "1", "Which vessel gives way?")
        store.save(key, setOf(1, 3))
        assertEquals(setOf(1, 3), QuestionKeywords(context).load(key, 4))
        store.save(key, setOf(3))
        assertEquals(setOf(3), QuestionKeywords(context).load(key, 4))
        store.save(key, emptySet())
        assertTrue(QuestionKeywords(context).load(key, 4).isEmpty())
    }

    @Test fun isolatesTracksQuestionsAndRevisedText() {
        val store = QuestionKeywords(ApplicationProvider.getApplicationContext())
        val key = store.key("DECK", "1", "Which vessel gives way?")
        store.save(key, setOf(1))
        assertTrue(store.load(store.key("ENGINE", "1", "Which vessel gives way?"), 4).isEmpty())
        assertTrue(store.load(store.key("DECK", "2", "Which vessel gives way?"), 4).isEmpty())
        assertTrue(store.load(store.key("DECK", "1", "Revised question"), 4).isEmpty())
        assertEquals(setOf(1), store.load(key, 4))
    }

    @Test fun rejectsOutOfBoundsPositions() {
        val store = QuestionKeywords(ApplicationProvider.getApplicationContext())
        val key = store.key("DECK", "bounds", "Two words")
        store.save(key, setOf(-1, 0, 2, 100))
        assertEquals(setOf(0), store.load(key, 2))
    }
}

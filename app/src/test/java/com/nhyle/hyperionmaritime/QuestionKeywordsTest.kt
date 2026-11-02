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

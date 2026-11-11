package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LearningProgressStorageComparatorTest {
    @Test fun equivalentNestedObjectsDoNotCreateNewCloudWrites() {
        val remote = JSONObject().put(
            "hyperion-history",
            """{"version":0,"state":{"savedSessions":[],"entries":[{"id":"session-A","finishedAt":22,"correct":10}]}}"""
        ).put("hyperion-bookmarks", """{"state":{"items":[{"id":"q1","savedAt":11}]}, "version":0}""")
        val android = JSONObject().put(
            "hyperion-bookmarks",
            """{"version":0,"state":{"items":[{"savedAt":11,"id":"q1"}]}}"""
        ).put(
            "hyperion-history",
            """{"state":{"entries":[{"correct":10,"finishedAt":22,"id":"session-A"}],"savedSessions":[]},"version":0}"""
        )
        assertTrue(LearningProgressStorageComparator.sameStorage(remote, android))
        assertEquals(
            LearningProgressStorageComparator.storageFingerprint(remote),
            LearningProgressStorageComparator.storageFingerprint(android)
        )
    }

    @Test fun changedAnswersOrRemovedCollectionsMustStillUpload() {
        val before = JSONObject().put(
            "hyperion-history",
            """{"state":{"entries":[{"id":"s1","correct":3}]},"version":0}"""
        )
        val after = JSONObject().put(
            "hyperion-history",
            """{"state":{"entries":[{"id":"s1","correct":4}]},"version":0}"""
        )
        assertFalse(LearningProgressStorageComparator.sameStorage(before, after))
        assertFalse(LearningProgressStorageComparator.sameStorage(before, JSONObject()))
        assertFalse(LearningProgressStorageComparator.sameStorage(before, null))
        assertTrue(LearningProgressStorageComparator.sameStorage(null, null))
    }

    @Test fun arrayOrderAndRawStringsRemainSignificant() {
        val a = JSONObject().put("hyperion-history", """{"state":{"entries":[{"id":"a"},{"id":"b"}]}}""")
        val b = JSONObject().put("hyperion-history", """{"state":{"entries":[{"id":"b"},{"id":"a"}]}}""")
        assertFalse(LearningProgressStorageComparator.sameStorage(a, b))
        assertFalse(LearningProgressStorageComparator.sameStorage(
            JSONObject().put("note", "  hi  "), JSONObject().put("note", "hi")
        ))
    }

    @Test fun malformedJsonAndNonJsonFieldsCannotSilentlyHideEdits() {
        val a = JSONObject().put("x", "{broken").put("y", "plain")
        val b = JSONObject().put("x", "{broken ").put("y", "plain")
        assertFalse(LearningProgressStorageComparator.sameStorage(a, b))
        assertEquals(
            LearningProgressStorageComparator.storageFingerprint(a),
            LearningProgressStorageComparator.storageFingerprint(JSONObject(a.toString()))
        )
    }
}

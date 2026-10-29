package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockExamCycleTest {

    @Test
    fun testMockExamNoRepeatsUntilCycleComplete() {
        val track = Track.GMDSS_GOC
        // GMDSS has 2 competencies: C1 (quota 85), C2 (quota 15), total 100
        val exam1 = MaritimeQuestionBank.getMockExamDataset(track, isProUnlocked = true)
        val exam2 = MaritimeQuestionBank.getMockExamDataset(track, isProUnlocked = true)

        assertEquals(100, exam1.size)
        assertEquals(100, exam2.size)

        // No duplicates within each exam
        assertEquals(100, exam1.map { it.id }.distinct().size)
        assertEquals(100, exam2.map { it.id }.distinct().size)

        // Exam 1 and Exam 2 should not have any overlapping questions in C1 or C2 since pools (1,911 and 349) are much larger than quota (85 and 15)
        val set1 = exam1.map { it.id }.toSet()
        val set2 = exam2.map { it.id }.toSet()
        val intersection = set1.intersect(set2)
        assertTrue("Questions repeated between consecutive exams: $intersection", intersection.isEmpty())
    }
}

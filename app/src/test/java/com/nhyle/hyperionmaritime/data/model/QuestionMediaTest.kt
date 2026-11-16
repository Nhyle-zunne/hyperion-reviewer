package com.nhyle.hyperionmaritime.data.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuestionMediaTest {
    private val sha = "a".repeat(64)
    private val sample = """[{"sha256":"$sha","mime":"image/webp","placement":"question","alt":"Radar plotting diagram"}]"""

    @Test fun exactSharedCsvContractSupportsLicensedQuestionDiagram() {
        val header = listOf("No.", "Question", "A", "B", "C", "D",
            "Correct", "Explanation", "HYPERION_STATUS", "HYPERION_MEDIA")
        val row = listOf("1", "Which bearing?", "A", "B", "C", "D",
            "A", "Radar image", "ACTIVE", sample)
        val refs = QuestionMedia.fromCsvRow(header, row)
        assertEquals(1, refs.size)
        assertEquals(sha, refs.single().sha256)
        assertEquals("question", refs.single().placement)
        assertEquals("Radar plotting diagram", refs.single().alt)
        assertEquals(emptyList<QuestionImageRef>(), QuestionMedia.fromCsvRow(header.dropLast(1), row))
        assertEquals(emptyList<QuestionImageRef>(), QuestionMedia.fromCsvRow(header, row.dropLast(1)))
    }

    @Test fun explanationsMayCarryUpToFourImages() {
        val refs = (0 until 4).map {
            """{"sha256":"${('a'.code + it).toChar().toString().repeat(64)}","mime":"image/png","placement":"explanation","alt":"Illustration $it"}"""
        }
        assertEquals(4, QuestionMedia.parse("[" + refs.joinToString(",") + "]").size)
        assertTrue(QuestionMedia.parse("[" + (refs + refs[0]).joinToString(",") + "]").isEmpty())
    }

    @Test fun unsafeUrlsPathsLargePayloadsAndDuplicateImagesFailClosed() {
        val cases = listOf(
            "[]", "not json",
            """[{"sha256":"../../private","mime":"image/webp","placement":"question","alt":"Unsafe"}]""",
            """[{"sha256":"$sha","mime":"text/html","placement":"question","alt":"Script"}]""",
            """[{"sha256":"$sha","mime":"image/webp","placement":"option","alt":"Untrusted"}]""",
            """[{"sha256":"$sha","mime":"image/webp","placement":"question","alt":""}]""",
            """[{"sha256":"$sha","mime":"image/webp","placement":"question","alt":"Diagram","url":"https://untrusted.test/image"}]""",
            " ".repeat(2049),
            "[" + listOf(sample.removePrefix("[").removeSuffix("]"),sample.removePrefix("[").removeSuffix("]")).joinToString(",") + "]"
        )
        for (raw in cases) {
            assertTrue("Accepted unsafe image metadata: ${raw.take(60)}", QuestionMedia.parse(raw).isEmpty())
        }
    }

    @Test fun existingQuestionsNeedNoSchemaMigration() {
        val question = Question("fixed-id", Track.DECK_OIC_NW, "F1", "C1",
            "Charts", "Read the chart", listOf("A", "B"), 0, "Because",
            Difficulty.MODERATE)
        assertTrue(question.imageAttachments.isEmpty())
        assertEquals("fixed-id", question.id)
    }
}

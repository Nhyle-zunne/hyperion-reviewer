package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.Question

/**
 * Question-Selection Dispatcher for Hyperion Reviewer.
 *
 * Implements the fail-safe "Prioritize Unseen Items" algorithm:
 * - When prioritizeUnseen is false: preserves existing selection/randomization behavior.
 * - When prioritizeUnseen is true:
 *   1. Partitions eligible pool into Bucket A (unseen) and Bucket B (seen).
 *   2. Randomizes Bucket A and Bucket B independently.
 *   3. Selects min(N, U) from Bucket A.
 *   4. If U < N, safely backfills remaining slots from Bucket B.
 *   5. Never returns duplicates unless the input pool contains duplicates.
 *   6. Handles empty pool, empty history, all seen, all unseen, and overflow gracefully.
 */
object QuestionSelectionDispatcher {

    fun selectQuestions(
        eligiblePool: List<Question>,
        requestedCount: Int,
        seenQuestionIds: Set<String>,
        prioritizeUnseen: Boolean,
        shuffleQuestions: Boolean = false
    ): List<Question> {
        if (eligiblePool.isEmpty() || requestedCount <= 0) {
            return emptyList()
        }

        val effectiveCount = requestedCount.coerceAtMost(eligiblePool.size)

        if (!prioritizeUnseen) {
            val list = if (shuffleQuestions) eligiblePool.shuffled() else eligiblePool
            return list.take(effectiveCount)
        }

        // Step 3: Partition the eligible pool
        val bucketA = mutableListOf<Question>() // Unseen
        val bucketB = mutableListOf<Question>() // Seen

        for (q in eligiblePool) {
            if (q.id !in seenQuestionIds) {
                bucketA.add(q)
            } else {
                bucketB.add(q)
            }
        }

        // Step 4 & 5: Select requested number of questions prioritizing unseen
        val unseenTake = minOf(effectiveCount, bucketA.size)
        val neededFromSeen = effectiveCount - unseenTake

        return if (shuffleQuestions) {
            val unseenShuffled = bucketA.shuffled().take(unseenTake)
            val seenShuffled = if (neededFromSeen > 0) bucketB.shuffled().take(neededFromSeen) else emptyList()
            unseenShuffled + seenShuffled
        } else {
            val unseenOrdered = bucketA.take(unseenTake)
            val seenOrdered = if (neededFromSeen > 0) bucketB.take(neededFromSeen) else emptyList()
            unseenOrdered + seenOrdered
        }
    }
}

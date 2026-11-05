package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * T1.5 — инвариант числа кругов по умолчанию: суммарное число текстов рассказа
 * не менее minimumPlannedReplies (R1.11, E1.11); подсчёт — только реплики
 * обычных ходов и концовка, без входящих в рассказ хвостов (R1.11).
 */
class DefaultRoundsInvariantTest {

    /** Реплики обычных ходов плюс концовка: хвосты в подсчёт не входят (R1.11). */
    private fun plannedTexts(finished: Match): Int =
        finished.snapshot().lines.count { line: StoryLine -> line !is StoryLine.Tail }

    @Test
    fun defaultRoundsGuaranteeMinimumPlannedReplies() { // R1.11, T1.5
        listOf(2, 3, 4, 6, 11, 12, 13).forEach { count: Int ->
            val squad: List<Participant> = (1..count).map { index: Int -> Participant(null) }
            val finished: Match = playToNaturalFinish(squad)
            val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
            val texts: Int = plannedTexts(finished)
            assertTrue(
                texts >= GameRules.MINIMUM_PLANNED_REPLIES,
                "партия из $count участников: реплик и концовок $texts, ожидается не менее ${GameRules.MINIMUM_PLANNED_REPLIES}",
            )
            assertTrue(
                story.entries.size > texts,
                "хвосты входят в рассказ (R4.1), но в гарантию не входят (R1.11): строк ${story.entries.size}",
            )
        }
    }

    @Test
    fun defaultRoundsMatchFormula() { // R1.8: партии создаются с defaultRoundCount
        listOf(2, 3, 4, 6, 11, 12, 13).forEach { count: Int ->
            val squad: List<Participant> = (1..count).map { index: Int -> Participant(null) }
            val match: Match = startMatchOrFail(squad)
            assertEquals(GameRules.defaultRoundCount(count), match.roundCount, "участников: $count")
        }
    }

    @Test
    fun sixteenParticipantsGetSingleRoundAndEnoughTexts() { // R1.1, R1.11, E1.11, R4.1
        val squad: List<Participant> = (1..16).map { index: Int -> Participant(null) }
        val match: Match = startMatchOrFail(squad)
        assertEquals(1, match.roundCount)
        val finished: Match = playToNaturalFinish(squad, match.roundCount)
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(16, plannedTexts(finished), "реплик и концовка больше минимального: один круг — по тексту на участника")
        assertEquals(15, finished.snapshot().lines.count { line: StoryLine -> line is StoryLine.Tail })
        assertEquals(31, story.entries.size, "плюс 15 хвостов: все написанные части в рассказе (R4.1)")
    }
}

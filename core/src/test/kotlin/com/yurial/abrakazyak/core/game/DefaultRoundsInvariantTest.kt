package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * T1.5 — инвариант числа кругов по умолчанию: суммарное число текстов рассказа
 * не менее minimumPlannedReplies (R1.11, E1.11).
 */
class DefaultRoundsInvariantTest {

    @Test
    fun defaultRoundsGuaranteeMinimumPlannedReplies() { // R1.11, T1.5
        listOf(2, 3, 4, 6, 11, 12, 13).forEach { count: Int ->
            val squad: List<Participant> = (1..count).map { index: Int -> Participant(null) }
            val finished: Match = playToNaturalFinish(squad)
            val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
            assertTrue(
                story.entries.size >= GameRules.MINIMUM_PLANNED_REPLIES,
                "партия из $count участников: текстов ${story.entries.size}, ожидается не менее ${GameRules.MINIMUM_PLANNED_REPLIES}",
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
    fun sixteenParticipantsGetSingleRoundAndEnoughTexts() { // R1.1, R1.11, E1.11
        val squad: List<Participant> = (1..16).map { index: Int -> Participant(null) }
        val match: Match = startMatchOrFail(squad)
        assertEquals(1, match.roundCount)
        val finished: Match = playToNaturalFinish(squad, match.roundCount)
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(16, story.entries.size, "текстов больше минимального: один круг — по тексту на участника")
    }
}

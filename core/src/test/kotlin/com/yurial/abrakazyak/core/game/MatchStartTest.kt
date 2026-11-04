package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * T1.1, T1.3 — проверки старта партии: участники, число кругов, вычисляемое значение по умолчанию.
 */
class MatchStartTest {

    @Test
    fun emptyParticipantsRejected() { // R1.6, T1.3
        val result: GameResult<Match> = startMatch(emptyList())
        assertEquals(GameError.EmptyParticipants, result.requireFailure())
    }

    @Test
    fun roundCountLessThanOneRejected() { // R1.7, T1.3; нецелые значения в типизированном API непредставимы
        val zero = startMatch(threeParticipants(), 0).requireFailure()
        val negative = startMatch(threeParticipants(), -3).requireFailure()
        assertEquals(GameError.InvalidRoundCount, zero)
        assertEquals(GameError.InvalidRoundCount, negative)
    }

    @Test
    fun emptyParticipantsCheckedBeforeRoundCount() { // порядок проверок старта задокументирован в startMatch
        assertEquals(GameError.EmptyParticipants, startMatch(emptyList(), 0).requireFailure())
    }

    @Test
    fun explicitRoundCountAccepted() { // R1.7
        val match: Match = startMatchOrFail(threeParticipants(), 2)
        assertEquals(2, match.roundCount)
        assertEquals(6, match.totalTurns) // R1.9: ходов participants × rounds
        assertEquals(TurnPhase.AWAITING_CHUNK, match.phase)
        assertEquals(0, match.turnIndex)
        assertEquals(threeParticipants(), match.participants)
        assertTrue(match.toString().contains("rounds=2"), "текстовое представление описывает состояние партии")
    }

    @Test
    fun defaultRoundCountComputedFromParticipantCount() { // R1.8, E1.11, T1.3
        assertEquals(6, startMatchOrFail(participants(2)).roundCount, "двое — шесть кругов")
        assertEquals(4, startMatchOrFail(participants(3)).roundCount, "трое — четыре круга")
        assertEquals(2, startMatchOrFail(participants(6)).roundCount, "шестеро — два круга")
        assertEquals(1, startMatchOrFail(participants(16)).roundCount, "шестнадцать — один круг")
    }

    @Test
    fun singleParticipantMatchPlaysFullCycle() { // R1.1, R1.5, T1.1
        val match: Match = startMatchOrFail(listOf(Participant("Один")), 2)
        assertEquals(2, match.totalTurns)
        val afterTurn: Match = playOrdinaryTurn(match, "начало", "дальше")
        assertEquals(TurnPhase.AWAITING_FINISH, afterTurn.phase)
        assertEquals(1, afterTurn.turnIndex)
        assertEquals("Один", afterTurn.currentParticipant?.name) // ход возвращается тому же участнику
    }

    @Test
    fun singleParticipantSingleRoundStartsOnFinishingTurn() { // R1.10, R1.9 (граничный случай)
        val match: Match = startMatchOrFail(listOf(Participant("Один")), 1)
        assertEquals(TurnPhase.AWAITING_FINISH, match.phase)
        assertEquals(GameError.InvalidStep, match.submitChunk("вне окна").requireFailure())
        assertEquals(GameError.InvalidStep, match.submitTail("вне окна").requireFailure())
        val finished: Match = match.finishWithReply("конец").requireSuccess()
        assertEquals(listOf("конец"), finished.finishedStory()?.entries?.map { it.text })
    }

    @Test
    fun matchAboveRecommendedPlayersCyclesThroughQueue() { // R1.1, R1.5, T1.1: полного предела числа участников нет
        val names: List<Participant> = participants(11)
        var match: Match = startMatchOrFail(names, 2)
        assertEquals(22, match.totalTurns)
        repeat(11) { index: Int ->
            assertEquals(names[index], match.currentParticipant, "ход ${match.turnIndex}")
            match = playOrdinaryTurn(match, "реплика-$index", "хвост-$index")
        }
        assertEquals(names[0], match.currentParticipant, "за последним участником следует первый (R1.5)")
    }

    @Test
    fun namedPartiallyNamedAndUnnamedPartiesPlayIdentically() { // R1.3, T1.2
        val fullyNamed = listOf(Participant("Аня"), Participant("Боря"), Participant("Вера"))
        val partiallyNamed = listOf(Participant("Гоша"), Participant(null), Participant("Даша"))
        val unnamed = listOf(Participant(null), Participant(null), Participant(null))
        listOf(fullyNamed, partiallyNamed, unnamed).forEach { squad: List<Participant> ->
            var match: Match = startMatchOrFail(squad, 1)
            repeat(2) { index: Int ->
                match = playOrdinaryTurn(match, "реплика-$index", "хвост-$index")
            }
            assertEquals(TurnPhase.AWAITING_FINISH, match.phase)
        }
    }

    private fun threeParticipants(): List<Participant> =
        listOf(Participant("Аня"), Participant("Боря"), Participant("Вера"))

    private fun participants(count: Int): List<Participant> =
        (1..count).map { index: Int -> Participant("Участник-$index") }
}

package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * T1.2 (авторство), T2.1–T2.3 — проверки хода, видимости и граничных текстов.
 */
class MatchTurnTest {

    private val squad: List<Participant> =
        listOf(Participant("Аня"), Participant("Боря"), Participant(null))

    @Test
    fun firstTurnContextEmptyAndSecondTurnSeesOnlyFirstTail() { // R2.1, R2.2, R2.3, T2.1
        val fresh: Match = startMatchOrFail(squad, 2)
        assertEquals(null, fresh.visibleContext, "контекст первого хода пуст")
        val afterChunk: Match = fresh.submitChunk("Жил-был кот, который мечтал о великих открытиях").requireSuccess()
        assertEquals(null, afterChunk.visibleContext, "на шаге хвоста контекст своего хода не меняется")
        val nextTurn: Match = afterChunk.submitTail("и он пошёл...").requireSuccess()
        assertEquals("и он пошёл...", nextTurn.visibleContext, "второй ход видит только хвост первого (R2.3)")
        val afterSecondChunk: Match = nextTurn.submitChunk("второй продолжает").requireSuccess()
        assertEquals("и он пошёл...", afterSecondChunk.visibleContext, "контекст хода не раскрывает другие тексты")
    }

    @Test
    fun chunkAtLimitAcceptedAndBeyondRejected() { // R2.4, T2.2
        val accepted: Match = startMatchOrFail(squad, 2)
            .submitChunk("а".repeat(GameRules.MAX_STORY_CHUNK_LENGTH)).requireSuccess()
        assertEquals(TurnPhase.AWAITING_TAIL, accepted.phase)
        val rejected: Match = startMatchOrFail(squad, 2)
        assertEquals(
            GameError.TextTooLong,
            rejected.submitChunk("б".repeat(GameRules.MAX_STORY_CHUNK_LENGTH + 1)).requireFailure(),
        )
    }

    @Test
    fun chunkWithoutNonWhitespaceRejected() { // R2.5, T2.2
        val fresh: Match = startMatchOrFail(squad, 2)
        assertEquals(GameError.EmptyText, fresh.submitChunk("").requireFailure())
        assertEquals(GameError.EmptyText, fresh.submitChunk("   ").requireFailure())
        assertEquals(GameError.EmptyText, fresh.submitChunk("\t\n ").requireFailure())
    }

    @Test
    fun tailAtLimitAcceptedAndBeyondRejected() { // R2.6, T2.3
        val fresh: Match = startMatchOrFail(squad, 2)
        val withChunk: Match = fresh.submitChunk("реплика").requireSuccess()
        val exactlyLimit: String = "х".repeat(GameRules.MAX_VISIBLE_TAIL_LENGTH)
        val accepted: Match = withChunk.submitTail(exactlyLimit).requireSuccess()
        assertEquals(exactlyLimit, accepted.visibleContext)
        val next: Match = accepted.submitChunk("дальше").requireSuccess()
        assertEquals(GameError.TextTooLong, next.submitTail("у".repeat(GameRules.MAX_VISIBLE_TAIL_LENGTH + 1)).requireFailure())
    }

    @Test
    fun tailWithoutNonWhitespaceRejected() { // R2.7, T2.3
        val fresh: Match = startMatchOrFail(squad, 2)
        val withChunk: Match = fresh.submitChunk("реплика").requireSuccess()
        assertEquals(GameError.EmptyText, withChunk.submitTail("").requireFailure())
        assertEquals(GameError.EmptyText, withChunk.submitTail("    ").requireFailure())
    }

    @Test
    fun stepsOutOfOrderRejectedAsInvalidStep() { // game/invalid-step: повторный шаг и шаг вне очереди
        val fresh: Match = startMatchOrFail(squad, 2)
        assertEquals(GameError.InvalidStep, fresh.submitTail("хвост без реплики").requireFailure())
        val withChunk: Match = fresh.submitChunk("реплика").requireSuccess()
        assertEquals(GameError.InvalidStep, withChunk.submitChunk("повторная реплика").requireFailure())
        // устаревший снимок того же хода: реплика уже подтверждена на актуальном состоянии (R3.6)
        assertEquals(GameError.InvalidStep, fresh.submitTail("хвост по устаревшему снимку").requireFailure())
    }

    @Test
    fun failedOperationKeepsStateUnchanged() { // Error handling: ошибка не изменяет состояние партии
        val fresh: Match = startMatchOrFail(squad, 2)
        val snapshotBefore: MatchSnapshot = fresh.snapshot()
        fresh.submitChunk("").requireFailure()
        fresh.submitChunk("длинная ".repeat(60)).requireFailure() // > maxStoryChunkLength
        fresh.submitTail("вне окна").requireFailure()
        assertEquals(snapshotBefore, fresh.snapshot())
        val playable: Match = fresh.submitChunk("партия жива").requireSuccess()
        assertEquals(TurnPhase.AWAITING_TAIL, playable.phase)
    }

    @Test
    fun chunksTrackAuthorInTurnOrder() { // R1.4, R1.5, T1.2: реплика связана с автором
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "первая", "хвост-0")
        match = playOrdinaryTurn(match, "вторая", "хвост-1")
        match = match.submitChunk("третья").requireSuccess()
        val authorIndexes: List<Int> = match.chunks.map { it.authorIndex }
        assertEquals(listOf(0, 1, 2), authorIndexes)
    }

    @Test
    fun currentTurnChunkVisibleOnlyOnTailStep() { // ui R5.8, T5.3: контекст экрана хвоста
        var match: Match = startMatchOrFail(squad, 1) // ходы 0 и 1 обычные, ход 2 — ход завершения
        assertNull(match.currentTurnChunk(), "до подтверждения реплики первого хода текста нет")
        match = match.submitChunk("реплика первого хода").requireSuccess()
        assertEquals(
            "реплика первого хода",
            match.currentTurnChunk(),
            "на шаге хвоста доступна подтверждённая реплика текущего хода (ui R5.8)",
        )
        match = match.submitTail("хвост первого хода").requireSuccess()
        assertNull(match.currentTurnChunk(), "на шаге реплики следующего хода реплики текущего хода нет")
        match = match.submitChunk("реплика второго хода").requireSuccess()
        assertEquals("реплика второго хода", match.currentTurnChunk(), "видна реплика именно текущего хода, а не предыдущего")
        match = match.submitTail("хвост второго хода").requireSuccess()
        assertEquals(TurnPhase.AWAITING_FINISH, match.phase)
        assertNull(match.currentTurnChunk(), "на ходе завершения подтверждённой реплики нет")
        val finished: Match = match.finishWithTemplate("ru-1").requireSuccess()
        assertNull(finished.currentTurnChunk(), "после завершения партии текста нет")
    }

    @Test
    fun turnPassesAroundAndFinishingTurnIsLastParticipants() { // R1.5, R1.10
        var match: Match = startMatchOrFail(squad, 1)
        match = playOrdinaryTurn(match, "раз", "хвост-0")
        assertEquals(squad[1], match.currentParticipant)
        match = playOrdinaryTurn(match, "два", "хвост-1")
        assertEquals(squad[2], match.currentParticipant, "третий участник последнего круга завершает партию (R1.10)")
        assertEquals(TurnPhase.AWAITING_FINISH, match.phase)
    }
}

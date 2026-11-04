package com.yurial.abrakazyak.core.game

import kotlin.test.assertIs

/** Вспомогательные операции тестов над [GameResult]. */
internal fun GameResult<Match>.requireSuccess(): Match = assertIs<GameResult.Success<Match>>(this).value

internal fun GameResult<Match>.requireFailure(): GameError = assertIs<GameResult.Failure>(this).error

/** Стартует партию, ожидая успеха (в тестах параметры всегда допустимы). */
internal fun startMatchOrFail(participants: List<Participant>, roundCount: Int? = null): Match =
    startMatch(participants, roundCount).requireSuccess()

/** Выполняет один обычный ход целиком: реплика, затем видимый хвост (R2.1). */
internal fun playOrdinaryTurn(match: Match, chunkText: String, tailText: String): Match =
    match.submitChunk(chunkText).requireSuccess().submitTail(tailText).requireSuccess()

/**
 * Доводит партию до естественного завершения (T1.4, T1.5): каждый обычный ход —
 * реплика и хвост с автонумерацией, ход завершения — финальной репликой.
 */
internal fun playToNaturalFinish(participants: List<Participant>, roundCount: Int? = null): Match {
    var match: Match = startMatchOrFail(participants, roundCount)
    var turn: Int = 0
    while (match.phase != TurnPhase.FINISHED) {
        match = if (match.phase == TurnPhase.AWAITING_CHUNK) {
            match.submitChunk("реплика-$turn").requireSuccess()
        } else if (match.phase == TurnPhase.AWAITING_TAIL) {
            match.submitTail("хвост-$turn").requireSuccess()
        } else {
            match.finishWithReply("финал-$turn").requireSuccess()
        }
        turn += 1
    }
    return match
}

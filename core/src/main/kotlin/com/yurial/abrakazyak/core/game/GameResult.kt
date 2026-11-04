package com.yurial.abrakazyak.core.game

/**
 * Результат операции модели партии (specs/game.md, Interface и Error handling):
 * либо новое неизменяемое состояние партии, либо ошибка без изменения состояния.
 */
sealed interface GameResult<out T> {

    /** Операция выполнена: [value] — новое актуальное состояние партии. */
    data class Success<out T>(val value: T) : GameResult<T>

    /** Операция отклонена: [error] — причина; состояние партии не изменилось. */
    data class Failure(val error: GameError) : GameResult<Nothing>
}

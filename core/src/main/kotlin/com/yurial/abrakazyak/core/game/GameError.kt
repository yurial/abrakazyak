package com.yurial.abrakazyak.core.game

/**
 * Ошибки модели партии; коды совпадают с полным списком из specs/game.md (Interface,
 * Error handling). Ошибка никогда не изменяет состояние партии: операция возвращает
 * [GameResult.Failure], и партия остаётся ровно в том состоянии, в котором была.
 */
sealed interface GameError {

    /** Машинночитаемый код из specs/game.md, например `game/empty-participants`. */
    val code: String

    /** `startMatch` с пустым списком участников (game/empty-participants, R1.6). */
    data object EmptyParticipants : GameError {
        override val code: String = "game/empty-participants"
    }

    /**
     * `startMatch` с числом кругов менее единицы (game/invalid-round-count, R1.7).
     * Нецелые значения в типизированном Kotlin API непредставимы: параметр имеет тип `Int?`.
     */
    data object InvalidRoundCount : GameError {
        override val code: String = "game/invalid-round-count"
    }

    /**
     * Операция вне окна текущего хода (game/invalid-step, R1.10, R3.6, Error handling):
     * завершение уже переданного хода, обычный шаг на ходе завершения,
     * повторный шаг в том же ходе, операция с устаревшим снимком состояния.
     */
    data object InvalidStep : GameError {
        override val code: String = "game/invalid-step"
    }

    /** Операция над завершённой партией (game/match-finished, R1.10, R3.1). */
    data object MatchFinished : GameError {
        override val code: String = "game/match-finished"
    }

    /** Текст не содержит ни одного непробельного символа (game/empty-text, R2.5, R2.7). */
    data object EmptyText : GameError {
        override val code: String = "game/empty-text"
    }

    /** Текст превышает лимит реплики или хвоста (game/text-too-long, R2.4, R2.6). */
    data object TextTooLong : GameError {
        override val code: String = "game/text-too-long"
    }

    /** `templateId` отсутствует в списке `templateEndings(languageKey)` (game/unknown-template, R3.3). */
    data object UnknownTemplate : GameError {
        override val code: String = "game/unknown-template"
    }
}

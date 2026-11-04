package com.yurial.abrakazyak.core.game

/**
 * Шаг хода (game/step, R2.1): на шаге реплики ждут `game/story-chunk`,
 * на шаге хвоста — `game/visible-tail`. Ход завершения не содержит
 * обычных шагов (game/finishing-turn, R1.10); завершённая партия шагов не имеет.
 */
enum class TurnPhase {

    /** Обычный ход: ожидается реплика (game/story-chunk, R2.1). */
    AWAITING_CHUNK,

    /** Обычный ход: реплика подтверждена, ожидается видимый хвост (game/visible-tail, R2.1). */
    AWAITING_TAIL,

    /** Ход завершения (game/finishing-turn, R1.10): обычные шаги недопустимы, ожидается завершение. */
    AWAITING_FINISH,

    /** Партия завершена (R3.2, R3.3): обычные операции возвращают game/match-finished. */
    FINISHED,
}

package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Трассировка кодов ошибок на полный список specs/game.md (Interface, Error handling).
 */
class GameErrorTest {

    @Test
    fun gameErrorCodesMatchSpecList() {
        assertEquals("game/empty-participants", GameError.EmptyParticipants.code)
        assertEquals("game/invalid-round-count", GameError.InvalidRoundCount.code)
        assertEquals("game/invalid-step", GameError.InvalidStep.code)
        assertEquals("game/match-finished", GameError.MatchFinished.code)
        assertEquals("game/empty-text", GameError.EmptyText.code)
        assertEquals("game/text-too-long", GameError.TextTooLong.code)
        assertEquals("game/unknown-template", GameError.UnknownTemplate.code)
    }
}

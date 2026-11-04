package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Проверки конфигурации и шаблонных концовок (specs/game.md, Configuration, Tests T3.2/R3.5).
 */
class GameRulesTest {

    @Test
    fun fixedConstantsMatchSpecConfiguration() { // R1.2, R2.4, R2.6, R1.11 — значения Configuration
        assertEquals(300, GameRules.MAX_STORY_CHUNK_LENGTH, "maxStoryChunkLength")
        assertEquals(80, GameRules.MAX_VISIBLE_TAIL_LENGTH, "maxVisibleTailLength")
        assertEquals(10, GameRules.RECOMMENDED_MAX_PLAYERS, "recommendedMaxPlayers")
        assertEquals(12, GameRules.MINIMUM_PLANNED_REPLIES, "minimumPlannedReplies")
    }

    @Test
    fun defaultRoundCountIsCeilingQuotientAtLeastOne() { // R1.8, E1.11
        assertEquals(12, GameRules.defaultRoundCount(1))
        assertEquals(6, GameRules.defaultRoundCount(2), "двое — шесть кругов")
        assertEquals(4, GameRules.defaultRoundCount(3), "трое — четыре круга")
        assertEquals(3, GameRules.defaultRoundCount(4))
        assertEquals(3, GameRules.defaultRoundCount(5))
        assertEquals(2, GameRules.defaultRoundCount(6), "шестеро — два круга")
        assertEquals(2, GameRules.defaultRoundCount(11))
        assertEquals(1, GameRules.defaultRoundCount(12))
        assertEquals(1, GameRules.defaultRoundCount(13))
        assertEquals(1, GameRules.defaultRoundCount(16), "шестнадцать — один круг")
    }

    @Test
    fun russianEndingsMatchSpecTexts() { // R3.3 — дословные тексты из Configuration
        val endings: List<TemplateEnding> = GameRules.templateEndings("ru")
        assertEquals(
            listOf(
                "Вот такая история получилась.",
                "Вот такие дела, собачка.",
                "Такой у этой истории замечательный конец.",
            ),
            endings.map(TemplateEnding::text),
        )
        assertEquals(listOf("ru-1", "ru-2", "ru-3"), endings.map(TemplateEnding::templateId))
        endings.forEach { assertEquals("ru", it.languageKey) }
    }

    @Test
    fun englishEndingsMatchSpecTexts() { // R3.3 — дословные тексты из Configuration
        val endings: List<TemplateEnding> = GameRules.templateEndings("en")
        assertEquals(
            listOf(
                "And that's the story we got.",
                "That's how it went, folks.",
                "And so the story came to a wonderful end.",
            ),
            endings.map(TemplateEnding::text),
        )
        assertEquals(listOf("en-1", "en-2", "en-3"), endings.map(TemplateEnding::templateId))
        endings.forEach { assertEquals("en", it.languageKey) }
    }

    @Test
    fun everyLanguageSetIsNotEmpty() { // R3.5
        GameRules.templateLanguageKeys.forEach { languageKey: String ->
            assertTrue(GameRules.templateEndings(languageKey).isNotEmpty(), "набор '$languageKey' непуст")
        }
    }

    @Test
    fun unknownLanguageKeyReturnsEmptyList() {
        assertTrue(GameRules.templateEndings("de").isEmpty())
    }

    @Test
    fun templateLookupResolvesKnownIdAndRejectsUnknown() { // R3.3, precondition game/unknown-template
        assertEquals("ru", GameRules.findTemplateEnding("ru-2")?.languageKey)
        assertEquals("en", GameRules.findTemplateEnding("en-3")?.languageKey)
        assertNull(GameRules.findTemplateEnding("no-such-template"))
    }
}

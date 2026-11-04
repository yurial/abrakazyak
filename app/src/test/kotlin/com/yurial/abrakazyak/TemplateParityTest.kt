package com.yurial.abrakazyak

import com.yurial.abrakazyak.core.game.GameRules
import com.yurial.abrakazyak.ui.UiLanguages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Паритет шаблонных концовок с языками интерфейса (ui R9.5): множество ключей
 * `templateEndingsByLanguage` совпадает со множеством `uiLanguages`; проверка
 * выполняется на данных `:core` (game Configuration, R3.5).
 */
class TemplateParityTest {

    @Test
    fun templateLanguageKeysMatchUiLanguages() {
        assertEquals(UiLanguages.UI_LANGUAGES, GameRules.templateLanguageKeys)
    }

    @Test
    fun everyLanguageHasNonEmptyEndings() {
        for (languageKey in UiLanguages.UI_LANGUAGES) {
            val endings = GameRules.templateEndings(languageKey)
            assertTrue(endings.isNotEmpty(), "language $languageKey has no template endings")
        }
    }

    @Test
    fun templateIdsAreUniqueAcrossLanguages() {
        val ids: List<String> = UiLanguages.UI_LANGUAGES
            .flatMap { GameRules.templateEndings(it) }
            .map { it.templateId }
        assertEquals(ids.size, ids.toSet().size)
    }
}

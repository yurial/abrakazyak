package com.yurial.abrakazyak.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/** Разрешение ключа языка интерфейса (ui R9.4, Configuration). */
class UiLanguagesTest {

    @Test
    fun supportedSystemLanguageIsKept() {
        assertEquals("ru", UiLanguages.resolve("ru"))
        assertEquals("en", UiLanguages.resolve("en"))
    }

    @Test
    fun unsupportedSystemLanguageFallsBack() {
        assertEquals(UiLanguages.FALLBACK_LANGUAGE, UiLanguages.resolve("de"))
        assertEquals(UiLanguages.FALLBACK_LANGUAGE, UiLanguages.resolve("fr"))
    }

    @Test
    fun absentSystemLanguageFallsBack() {
        assertEquals(UiLanguages.FALLBACK_LANGUAGE, UiLanguages.resolve(null))
        assertEquals(UiLanguages.FALLBACK_LANGUAGE, UiLanguages.resolve(""))
    }

    @Test
    fun fallbackLanguageIsSupported() {
        kotlin.test.assertTrue(UiLanguages.FALLBACK_LANGUAGE in UiLanguages.UI_LANGUAGES)
    }
}

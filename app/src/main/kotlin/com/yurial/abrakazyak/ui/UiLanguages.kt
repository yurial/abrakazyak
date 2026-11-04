package com.yurial.abrakazyak.ui

/**
 * Языки интерфейса (ui Configuration): `uiLanguages` и `fallbackLanguage`.
 * Ресурсы приложения (`values`, `values-en`) локализуются системой Android;
 * этот объект согласует ключи шаблонных концовок `:core` (ui R9.5) с ключами
 * `uiLanguages`.
 */
object UiLanguages {

    /** `uiLanguages` (ui Configuration): языки интерфейса первой версии (ui R9.1). */
    val UI_LANGUAGES: Set<String> = setOf("ru", "en")

    /** `fallbackLanguage` (ui Configuration): язык при неподдерживаемом системном (ui R9.4). */
    const val FALLBACK_LANGUAGE: String = "ru"

    /**
     * Разрешает ключ языка интерфейса из кода системного языка устройства (ui R9.4):
     * поддерживаемый ключ возвращается как есть, любой другой заменяется
     * [FALLBACK_LANGUAGE].
     */
    fun resolve(systemLanguage: String?): String =
        if (systemLanguage != null && systemLanguage in UI_LANGUAGES) systemLanguage else FALLBACK_LANGUAGE
}

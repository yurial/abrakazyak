package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Шаблонная концовка (game/template-ending, R3.3): готовый текст из
 * `templateEndingsByLanguage` для ключа языка [languageKey]. Идентификатор
 * [templateId] уникален среди всех наборов и выбирается в
 * [Match.finishWithTemplate].
 */
data class TemplateEnding(
    val templateId: String,
    val languageKey: String,
    val text: String,
)

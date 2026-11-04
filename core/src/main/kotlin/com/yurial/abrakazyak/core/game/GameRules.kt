package com.yurial.abrakazyak.core.game

/**
 * Фиксированная конфигурация игры — раздел Configuration specs/game.md.
 * Все константы fixed; изменение значений — изменение спецификации.
 */
object GameRules {

    /**
     * `maxStoryChunkLength` (Configuration, R2.4): максимальная длина реплики
     * и финальной реплики (R3.2) в символах, включая пробелы; жёсткий предел ввода.
     * Символ считается как `String.length` (единицы UTF-16).
     */
    const val MAX_STORY_CHUNK_LENGTH: Int = 300

    /**
     * `maxVisibleTailLength` (Configuration, R2.6): максимальная длина видимого
     * хвоста в символах, включая пробелы; жёсткий предел ввода.
     */
    const val MAX_VISIBLE_TAIL_LENGTH: Int = 80

    /**
     * `recommendedMaxPlayers` (Configuration, R1.2): рекомендация числа участников
     * в тексте правил; модель верхнего предела не накладывает (R1.1).
     */
    const val RECOMMENDED_MAX_PLAYERS: Int = 10

    /**
     * `minimumPlannedReplies` (Configuration, R1.11): минимальное суммарное число
     * текстов готового рассказа при старте без явного числа кругов.
     */
    const val MINIMUM_PLANNED_REPLIES: Int = 12

    private val endingsByLanguage: Map<String, List<TemplateEnding>> = mapOf(
        "ru" to listOf(
            TemplateEnding("ru-1", "ru", "Вот такая история получилась."),
            TemplateEnding("ru-2", "ru", "Вот такие дела, собачка."),
            TemplateEnding("ru-3", "ru", "Такой у этой истории замечательный конец."),
        ),
        "en" to listOf(
            TemplateEnding("en-1", "en", "And that's the story we got."),
            TemplateEnding("en-2", "en", "That's how it went, folks."),
            TemplateEnding("en-3", "en", "And so the story came to a wonderful end."),
        ),
    )

    /** Ключи языков набора `templateEndingsByLanguage` (Configuration, R3.3, R3.5). */
    val templateLanguageKeys: Set<String>
        get() = endingsByLanguage.keys

    /**
     * `defaultRoundCount` (Configuration, R1.8): наибольшее из единицы и округлённого
     * вверх частного [MINIMUM_PLANNED_REPLIES] и числа участников; обеспечивает
     * не менее [MINIMUM_PLANNED_REPLIES] текстов в рассказе (R1.11, E1.11).
     *
     * @param participantCount число участников, не менее единицы (партия непуста — R1.6).
     */
    fun defaultRoundCount(participantCount: Int): Int =
        maxOf(1, (MINIMUM_PLANNED_REPLIES + participantCount - 1) / participantCount)

    /**
     * `templateEndings(languageKey)` (Interface, R3.3): список шаблонных концовок
     * для ключа языка [languageKey]; для неизвестного ключа список пуст,
     * для каждого ключа из [templateLanguageKeys] список непуст (R3.5).
     */
    fun templateEndings(languageKey: String): List<TemplateEnding> =
        endingsByLanguage[languageKey].orEmpty()

    /**
     * Поиск концовки по идентификатору среди всех наборов (R3.3): идентификаторы
     * уникальны между языками, поэтому `finishWithTemplate` проверяет
     * `templateId` против объединённого списка всех языков.
     */
    internal fun findTemplateEnding(templateId: String): TemplateEnding? =
        endingsByLanguage.values.flatten().find { it.templateId == templateId }
}

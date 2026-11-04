package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Готовый рассказ (game/finished-story, R4.1): все реплики и концовка в порядке
 * написания, каждая с новой строки ([text]); хвосты в рассказ не входят (R4.2).
 * Строка с репликой несёт связь с автором (R4.3); авторство концовки —
 * участник, выполнивший ход завершения.
 */
@Serializable
data class FinishedStory(
    val entries: List<FinishedStoryEntry>,
) {

    /** Полный текст рассказа: каждая реплика и концовка на своей строке (R4.1). */
    val text: String
        get() = entries.joinToString(separator = "\n") { it.text }
}

/**
 * Строка готового рассказа: текст и авторство (R4.3). [authorIndex] — позиция
 * автора в очереди участников; [authorName] — имя автора при его наличии
 * (отображение авторства без имён — область `ui`).
 */
@Serializable
data class FinishedStoryEntry(
    val text: String,
    val authorIndex: Int,
    val authorName: String?,
)

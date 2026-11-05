package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Готовый рассказ (game/finished-story, R4.1): все реплики, все видимые хвосты
 * и концовка в порядке написания, каждая часть — отдельная строка ([text]).
 * Хвост предшествует реплике, которую он начинает (R4.2); неподтверждённый
 * шаг хвоста (R3.8) в рассказ не входит. Каждая строка — реплика, хвост
 * или концовка — несёт связь с автором (R4.3); авторство концовки —
 * участник, выполнивший ход завершения.
 */
@Serializable
data class FinishedStory(
    val entries: List<FinishedStoryEntry>,
) {

    /** Полный текст рассказа: каждая строка на своей строке (R4.1). */
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

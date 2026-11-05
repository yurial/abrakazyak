package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Строка написанных текстов партии в порядке подтверждения (game/story-line,
 * R4.1): реплики ходов, видимые хвосты и концовка. Хвост, зафиксированный
 * сразу после своей реплики, в рассказе предшествует реплике, которую он
 * начинает (R4.2); неподтверждённый шаг хвоста строки не создаёт (R3.8).
 */
@Serializable
sealed interface StoryLine {

    /** Подтверждённая реплика хода (game/story-chunk, R2.1, R2.4). */
    @Serializable
    data class Chunk(val chunk: StoryChunk) : StoryLine

    /**
     * Подтверждённый видимый хвост хода (game/visible-tail, R2.1): начало
     * реплики следующего участника. [authorIndex] — позиция написавшего
     * в очереди участников (R4.3).
     */
    @Serializable
    data class Tail(val text: String, val authorIndex: Int) : StoryLine

    /** Концовка партии: финальная реплика (R3.2) или текст шаблона (R3.3). */
    @Serializable
    data class Ending(val text: String) : StoryLine
}

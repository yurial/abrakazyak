package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Строка написанных текстов партии в порядке подтверждения: реплики ходов
 * и концовка (R4.1). Видимые хвосты в список не входят (R4.2).
 */
@Serializable
sealed interface StoryLine {

    /** Подтверждённая реплика хода (game/story-chunk, R2.1, R2.4). */
    @Serializable
    data class Chunk(val chunk: StoryChunk) : StoryLine

    /** Концовка партии: финальная реплика (R3.2) или текст шаблона (R3.3). */
    @Serializable
    data class Ending(val text: String) : StoryLine
}

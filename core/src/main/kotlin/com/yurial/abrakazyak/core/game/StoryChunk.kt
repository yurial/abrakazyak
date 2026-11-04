package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Подтверждённая реплика (game/story-chunk, R2.1, R2.4): текст с индексом
 * автора в очереди участников ([Participant]); связь реплики с автором
 * доступна в готовом рассказе (R1.4, R4.3).
 */
@Serializable
data class StoryChunk(
    val text: String,
    val authorIndex: Int,
)

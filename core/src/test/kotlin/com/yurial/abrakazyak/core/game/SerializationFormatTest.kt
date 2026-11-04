package com.yurial.abrakazyak.core.game

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Строгость формата данных партии (storage R1.5): JSON без обязательных полей
 * не десериализуется — хранилище трактует такой черновик как отсутствующий.
 * Заодно фиксирует покрытие веток «поля отсутствуют» сгенерированных сериализаторов.
 */
class SerializationFormatTest {

    @Test
    fun decodingWithoutRequiredFieldsFailsCleanly() {
        assertFailsWith<SerializationException> { Json.decodeFromString<MatchSnapshot>("{}") }
        assertFailsWith<SerializationException> { Json.decodeFromString<Participant>("{}") }
        assertFailsWith<SerializationException> { Json.decodeFromString<StoryChunk>("""{"authorIndex":0}""") }
        assertFailsWith<SerializationException> { Json.decodeFromString<FinishedStory>("{}") }
        assertFailsWith<SerializationException> { Json.decodeFromString<FinishedStoryEntry>("""{"text":"реплика"}""") }
        assertFailsWith<SerializationException> {
            Json.decodeFromString<StoryLine>("""{"type":"com.yurial.abrakazyak.core.game.StoryLine.Chunk"}""")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString<StoryLine>("""{"type":"com.yurial.abrakazyak.core.game.StoryLine.Ending"}""")
        }
    }

    @Test
    fun decodingValidFormatSucceeds() {
        val snapshot: MatchSnapshot = Json.decodeFromString(
            """{"participants":[{"name":"Аня"},{"name":null}],"roundCount":1,"turnIndex":0,""" +
                """"phase":"AWAITING_CHUNK","lines":[{"type":"com.yurial.abrakazyak.core.game.StoryLine.Chunk",""" +
                """"chunk":{"text":"реплика","authorIndex":0}}],"visibleContext":null}""",
        )
        assertEquals(listOf(Participant("Аня"), Participant(null)), snapshot.participants)
        assertEquals(listOf(StoryLine.Chunk(StoryChunk("реплика", 0))), snapshot.lines)
    }
}

package com.yurial.abrakazyak.core.game

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Снимок состояния партии (storage R1.2): восстановление продолжения партии и формат данных.
 */
class MatchSnapshotTest {

    private val squad: List<Participant> =
        listOf(Participant("Аня"), Participant("Боря"), Participant(null))

    @Test
    fun snapshotCarriesConfirmedStateOnly() { // storage R1.2, R1.6
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "реплика-0", "хвост-0")
        val snapshot: MatchSnapshot = match.snapshot()
        assertEquals(squad, snapshot.participants)
        assertEquals(2, snapshot.roundCount)
        assertEquals(1, snapshot.turnIndex)
        assertEquals(TurnPhase.AWAITING_CHUNK, snapshot.phase)
        assertEquals(listOf(StoryLine.Chunk(StoryChunk("реплика-0", 0))), snapshot.lines)
        assertEquals("хвост-0", snapshot.visibleContext)
    }

    @Test
    fun restoredMatchContinuesFromConfirmedStep() { // storage R1.2
        val original: Match = startMatchOrFail(squad, 2).submitChunk("реплика-0").requireSuccess()
        val restored: Match = Match.fromSnapshot(original.snapshot())
        assertEquals(original.snapshot(), restored.snapshot())
        val continued: Match = restored.submitTail("хвост-0").requireSuccess()
        assertEquals("хвост-0", continued.visibleContext)
        assertEquals(1, continued.turnIndex)
    }

    @Test
    fun restoredAndOriginalAreIndependent() {
        val original: Match = startMatchOrFail(squad, 2)
        val restored: Match = Match.fromSnapshot(original.snapshot())
        val originalNext: Match = original.submitChunk("у оригинала").requireSuccess()
        val restoredNext: Match = restored.submitChunk("у восстановленной").requireSuccess()
        assertEquals(listOf(StoryLine.Chunk(StoryChunk("у оригинала", 0))), originalNext.lines)
        assertEquals(listOf(StoryLine.Chunk(StoryChunk("у восстановленной", 0))), restoredNext.lines)
    }

    @Test
    fun serializationRoundTripPreservesSnapshot() { // формат данных хранилища: активная и завершённая партии
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "реплика-0", "хвост-0")
        val active: MatchSnapshot = match.snapshot()
        val decodedActive: MatchSnapshot = Json.decodeFromString(Json.encodeToString(active))
        assertEquals(active, decodedActive)
        assertEquals(active, Match.fromSnapshot(decodedActive).snapshot())

        val finished: Match = playToNaturalFinish(squad, 1) // строки: реплики и концовка обоих видов
        val done: MatchSnapshot = finished.snapshot()
        val decodedDone: MatchSnapshot = Json.decodeFromString(Json.encodeToString(done))
        assertEquals(done, decodedDone)
        assertEquals(done, Match.fromSnapshot(decodedDone).snapshot())
    }

    @Test
    fun finishedSnapshotRestoresAsFinished() {
        val finished: Match = playToNaturalFinish(squad, 1)
        val restored: Match = Match.fromSnapshot(finished.snapshot())
        assertEquals(TurnPhase.FINISHED, restored.phase)
        assertEquals(finished.finishedStory(), restored.finishedStory())
        assertEquals(GameError.MatchFinished, restored.submitChunk("после конца").requireFailure())
    }
}

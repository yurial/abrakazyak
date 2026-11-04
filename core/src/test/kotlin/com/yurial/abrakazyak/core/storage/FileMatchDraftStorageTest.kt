package com.yurial.abrakazyak.core.storage

import com.yurial.abrakazyak.core.game.Match
import com.yurial.abrakazyak.core.game.MatchSnapshot
import com.yurial.abrakazyak.core.game.Participant
import com.yurial.abrakazyak.core.game.TurnPhase
import com.yurial.abrakazyak.core.game.requireSuccess
import com.yurial.abrakazyak.core.game.startMatch
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * T1.1–T1.4 — проверки черновика текущей партии (specs/storage.md, R1).
 */
class FileMatchDraftStorageTest {

    private val temporaryDirectory: Path = Files.createTempDirectory("abrakazyak-draft-test")

    @AfterTest
    fun removeTemporaryDirectory() {
        temporaryDirectory.toFile().deleteRecursively()
    }

    private fun newStorage(): MatchDraftStorage = FileMatchDraftStorage(temporaryDirectory)

    @Test
    fun draftRestoresStateAfterEachConfirmedStep() { // R1.1, R1.2, T1.1: реплики, хвосты и передача хода
        val squad: List<Participant> =
            listOf(Participant("Аня"), Participant(null), Participant("Вера"))
        var storage: MatchDraftStorage = newStorage()
        var match: Match = startMatch(squad, 2).requireSuccess()
        storage.saveDraft(match.snapshot())
        var reloaded: Match = requireDraftEquals(storage, match)

        match = match.submitChunk("реплика-0").requireSuccess()
        storage.saveDraft(match.snapshot())
        storage = newStorage()
        reloaded = requireDraftEquals(storage, match)

        match = reloaded.submitTail("хвост-0").requireSuccess()
        storage.saveDraft(match.snapshot())
        storage = newStorage()
        reloaded = requireDraftEquals(storage, match)
        assertEquals(TurnPhase.AWAITING_CHUNK, reloaded.phase)
        assertEquals("хвост-0", reloaded.visibleContext)

        match = reloaded.submitChunk("реплика-1").requireSuccess()
        storage.saveDraft(match.snapshot())
        requireDraftEquals(newStorage(), match)
    }

    @Test
    fun finishingRemovesDraftAndNewStartReplacesIt() { // R1.3, R1.4, T1.2
        val storage: MatchDraftStorage = newStorage()
        val first: Match = startMatch(listOf(Participant("Аня")), 2).requireSuccess()
        storage.saveDraft(first.snapshot())

        storage.clearDraft()
        assertEquals(null, storage.loadDraft(), "после завершения партии черновик отсутствует (R1.3)")

        val second: Match = startMatch(listOf(Participant("Боря"), Participant("Вера")), 1).requireSuccess()
        storage.saveDraft(second.snapshot())
        assertEquals(second.snapshot(), storage.loadDraft(), "старт новой партии заменяет черновик (R1.4)")
    }

    @Test
    fun clearDraftWithoutExistingFileIsHarmless() {
        val storage: MatchDraftStorage = newStorage()
        storage.clearDraft()
        assertEquals(null, storage.loadDraft())
    }

    @Test
    fun corruptedDraftTreatedAsAbsentAndNewPartyStarts() { // R1.5, T1.3
        val draftFile: Path = temporaryDirectory.resolve("draft.json")
        draftFile.writeText("{это не формат партии")
        val storage: MatchDraftStorage = newStorage()
        assertEquals(null, storage.loadDraft(), "нечитаемый черновик — «отсутствует» без ошибки наружу")
        val fresh: Match = startMatch(listOf(Participant("Аня")), 1).requireSuccess()
        storage.saveDraft(fresh.snapshot())
        assertEquals(fresh.snapshot(), storage.loadDraft(), "новая партия создаётся и работает")
    }

    @Test
    fun unreadableDraftPathTreatedAsAbsent() { // R1.5: сбой чтения трактуется как отсутствие
        val draftFile: Path = temporaryDirectory.resolve("draft.json")
        draftFile.createDirectory() // каталог вместо файла: чтение завершится IOException
        assertEquals(null, newStorage().loadDraft())
    }

    @Test
    fun unconfirmedStepTextNeverEntersDraft() { // R1.6, T1.4
        val storage: MatchDraftStorage = newStorage()
        val confirmed: Match = startMatch(listOf(Participant("Аня"), Participant("Боря")), 2)
            .requireSuccess()
            .submitChunk("подтверждённая реплика").requireSuccess()
        storage.saveDraft(confirmed.snapshot())

        val typedButNotConfirmed = "набрано, но кнопка подтверждения не нажата"
        val reloaded: Match = requireDraftEquals(newStorage(), confirmed)
        assertEquals(TurnPhase.AWAITING_TAIL, reloaded.phase, "партия продолжается с подтверждённого шага")
        assertEquals(
            listOf("подтверждённая реплика"),
            reloaded.chunks.map { it.text },
            "неподтверждённый текст не входит в черновик",
        )
        assertTrue(!reloaded.snapshot().toString().contains(typedButNotConfirmed))
    }

    @Test
    fun saveCreatesMissingDataDirectory() {
        val nested: Path = temporaryDirectory.resolve("nested").resolve("deeper")
        val storage: MatchDraftStorage = FileMatchDraftStorage(nested)
        val match: Match = startMatch(listOf(Participant("Аня")), 1).requireSuccess()
        storage.saveDraft(match.snapshot())
        assertEquals(match.snapshot(), storage.loadDraft())
    }

    /** Перезапуск хранилища и сверка восстановленного состояния с ожидаемым. */
    private fun requireDraftEquals(storage: MatchDraftStorage, expected: Match): Match {
        val loaded: MatchSnapshot = storage.loadDraft() ?: error("черновик должен существовать")
        assertEquals(expected.snapshot(), loaded, "восстановленное состояние равно состоянию после последнего шага")
        return Match.fromSnapshot(loaded)
    }
}

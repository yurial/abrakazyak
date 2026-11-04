package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.FinishedStory
import com.yurial.abrakazyak.core.game.GameResult
import com.yurial.abrakazyak.core.game.Match
import com.yurial.abrakazyak.core.game.MatchSnapshot
import com.yurial.abrakazyak.core.game.Participant
import com.yurial.abrakazyak.core.game.startMatch
import com.yurial.abrakazyak.core.storage.ArchiveEntry
import com.yurial.abrakazyak.core.storage.ArchiveEntryId
import com.yurial.abrakazyak.core.storage.MatchDraftStorage
import com.yurial.abrakazyak.core.storage.StorageError
import com.yurial.abrakazyak.core.storage.StorageResult
import com.yurial.abrakazyak.core.storage.StoryArchiveStorage
import java.io.IOException
import java.time.Instant

/** Фейк хранилища черновика: фиксирует операции для проверок автосохранения (storage R1). */
internal class FakeDraftStorage : MatchDraftStorage {

    /** Снимки, переданные [saveDraft], в порядке вызовов. */
    val saved: MutableList<MatchSnapshot> = mutableListOf()

    /** Число вызовов [clearDraft] (storage R1.3). */
    var clearedCount: Int = 0
        private set

    /** Результат [loadDraft]; [loadError] имитирует недоступность каталога данных. */
    var loadResult: MatchSnapshot? = null

    var loadError: IOException? = null

    override fun saveDraft(snapshot: MatchSnapshot) {
        saved.add(snapshot)
    }

    override fun loadDraft(): MatchSnapshot? {
        loadError?.let { throw it }
        return loadResult
    }

    override fun clearDraft() {
        clearedCount++
    }
}

/** Фейк архива: добавляет записи с порядковыми идентификаторами, имитирует entry-not-found. */
internal class FakeArchiveStorage : StoryArchiveStorage {

    /** Рассказы, переданные [addFinishedStory], в порядке вызовов (storage R2.1). */
    val added: MutableList<FinishedStory> = mutableListOf()

    /** Идентификаторы, для которых [readArchiveEntry] возвращает storage/entry-not-found. */
    val missingIds: MutableSet<String> = mutableSetOf()

    private val entries: MutableList<ArchiveEntry> = mutableListOf()

    override fun addFinishedStory(story: FinishedStory): ArchiveEntryId {
        added.add(story)
        val id: ArchiveEntryId = ArchiveEntryId("entry-${entries.size + 1}")
        entries.add(ArchiveEntry(id, story, Instant.EPOCH))
        return id
    }

    override fun listArchiveEntries(): List<ArchiveEntry> = entries.reversed()

    override fun readArchiveEntry(entryId: ArchiveEntryId): StorageResult<ArchiveEntry> {
        if (entryId.value in missingIds) return StorageResult.Failure(StorageError.EntryNotFound)
        val entry: ArchiveEntry? = entries.find { it.entryId == entryId }
        return entry?.let { StorageResult.Success(it) } ?: StorageResult.Failure(StorageError.EntryNotFound)
    }
}

/** Партия из [players] безымянных участников и [rounds] кругов (успешный старт). */
internal fun startedMatch(players: Int = 2, rounds: Int = 1): Match =
    success(startMatch(List(players) { Participant(null) }, rounds))

/** Успех результата модели или сбой теста. */
internal fun success(result: GameResult<Match>): Match =
    when (result) {
        is GameResult.Success -> result.value
        is GameResult.Failure -> error("unexpected failure: ${result.error.code}")
    }

/** Снимок хода в фазе AWAITING_TAIL: реплика подтверждена, хвост не введён. */
internal fun snapshotAwaitingTail(players: Int = 2, rounds: Int = 1): MatchSnapshot {
    var match: Match = startedMatch(players, rounds)
    match = success(match.submitChunk("реплика 0"))
    return match.snapshot()
}

/** Снимок обычного хода после первого: видимый контекст — хвост предыдущего участника. */
internal fun snapshotSecondTurn(players: Int = 2, rounds: Int = 2): MatchSnapshot {
    var match: Match = startedMatch(players, rounds)
    match = success(match.submitChunk("реплика 0"))
    match = success(match.submitTail("хвост 0"))
    return match.snapshot()
}

/** Снимок хода завершения: предыдущий ход передан, ожидается завершение партии. */
internal fun snapshotFinishingTurn(players: Int = 2, rounds: Int = 1): MatchSnapshot {
    var match: Match = startedMatch(players, rounds)
    match = success(match.submitChunk("реплика 0"))
    match = success(match.submitTail("хвост 0"))
    return match.snapshot()
}

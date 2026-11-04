package com.yurial.abrakazyak.core.storage

import com.yurial.abrakazyak.core.game.FinishedStory
import com.yurial.abrakazyak.core.game.FinishedStoryEntry
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * T2.1, T2.2 — проверки архива завершённых рассказов (specs/storage.md, R2).
 * Даты завершения фиксированы инжектируемыми часами — без флапов.
 */
class FileStoryArchiveStorageTest {

    private val temporaryDirectory: Path = Files.createTempDirectory("abrakazyak-archive-test")
    private val clock: SettableClock = SettableClock(Instant.parse("2026-01-01T10:00:00Z"))

    @AfterTest
    fun removeTemporaryDirectory() {
        temporaryDirectory.toFile().deleteRecursively()
    }

    private fun newStorage(): StoryArchiveStorage = FileStoryArchiveStorage(temporaryDirectory, clock)

    private fun namedStory(text: String): FinishedStory = FinishedStory(
        listOf(
            FinishedStoryEntry("Жил-был кот", 0, "Аня"),
            FinishedStoryEntry("он нашёл ларец", 1, null),
            FinishedStoryEntry(text, 0, "Аня"),
        ),
    )

    private fun unnamedStory(text: String): FinishedStory = FinishedStory(
        listOf(FinishedStoryEntry("начало без имён", 0, null), FinishedStoryEntry(text, 1, null)),
    )

    @Test
    fun entryCarriesFullTextAuthorshipAndFinishTime() { // R2.1, T2.1
        val storage: StoryArchiveStorage = newStorage()
        clock.set(Instant.parse("2026-03-14T15:09:26Z"))
        val entryId: ArchiveEntryId = storage.addFinishedStory(namedStory("Вот такая история получилась."))
        val entry: ArchiveEntry = readOrFail(storage, entryId)
        assertEquals(
            "Жил-был кот\nон нашёл ларец\nВот такая история получилась.",
            entry.story.text,
            "полный текст готового рассказа",
        )
        assertEquals("Аня", entry.story.entries[0].authorName, "авторство реплики при наличии имени")
        assertEquals(null, entry.story.entries[1].authorName, "без имени авторство не изобретается")
        assertEquals(Instant.parse("2026-03-14T15:09:26Z"), entry.finishedAt, "дата и время завершения")
    }

    @Test
    fun entriesListedFromNewToOld() { // R2.2, T2.1: упорядочивание по времени завершения
        val storage: StoryArchiveStorage = newStorage()
        clock.set(Instant.parse("2026-01-01T08:00:00Z"))
        val first: ArchiveEntryId = storage.addFinishedStory(namedStory("рассказ один"))
        clock.set(Instant.parse("2026-01-01T09:30:00Z"))
        val second: ArchiveEntryId = storage.addFinishedStory(unnamedStory("рассказ два"))
        clock.set(Instant.parse("2026-01-01T20:00:00Z"))
        val third: ArchiveEntryId = storage.addFinishedStory(namedStory("рассказ три"))

        val listed: List<ArchiveEntry> = storage.listArchiveEntries()
        assertEquals(listOf(third, second, first), listed.map { it.entryId }, "от новых к старым")
        assertEquals(Instant.parse("2026-01-01T20:00:00Z"), listed[0].finishedAt)
        assertEquals(Instant.parse("2026-01-01T08:00:00Z"), listed[2].finishedAt)
    }

    @Test
    fun equalFinishTimesOrderLaterAdditionFirst() { // R2.2: детерминированный порядок при равных моментах
        val storage: StoryArchiveStorage = newStorage()
        clock.set(Instant.parse("2026-02-02T02:02:02Z"))
        val first: ArchiveEntryId = storage.addFinishedStory(namedStory("первая"))
        val second: ArchiveEntryId = storage.addFinishedStory(namedStory("вторая"))
        assertEquals(listOf(second, first), storage.listArchiveEntries().map { it.entryId })
    }

    @Test
    fun entrySurvivesStorageRestart() { // R2.3, T2.2: перезапуск хранилища
        val entryId: ArchiveEntryId = newStorage().addFinishedStory(unnamedStory("переживает перезапуск"))
        val restarted: StoryArchiveStorage = newStorage()
        val entry: ArchiveEntry = readOrFail(restarted, entryId)
        assertEquals("начало без имён\nпереживает перезапуск", entry.story.text)
        assertEquals(clock.instant(), entry.finishedAt)
    }

    @Test
    fun unknownEntryIdReturnsEntryNotFound() { // storage/entry-not-found; состояние архива не изменяется
        val storage: StoryArchiveStorage = newStorage()
        storage.addFinishedStory(namedStory("существующая запись"))
        val result: StorageResult<ArchiveEntry> = storage.readArchiveEntry(ArchiveEntryId("no-such-entry"))
        assertEquals(StorageError.EntryNotFound, assertIs<StorageResult.Failure>(result).error)
        assertEquals("storage/entry-not-found", StorageError.EntryNotFound.code)
        assertEquals(1, storage.listArchiveEntries().size, "состояние архива при ошибке не изменяется")
    }

    @Test
    fun emptyArchiveListsNothingAndReadFails() {
        val storage: StoryArchiveStorage = newStorage()
        assertEquals(emptyList(), storage.listArchiveEntries())
        val result: StorageResult<ArchiveEntry> = storage.readArchiveEntry(ArchiveEntryId("entry-1"))
        assertEquals(StorageError.EntryNotFound, assertIs<StorageResult.Failure>(result).error)
    }

    @Test
    fun corruptedArchiveFileTreatedAsEmpty() { // см. TODO.md: поведение при повреждении архива
        val archiveFile: Path = temporaryDirectory.resolve("archive.json")
        archiveFile.writeText("[битые данные архива")
        val storage: StoryArchiveStorage = newStorage()
        assertEquals(emptyList(), storage.listArchiveEntries())
        val entryId: ArchiveEntryId = storage.addFinishedStory(namedStory("после повреждения"))
        assertEquals(listOf(entryId), newStorage().listArchiveEntries().map { it.entryId })
    }

    @Test
    fun archiveMissingRequiredFieldsTreatedAsEmpty() { // валидный JSON без обязательных полей записи
        val archiveFile: Path = temporaryDirectory.resolve("archive.json")
        archiveFile.writeText("""[{"entryId":"entry-1"}]""")
        val storage: StoryArchiveStorage = newStorage()
        assertEquals(emptyList(), storage.listArchiveEntries())
        assertEquals(null, storage.readArchiveEntry(ArchiveEntryId("entry-1")).valueOrNull())
    }

    @Test
    fun newEntryTakesSequenceAboveHandwrittenArchive() { // порядок добавления устойчив при любом составе файла
        val archiveFile: Path = temporaryDirectory.resolve("archive.json")
        archiveFile.writeText(
            "[" +
                """{"entryId":"entry-7","sequence":7,"finishedAtEpochMillis":100,"story":{"entries":[]}},""" +
                """{"entryId":"entry-3","sequence":3,"finishedAtEpochMillis":200,"story":{"entries":[]}}]""",
        )
        clock.set(Instant.ofEpochMilli(300))
        val storage: StoryArchiveStorage = newStorage()
        val entryId: ArchiveEntryId = storage.addFinishedStory(namedStory("поверх рукописного"))
        assertEquals("entry-8", entryId.value)
        assertEquals(listOf(entryId, ArchiveEntryId("entry-3"), ArchiveEntryId("entry-7")), storage.listArchiveEntries().map { it.entryId })
    }

    @Test
    fun unreadableArchivePathTreatedAsEmpty() { // сбой чтения трактуется как пустой архив
        temporaryDirectory.resolve("archive.json").createDirectory()
        assertEquals(emptyList(), newStorage().listArchiveEntries())
    }

    private fun readOrFail(storage: StoryArchiveStorage, entryId: ArchiveEntryId): ArchiveEntry {
        val result: StorageResult<ArchiveEntry> = storage.readArchiveEntry(entryId)
        return assertIs<StorageResult.Success<ArchiveEntry>>(result).value
    }
}

/** Результат чтения без ветвления в тестах: null при ошибке. */
private fun StorageResult<ArchiveEntry>.valueOrNull(): ArchiveEntry? = when (this) {
    is StorageResult.Success -> value
    is StorageResult.Failure -> null
}

/** Часы с управляемым моментом времени: детерминированные даты архива в тестах. */
private class SettableClock(private var moment: Instant) : Clock() {

    fun set(instant: Instant) {
        moment = instant
    }

    override fun instant(): Instant = moment

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}

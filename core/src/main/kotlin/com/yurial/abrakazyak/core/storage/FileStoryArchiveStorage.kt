package com.yurial.abrakazyak.core.storage

import com.yurial.abrakazyak.core.game.FinishedStory
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.Instant

/**
 * Файловая реализация [StoryArchiveStorage]: один файл `archive.json`
 * в каталоге данных, инжектируемом конструктором. Дата и время завершения
 * берутся из инжектируемых [clock] (детерминизм в тестах, отсутствие флапов).
 *
 * Повреждённый файл архива трактуется как пустой архив (решение исполнителя,
 * спецификация поведение при повреждении архива не определяет; см. TODO.md):
 * следующая запись пересоздаёт файл.
 */
class FileStoryArchiveStorage(
    private val dataDirectory: Path,
    private val clock: Clock,
) : StoryArchiveStorage {

    override fun addFinishedStory(story: FinishedStory): ArchiveEntryId {
        val entries: MutableList<ArchiveEntryDto> = readEntries().toMutableList()
        val sequence: Long = (entries.maxOfOrNull { it.sequence } ?: 0L) + 1L
        val entry = ArchiveEntryDto(
            entryId = "entry-$sequence",
            sequence = sequence,
            finishedAtEpochMillis = clock.instant().toEpochMilli(),
            story = story,
        )
        entries += entry
        writeEntries(entries)
        return ArchiveEntryId(entry.entryId)
    }

    override fun listArchiveEntries(): List<ArchiveEntry> =
        readEntries()
            .sortedWith(
                compareByDescending<ArchiveEntryDto> { it.finishedAtEpochMillis }
                    .thenByDescending { it.sequence },
            )
            .map { dto: ArchiveEntryDto -> dto.toArchiveEntry() }

    override fun readArchiveEntry(entryId: ArchiveEntryId): StorageResult<ArchiveEntry> {
        val dto: ArchiveEntryDto = readEntries().find { it.entryId == entryId.value }
            ?: return StorageResult.Failure(StorageError.EntryNotFound)
        return StorageResult.Success(dto.toArchiveEntry())
    }

    /**
     * Читает все записи; отсутствующий или повреждённый файл, как и сбой чтения,
     * даёт пустой архив (решение зафиксировано в KDoc класса и TODO.md).
     */
    private fun readEntries(): List<ArchiveEntryDto> = try {
        if (Files.exists(archiveFile())) {
            Json.decodeFromString<List<ArchiveEntryDto>>(Files.readString(archiveFile()))
        } else {
            emptyList()
        }
    } catch (error: IOException) {
        emptyList()
    } catch (error: SerializationException) {
        emptyList()
    }

    private fun writeEntries(entries: List<ArchiveEntryDto>) {
        Files.createDirectories(dataDirectory)
        val tempFile: Path = dataDirectory.resolve("$ARCHIVE_FILE_NAME.tmp")
        Files.writeString(tempFile, Json.encodeToString(entries))
        Files.move(tempFile, archiveFile(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun ArchiveEntryDto.toArchiveEntry(): ArchiveEntry = ArchiveEntry(
        entryId = ArchiveEntryId(entryId),
        story = story,
        finishedAt = Instant.ofEpochMilli(finishedAtEpochMillis),
    )

    private fun archiveFile(): Path = dataDirectory.resolve(ARCHIVE_FILE_NAME)

    private companion object {
        const val ARCHIVE_FILE_NAME = "archive.json"
    }
}

/**
 * DTO файла архива: идентификатор записи, номер добавления (порядок при равных
 * моментах завершения), момент завершения в миллисекундах эпохи и рассказ.
 */
@Serializable
internal data class ArchiveEntryDto(
    val entryId: String,
    val sequence: Long,
    val finishedAtEpochMillis: Long,
    val story: FinishedStory,
)

package com.yurial.abrakazyak.core.storage

import com.yurial.abrakazyak.core.game.FinishedStory

/**
 * Архив завершённых рассказов (specs/storage.md, Interface, R2).
 * Реализация подменяется в `:app`; записи персистентны и переживают
 * перезапуск приложения (storage R2.3).
 */
interface StoryArchiveStorage {

    /**
     * Добавляет завершённый рассказ в архив (storage R2.1) и возвращает
     * идентификатор записи. Дата и время завершения проставляются хранилищем.
     *
     * @throws java.io.IOException при недоступности каталога данных.
     */
    fun addFinishedStory(story: FinishedStory): ArchiveEntryId

    /**
     * Список записей архива, упорядоченный от новых к старым (storage R2.2):
     * по дате и времени завершения по убыванию; записи с равным моментом
     * завершения упорядочиваются по времени добавления — более поздняя выше.
     * Сбой чтения или повреждённый файл дают пустой архив: ошибок данных
     * наружу операция не отдаёт.
     */
    fun listArchiveEntries(): List<ArchiveEntry>

    /**
     * Возвращает запись архива по идентификатору (storage Interface);
     * неизвестный идентификатор — [StorageError.EntryNotFound], состояние
     * архива при этом не изменяется (storage Error handling). Сбой чтения
     * или повреждённый файл дают пустой архив и, как следствие, тот же
     * [StorageError.EntryNotFound]: ошибок данных наружу операция не отдаёт.
     */
    fun readArchiveEntry(entryId: ArchiveEntryId): StorageResult<ArchiveEntry>
}

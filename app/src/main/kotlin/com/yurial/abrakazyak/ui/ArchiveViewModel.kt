package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.storage.ArchiveEntry
import com.yurial.abrakazyak.core.storage.ArchiveEntryId
import com.yurial.abrakazyak.core.storage.StoryArchiveStorage
import com.yurial.abrakazyak.core.storage.StorageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel экрана архива `screen-archive` (specs/ui.md R8): список записей
 * от новых к старым (R8.1, storage R2.2) и чтение выбранной записи (R8.2).
 *
 * Чистый JVM-класс; хранилище внедряется через конструктор (тестируемость).
 */
class ArchiveViewModel(private val archiveStorage: StoryArchiveStorage) {

    /**
     * Состояние экрана: [entries] — записи архива в порядке хранилища —
     * от новых к старым (R8.1).
     */
    data class UiState(
        val entries: List<ArchiveEntry> = emptyList(),
    )

    private val mutableState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val state: StateFlow<UiState> = mutableState

    /**
     * Перечитывает список записей (R8.1); вызывается при каждом открытии экрана
     * архива, чтобы отражать рассказы, добавленные после предыдущего открытия.
     */
    fun refresh() {
        mutableState.value = UiState(entries = archiveStorage.listArchiveEntries())
    }

    /**
     * Читает запись по идентификатору (R8.2); возвращает её для экрана чтения,
     * null при неизвестном идентификаторе (storage/entry-not-found) — состояние
     * не меняется.
     */
    fun select(entryId: ArchiveEntryId): ArchiveEntry? =
        when (val result = archiveStorage.readArchiveEntry(entryId)) {
            is StorageResult.Success -> result.value
            is StorageResult.Failure -> null
        }
}

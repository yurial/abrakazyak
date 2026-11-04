package com.yurial.abrakazyak.core.storage

import com.yurial.abrakazyak.core.game.FinishedStory
import java.time.Instant

/**
 * Запись архива завершённых рассказов (storage/archive-entry, R2.1):
 * полный текст готового рассказа с авторством реплик при наличии имён
 * ([story]) и дата и время завершения партии ([finishedAt]).
 */
data class ArchiveEntry(
    val entryId: ArchiveEntryId,
    val story: FinishedStory,
    val finishedAt: Instant,
)

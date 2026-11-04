package com.yurial.abrakazyak.core.storage

/**
 * Ошибки хранилища; коды совпадают с полным списком из specs/storage.md
 * (Interface, Error handling). Повреждённый черновик кодом ошибки наружу
 * не является: [com.yurial.abrakazyak.core.storage.MatchDraftStorage.loadDraft]
 * сообщает «отсутствует» (storage R1.5).
 */
sealed interface StorageError {

    /** Машинночитаемый код из specs/storage.md. */
    val code: String

    /** `readArchiveEntry` с неизвестным идентификатором (storage/entry-not-found). */
    data object EntryNotFound : StorageError {
        override val code: String = "storage/entry-not-found"
    }
}

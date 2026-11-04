package com.yurial.abrakazyak.core.storage

/**
 * Результат операции хранилища, способной завершиться логической ошибкой
 * (specs/storage.md, Error handling). Сбои файловой системы вне этого типа:
 * они пробрасываются как IOException согласно контрактам операций.
 */
sealed interface StorageResult<out T> {

    /** Операция выполнена: [value] — результат. */
    data class Success<out T>(val value: T) : StorageResult<T>

    /** Операция отклонена: [error] — причина; состояние хранилища не изменилось. */
    data class Failure(val error: StorageError) : StorageResult<Nothing>
}

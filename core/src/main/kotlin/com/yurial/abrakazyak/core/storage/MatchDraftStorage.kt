package com.yurial.abrakazyak.core.storage

import com.yurial.abrakazyak.core.game.MatchSnapshot

/**
 * Хранилище черновика текущей партии (specs/storage.md, Interface, R1).
 * Реализация подменяется в `:app`; операции не возвращают логических ошибок.
 */
interface MatchDraftStorage {

    /**
     * Сохраняет полное состояние незавершённой партии (storage R1.1): вызывается
     * автоматически после каждого подтверждённого шага партии. Старт новой партии
     * заменяет прежний черновик (storage R1.4). Снимок хранится как есть; для
     * исключения неподтверждённых текстов используйте только [com.yurial.abrakazyak.core.game.Match.snapshot]
     * после подтверждённых операций (storage R1.6).
     *
     * @throws java.io.IOException при недоступности каталога данных.
     */
    fun saveDraft(snapshot: MatchSnapshot)

    /**
     * Возвращает сохранённое состояние партии либо null — «отсутствует»
     * (storage Interface): отсутствует и файл черновика, и нечитаемый
     * (повреждённый) черновик — он трактуется как отсутствующий и не
     * препятствует старту новой партии (storage R1.5).
     *
     * @throws java.io.IOException при недоступности каталога данных.
     */
    fun loadDraft(): MatchSnapshot?

    /**
     * Удаляет черновик; вызывается при завершении партии (storage R1.3).
     * Отсутствующий черновик удалять не нужно — операция безвредна.
     *
     * @throws java.io.IOException при недоступности каталога данных.
     */
    fun clearDraft()
}

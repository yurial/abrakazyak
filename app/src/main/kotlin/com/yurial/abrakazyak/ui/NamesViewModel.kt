package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.GameRules
import com.yurial.abrakazyak.core.game.Participant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel экрана участников `screen-names` (specs/ui.md, R2):
 * строки участников с добавлением и удалением (R2.2), опциональные имена,
 * число кругов с предзаполнением `defaultRoundCount` и защитой ручной правки
 * (R2.4, R2.6, R2.7), параметры старта партии (R2.3, R2.5).
 *
 * Чистый JVM-класс без Android-зависимостей; состояние — [StateFlow],
 * все операции синхронные и вызываются с главного потока.
 */
class NamesViewModel(initialParticipantRows: Int = DEFAULT_PARTICIPANT_ROWS) {

    /**
     * Состояние экрана: [names] — текущий текст строк участников (пустая строка —
     * имя не задано); [roundsText] — текст поля числа кругов; [roundsEditedManually] —
     * признак ручной правки поля кругов (R2.7).
     */
    data class UiState(
        val names: List<String>,
        val roundsText: String,
        val roundsEditedManually: Boolean,
    ) {

        /**
         * Старт доступен, когда текст поля кругов — целое число не менее единицы
         * (R2.4); число строк участников всегда не менее единицы (R2.2).
         */
        val canStart: Boolean
            get() = (roundsText.toIntOrNull() ?: 0) >= 1

        /** Предзаполнение поля кругов для текущего числа строк (R2.6). */
        val defaultRounds: Int
            get() = GameRules.defaultRoundCount(names.size)
    }

    /** Параметры старта партии, собранные из состояния экрана (R2.3, R2.5). */
    data class StartParams(
        val participants: List<Participant>,
        val roundCount: Int,
    )

    private val rows: MutableList<String> = MutableList(initialParticipantRows) { "" }

    private var roundsText: String = GameRules.defaultRoundCount(initialParticipantRows).toString()

    private var roundsEditedManually: Boolean = false

    private val mutableState: MutableStateFlow<UiState> = MutableStateFlow(currentState())

    val state: StateFlow<UiState> = mutableState

    /** Заменяет имя строки [rowIndex]; имена опциональны (R2.2, game R1.3). */
    fun onNameChange(rowIndex: Int, name: String) {
        rows[rowIndex] = name
        publish()
    }

    /** Добавляет строку участника; пересчитывает поле кругов до ручной правки (R2.2, R2.6). */
    fun addRow() {
        rows.add("")
        recomputeRoundsIfNotEdited()
        publish()
    }

    /**
     * Удаляет строку [rowIndex]; последняя строка не удаляется — минимум одна
     * строка присутствует всегда (R2.2). До ручной правки пересчитывает круги (R2.6).
     */
    fun deleteRow(rowIndex: Int) {
        if (rows.size <= 1) return
        rows.removeAt(rowIndex)
        recomputeRoundsIfNotEdited()
        publish()
    }

    /**
     * Принимает текст поля кругов: нецифровые символы не принимаются (R2.4);
     * любая правка помечает поле как отредактированное вручную (R2.7).
     */
    fun onRoundsChange(roundsText: String) {
        roundsEditedManually = true
        this.roundsText = roundsText.filter { it.isDigit() }
        publish()
    }

    /**
     * Параметры подтверждения старта (R2.3, R2.5): участники из строк на момент
     * подтверждения (пустые имена — [Participant.name] равно null) и число кругов;
     * null, если число кругов вне допустимых значений (R2.4).
     */
    fun startParams(): StartParams? {
        val roundCount: Int = roundsText.toIntOrNull()?.takeIf { it >= 1 } ?: return null
        return StartParams(
            participants = rows.map { row: String -> Participant(row.ifBlank { null }) },
            roundCount = roundCount,
        )
    }

    /** Поле кругов пересчитывается по числу строк, пока не отредактировано вручную (R2.6). */
    private fun recomputeRoundsIfNotEdited() {
        if (!roundsEditedManually) roundsText = GameRules.defaultRoundCount(rows.size).toString()
    }

    /** Публикует срез текущего состояния (списки копируются — иммутабельность среза). */
    private fun currentState(): UiState = UiState(
        names = rows.toList(),
        roundsText = roundsText,
        roundsEditedManually = roundsEditedManually,
    )

    private fun publish() {
        mutableState.value = currentState()
    }

    companion object {

        /**
         * Число строк участников при открытии экрана; спецификация не задаёт
         * значение по умолчанию (TODO «Открытые вопросы», п. 6), выбрано 2 —
         * аудитория игры: компании из двух и более человек.
         */
        const val DEFAULT_PARTICIPANT_ROWS: Int = 2
    }
}

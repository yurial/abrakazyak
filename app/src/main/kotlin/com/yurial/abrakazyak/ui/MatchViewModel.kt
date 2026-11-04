package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.FinishedStory
import com.yurial.abrakazyak.core.game.GameError
import com.yurial.abrakazyak.core.game.GameResult
import com.yurial.abrakazyak.core.game.GameRules
import com.yurial.abrakazyak.core.game.Match
import com.yurial.abrakazyak.core.game.MatchSnapshot
import com.yurial.abrakazyak.core.game.Participant
import com.yurial.abrakazyak.core.game.TemplateEnding
import com.yurial.abrakazyak.core.game.TurnPhase
import com.yurial.abrakazyak.core.game.startMatch
import com.yurial.abrakazyak.core.storage.ArchiveEntryId
import com.yurial.abrakazyak.core.storage.MatchDraftStorage
import com.yurial.abrakazyak.core.storage.StoryArchiveStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel игровой сессии (specs/ui.md R4–R6, storage R1): владеет текущим
 * [Match], черновиками неподтверждённого ввода (R6.9) и оркестрацией
 * автосохранения (storage R1.1, R1.3, R1.4, R2.1).
 *
 * Все операции синхронные и вызываются с главного потока: [Match] не
 * потокобезопасен (game Usage constraints). Хранилища внедряются через
 * конструктор — тестируемость (файловые реализации из `:core` подменяются).
 */
class MatchViewModel(
    private val draftStorage: MatchDraftStorage,
    private val archiveStorage: StoryArchiveStorage,
    languageKey: String,
) {

    /** Экран игровой сессии внутри области MATCH; выбирается по фазе хода (R4, R5, R6.10). */
    enum class Screen { CHUNK, TAIL, FINISH }

    /** Итог игровой операции: интерпретируется [AppViewModel] для навигации (R7.1). */
    sealed interface Outcome {

        /** Шаг подтверждён: партия жива, автосохранение выполнено (storage R1.1). */
        data object TurnAdvanced : Outcome

        /** Партия завершена: черновик удалён, рассказ добавлен в архив (storage R1.3, R2.1). */
        data class Finished(val story: FinishedStory) : Outcome

        /** Операция отклонена моделью; состояние сессии не изменилось (game Error handling). */
        data class Rejected(val error: GameError) : Outcome

        /** Операция вне сессии: партия не начата; состояние не изменилось. */
        data object NoSession : Outcome
    }

    /**
     * Состояние игровой сессии: [match] — текущая партия (null до старта);
     * [showFinishScreen] — экран завершения открыт с обычного хода (R4.7, R5.6)
     * либо ход завершения (R6.10); [chunkDraft], [tailDraft], [finishDraft] —
     * набранный неподтверждённый текст соответствующего поля (R6.9); [templates] —
     * шаблонные концовки текущего языка интерфейса (R6.3); [finishedStory] —
     * готовый рассказ завершённой партии (R7.1).
     */
    data class UiState(
        val match: Match? = null,
        val showFinishScreen: Boolean = false,
        val chunkDraft: String = "",
        val tailDraft: String = "",
        val finishDraft: String = "",
        val templates: List<TemplateEnding> = emptyList(),
        val finishedStory: FinishedStory? = null,
    ) {

        /**
         * Активный экран сессии: экран завершения, если он открыт либо ход — ход
         * завершения (R6.10); иначе экран по фазе хода (R4.1, R5.5); null, если
         * партия не начата или завершена (чтение рассказа маршрутизирует [AppViewModel]).
         */
        val activeScreen: Screen?
            get() {
                val current: Match = match ?: return null
                if (showFinishScreen) return Screen.FINISH
                return when (current.phase) {
                    TurnPhase.AWAITING_CHUNK -> Screen.CHUNK
                    TurnPhase.AWAITING_TAIL -> Screen.TAIL
                    TurnPhase.AWAITING_FINISH -> Screen.FINISH
                    TurnPhase.FINISHED -> null
                }
            }
    }

    private var languageKey: String = languageKey

    private val mutableState: MutableStateFlow<UiState> = MutableStateFlow(initialState())

    val state: StateFlow<UiState> = mutableState

    /** Заменяет ключ языка интерфейса и пересчитывает список шаблонных концовок (R6.3, R9.4). */
    fun updateLanguage(languageKey: String) {
        this.languageKey = languageKey
        mutableState.value = mutableState.value.copy(templates = GameRules.templateEndings(languageKey))
    }

    /**
     * Стартует новую партию (R2.3, R2.5) и заменяет черновик её снимком
     * (storage R1.4). Отклонение модели — [Outcome.Rejected], состояние не меняется.
     */
    fun startNewMatch(participants: List<Participant>, roundCount: Int?): Outcome =
        when (val result = startMatch(participants, roundCount)) {
            is GameResult.Success -> {
                val match: Match = result.value
                draftStorage.saveDraft(match.snapshot())
                mutableState.value = UiState(match = match, templates = GameRules.templateEndings(languageKey))
                Outcome.TurnAdvanced
            }
            is GameResult.Failure -> Outcome.Rejected(result.error)
        }

    /**
     * Восстанавливает партию из снимка черновика (R3.1, storage R1.2): экран сессии
     * выбирается по восстановленной фазе, ход завершения сразу открывает экран
     * завершения (R6.10); неподтверждённый ввод не входит в снимок (storage R1.6),
     * черновики пусты.
     */
    fun restore(snapshot: MatchSnapshot) {
        mutableState.value = UiState(
            match = Match.fromSnapshot(snapshot),
            showFinishScreen = snapshot.phase == TurnPhase.AWAITING_FINISH,
            templates = GameRules.templateEndings(languageKey),
        )
    }

    /**
     * Принимает текст реплики; ввод сверх [GameRules.MAX_STORY_CHUNK_LENGTH]
     * не принимается (R4.3) — состояние не меняется.
     */
    fun onChunkDraftChange(text: String) {
        if (text.length > GameRules.MAX_STORY_CHUNK_LENGTH) return
        mutableState.value = mutableState.value.copy(chunkDraft = text)
    }

    /** Принимает текст хвоста; ввод сверх [GameRules.MAX_VISIBLE_TAIL_LENGTH] не принимается (R5.2). */
    fun onTailDraftChange(text: String) {
        if (text.length > GameRules.MAX_VISIBLE_TAIL_LENGTH) return
        mutableState.value = mutableState.value.copy(tailDraft = text)
    }

    /** Принимает финальную реплику; ввод сверх [GameRules.MAX_STORY_CHUNK_LENGTH] не принимается (R6.2). */
    fun onFinishDraftChange(text: String) {
        if (text.length > GameRules.MAX_STORY_CHUNK_LENGTH) return
        mutableState.value = mutableState.value.copy(finishDraft = text)
    }

    /**
     * Подтверждает реплику текущего хода (R4.6); при успехе сохраняет снимок
     * (storage R1.1), очищает черновик реплики и открывает экран хвоста (R5.5).
     */
    fun submitChunk(): Outcome {
        val current: UiState = mutableState.value
        val match: Match = current.match ?: return Outcome.NoSession
        return when (val result = match.submitChunk(current.chunkDraft)) {
            is GameResult.Success -> {
                val next: Match = result.value
                draftStorage.saveDraft(next.snapshot())
                mutableState.value = current.copy(match = next, chunkDraft = "")
                Outcome.TurnAdvanced
            }
            is GameResult.Failure -> Outcome.Rejected(result.error)
        }
    }

    /**
     * Подтверждает хвост и передаёт ход (R5.5); при успехе сохраняет снимок
     * (storage R1.1); ход завершения открывает экран завершения (R6.10).
     */
    fun submitTail(): Outcome {
        val current: UiState = mutableState.value
        val match: Match = current.match ?: return Outcome.NoSession
        return when (val result = match.submitTail(current.tailDraft)) {
            is GameResult.Success -> {
                val next: Match = result.value
                draftStorage.saveDraft(next.snapshot())
                mutableState.value = current.copy(
                    match = next,
                    tailDraft = "",
                    finishDraft = "",
                    showFinishScreen = next.phase == TurnPhase.AWAITING_FINISH,
                )
                Outcome.TurnAdvanced
            }
            is GameResult.Failure -> Outcome.Rejected(result.error)
        }
    }

    /** Открывает экран завершения с текущего хода (R4.7, R5.6); ввод сохраняется (R6.9). */
    fun openFinish() {
        mutableState.value = mutableState.value.copy(showFinishScreen = true)
    }

    /**
     * Возвращает с экрана завершения к прерванному обычному ходу (R6.5);
     * на ходе завершения возврата нет — состояние не меняется.
     */
    fun abandonFinish() {
        val current: UiState = mutableState.value
        if (current.match?.phase != TurnPhase.AWAITING_FINISH) {
            mutableState.value = current.copy(showFinishScreen = false)
        }
    }

    /**
     * Завершает партию финальной репликой (R6.1, R6.2): удаляет черновик и
     * добавляет рассказ в архив (storage R1.3, R2.1).
     */
    fun finishWithReply(): Outcome = finishWith { match: Match -> match.finishWithReply(mutableState.value.finishDraft) }

    /** Завершает партию шаблонной концовкой без ввода реплики (R6.4). */
    fun finishWithTemplate(templateId: String): Outcome = finishWith { match: Match -> match.finishWithTemplate(templateId) }

    /**
     * Сбрасывает сессию после чтения рассказа (R7.4): следующая партия стартует
     * с чистого состояния.
     */
    fun reset() {
        mutableState.value = initialState()
    }

    /**
     * Общая ветка завершения: делегирует операцию модели, затем удаляет черновик
     * и пополняет архив (storage R1.3, R2.1).
     */
    private fun finishWith(operation: (Match) -> GameResult<Match>): Outcome {
        val current: UiState = mutableState.value
        val match: Match = current.match ?: return Outcome.NoSession
        return when (val result = operation(match)) {
            is GameResult.Success -> {
                val finished: Match = result.value
                // Успех операции завершения гарантирует фазу FINISHED и непустой
                // рассказ (контракт :core, Match.finishedStory).
                val story: FinishedStory = checkNotNull(finished.finishedStory()) {
                    "finish operation succeeded without a finished story"
                }
                draftStorage.clearDraft()
                archiveStorage.addFinishedStory(story)
                mutableState.value = current.copy(
                    match = finished,
                    showFinishScreen = false,
                    finishDraft = "",
                    finishedStory = story,
                )
                Outcome.Finished(story)
            }
            is GameResult.Failure -> Outcome.Rejected(result.error)
        }
    }

    /** Чистое состояние сессии: черновики пусты, концовки текущего языка. */
    private fun initialState(): UiState = UiState(templates = GameRules.templateEndings(languageKey))
}

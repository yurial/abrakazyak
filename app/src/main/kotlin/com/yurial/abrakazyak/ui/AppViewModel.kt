package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.FinishedStory
import com.yurial.abrakazyak.core.game.MatchSnapshot
import com.yurial.abrakazyak.core.storage.ArchiveEntryId
import com.yurial.abrakazyak.core.storage.MatchDraftStorage
import com.yurial.abrakazyak.core.storage.StoryArchiveStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

/**
 * Корневой ViewModel приложения (specs/ui.md R1, R3, R7, R8, R10): маршрутизация
 * экранов, холодный старт с черновиком и `dialog-resume` (R3.1–R3.3), правила
 * в любой момент (R10), чтение рассказа и архива (R7, R8).
 *
 * Владеет экранными ViewModel ([namesViewModel], [matchViewModel],
 * [archiveViewModel]); все операции синхронные и вызываются с главного потока.
 */
class AppViewModel(
    private val draftStorage: MatchDraftStorage,
    private val archiveStorage: StoryArchiveStorage,
    languageKey: String,
) {

    /** Экран верхнего уровня; внутри MATCH экран выбирает [MatchViewModel] (R4–R6). */
    enum class Screen { RULES, NAMES, MATCH, STORY, ARCHIVE }

    /** Режим экрана чтения: только что завершённая партия (R7.1) либо архивная запись (R8.2). */
    enum class StoryMode { FINISHED_MATCH, ARCHIVE_ENTRY }

    /** Содержимое экрана чтения: рассказ и его режим. */
    data class StoryView(
        val mode: StoryMode,
        val story: FinishedStory,
    )

    /**
     * Состояние маршрутизации: [screen] — активный экран; [showResumeDialog] —
     * модальный диалог возобновления поверх `screen-rules` (R3.1, R3.3);
     * [showRulesDialog] — диалог правил поверх текущего экрана (R10.1, R10.2);
     * [story] — содержимое экрана чтения (R7.1, R8.2).
     */
    data class UiState(
        val screen: Screen = Screen.RULES,
        val showResumeDialog: Boolean = false,
        val showRulesDialog: Boolean = false,
        val story: StoryView? = null,
    )

    val namesViewModel: NamesViewModel = NamesViewModel()
    val matchViewModel: MatchViewModel = MatchViewModel(draftStorage, archiveStorage, languageKey)
    val archiveViewModel: ArchiveViewModel = ArchiveViewModel(archiveStorage)

    private val mutableState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val state: StateFlow<UiState> = mutableState

    /** Снимок черновика, предложенный в `dialog-resume`; null после выбора. */
    private var pendingDraftSnapshot: MatchSnapshot? = null

    /** Экран, из которого открыт архив (R8.3); архив без источника возвращает на `screen-rules`. */
    private var archiveReturnScreen: Screen = Screen.RULES

    init {
        pendingDraftSnapshot = try {
            draftStorage.loadDraft()
        } catch (error: IOException) {
            // Недоступный каталог данных трактуется как отсутствие черновика
            // (storage R1.5 — нечитаемый черновик не препятствует новой партии).
            null
        }
        if (pendingDraftSnapshot != null) {
            mutableState.value = mutableState.value.copy(showResumeDialog = true)
        }
    }

    /** Заменяет ключ языка интерфейса (R9.4) — пересчёт шаблонных концовок. */
    fun updateLanguage(languageKey: String) {
        matchViewModel.updateLanguage(languageKey)
    }

    /** Переход `screen-rules` → `screen-names` по `rules.play` (R2.1). */
    fun playClicked() {
        mutableState.value = mutableState.value.copy(screen = Screen.NAMES)
    }

    /** Возврат с `screen-names` на `screen-rules` (системная навигация назад). */
    fun namesBackClicked() {
        mutableState.value = mutableState.value.copy(screen = Screen.RULES)
    }

    /**
     * Подтверждение старта с `screen-names` (R2.3, R2.5): создаёт партию,
     * заменяет черновик (storage R1.4) и открывает первый ход (R2.3); при
     * недопустимых параметрах остаётся на `screen-names`.
     */
    fun confirmNames() {
        val params: NamesViewModel.StartParams = namesViewModel.startParams() ?: return
        val outcome: MatchViewModel.Outcome =
            matchViewModel.startNewMatch(params.participants, params.roundCount)
        if (outcome is MatchViewModel.Outcome.TurnAdvanced) {
            mutableState.value = mutableState.value.copy(screen = Screen.MATCH, story = null)
        }
    }

    /** Согласие в `dialog-resume` (R3.1): восстановление партии и экрана текущего хода. */
    fun resumeConfirmed() {
        val snapshot: MatchSnapshot = pendingDraftSnapshot ?: return
        pendingDraftSnapshot = null
        matchViewModel.restore(snapshot)
        mutableState.value = mutableState.value.copy(screen = Screen.MATCH, showResumeDialog = false)
    }

    /** Отказ в `dialog-resume` (R3.2): черновик удаляется, открывается `screen-names`. */
    fun resumeDeclined() {
        pendingDraftSnapshot = null
        draftStorage.clearDraft()
        mutableState.value = mutableState.value.copy(screen = Screen.NAMES, showResumeDialog = false)
    }

    /** Подтверждение реплики на `screen-chunk` (R4.6); экран сессии выбирается по фазе. */
    fun chunkConfirmed() {
        matchViewModel.submitChunk()
    }

    /** Подтверждение хвоста на `screen-tail` (R5.5). */
    fun tailConfirmed() {
        matchViewModel.submitTail()
    }

    /** Кнопка `chunk.finish`/`tail.finish` — экран завершения (R4.7, R5.6). */
    fun finishOpened() {
        matchViewModel.openFinish()
    }

    /** Возврат с `screen-finish` к прерванному ходу (R6.5). */
    fun finishAbandoned() {
        matchViewModel.abandonFinish()
    }

    /** Завершение финальной репликой (R6.1): после успеха — `screen-story` (R7.1). */
    fun finishConfirmed() {
        finishBy { matchViewModel.finishWithReply() }
    }

    /** Завершение выбором шаблонной концовки (R6.4): после успеха — `screen-story` (R7.1). */
    fun templateConfirmed(templateId: String) {
        finishBy { matchViewModel.finishWithTemplate(templateId) }
    }

    /** `story.play-again` (R7.4): новая партия через `screen-names` с чистой сессией. */
    fun storyPlayAgain() {
        matchViewModel.reset()
        mutableState.value = mutableState.value.copy(screen = Screen.NAMES, story = null)
    }

    /** `story.archive` (R7.4): архив с возвратом на `screen-story`. */
    fun storyArchiveClicked() {
        archiveOpenedFrom(Screen.STORY)
    }

    /** Открытие архива с `screen-rules` (R1.3) либо `screen-names` (R2.8). */
    fun archiveOpenedFrom(screen: Screen) {
        archiveReturnScreen = screen
        archiveViewModel.refresh()
        mutableState.value = mutableState.value.copy(screen = Screen.ARCHIVE)
    }

    /** Возврат из архива к экрану, из которого он открыт (R8.3). */
    fun archiveBack() {
        mutableState.value = mutableState.value.copy(screen = archiveReturnScreen)
    }

    /** Выбор записи архива (R8.2): чтение на `screen-story` в режиме архивной записи. */
    fun archiveEntrySelected(entryId: ArchiveEntryId) {
        val entry = archiveViewModel.select(entryId) ?: return
        mutableState.value = mutableState.value.copy(
            screen = Screen.STORY,
            story = StoryView(StoryMode.ARCHIVE_ENTRY, entry.story),
        )
    }

    /** Возврат с архивной записи на `screen-archive` (R8.2). */
    fun storyBack() {
        mutableState.value = mutableState.value.copy(screen = Screen.ARCHIVE, story = null)
    }

    /** Вызов правил с игрового экрана (R10.1). */
    fun rulesOpened() {
        mutableState.value = mutableState.value.copy(showRulesDialog = true)
    }

    /** Явный возврат из диалога правил (R10.2, R10.3): состояние экрана сохраняется. */
    fun rulesBack() {
        mutableState.value = mutableState.value.copy(showRulesDialog = false)
    }

    /** Маршрутизирует успешное завершение партии на экран чтения (R7.1). */
    private fun finishBy(operation: () -> MatchViewModel.Outcome) {
        val outcome: MatchViewModel.Outcome = operation()
        if (outcome is MatchViewModel.Outcome.Finished) {
            mutableState.value = mutableState.value.copy(
                screen = Screen.STORY,
                story = StoryView(StoryMode.FINISHED_MATCH, outcome.story),
            )
        }
    }
}

package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.FinishedStory
import com.yurial.abrakazyak.core.game.GameRules
import com.yurial.abrakazyak.core.storage.ArchiveEntryId
import com.yurial.abrakazyak.ui.AppViewModel.Screen
import com.yurial.abrakazyak.ui.AppViewModel.StoryMode
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Контракт маршрутизации и оркестрации (specs/ui.md R1, R2, R3, R4–R8, R10):
 * холодный старт, dialog-resume, старт партии, полный цикл до чтения рассказа,
 * архив с возвратом, правила в любой момент.
 */
class AppViewModelTest {

    private fun vm(
        drafts: FakeDraftStorage = FakeDraftStorage(),
        archive: FakeArchiveStorage = FakeArchiveStorage(),
        language: String = "ru",
    ): AppViewModel = AppViewModel(drafts, archive, language)

    /** Готовая к короткой партии сессия: 2 участника, 1 круг (2 хода, второй — ход завершения). */
    private fun matchSession(
        drafts: FakeDraftStorage = FakeDraftStorage(),
        archive: FakeArchiveStorage = FakeArchiveStorage(),
    ): AppViewModel {
        val model: AppViewModel = vm(drafts, archive)
        model.playClicked()
        model.namesViewModel.onRoundsChange("1")
        model.confirmNames()
        return model
    }

    @Test
    fun coldStartWithoutDraftOpensRules() {
        val model: AppViewModel = vm()
        val state = model.state.value
        assertEquals(Screen.RULES, state.screen)
        assertFalse(state.showResumeDialog)
        assertFalse(state.showRulesDialog)
        assertNull(state.story)
    }

    @Test
    fun coldStartWithDraftShowsResumeDialogOverRules() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        drafts.loadResult = snapshotAwaitingTail()
        val model: AppViewModel = vm(drafts = drafts)
        val state = model.state.value
        assertTrue(state.showResumeDialog)
        assertEquals(Screen.RULES, state.screen)
    }

    @Test
    fun unreadableDraftIsTreatedAsAbsent() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        drafts.loadError = IOException("catalog unavailable")
        val model: AppViewModel = vm(drafts = drafts)
        assertFalse(model.state.value.showResumeDialog)
        assertEquals(Screen.RULES, model.state.value.screen)
    }

    @Test
    fun resumeConfirmedRestoresTurnScreen() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        drafts.loadResult = snapshotAwaitingTail()
        val model: AppViewModel = vm(drafts = drafts)
        model.resumeConfirmed()
        val state = model.state.value
        assertFalse(state.showResumeDialog)
        assertEquals(Screen.MATCH, state.screen)
        assertEquals(MatchViewModel.Screen.TAIL, model.matchViewModel.state.value.activeScreen)
    }

    @Test
    fun resumeConfirmedFinishingTurnOpensFinishScreen() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        drafts.loadResult = snapshotFinishingTurn()
        val model: AppViewModel = vm(drafts = drafts)
        model.resumeConfirmed()
        assertEquals(Screen.MATCH, model.state.value.screen)
        assertEquals(MatchViewModel.Screen.FINISH, model.matchViewModel.state.value.activeScreen)
    }

    @Test
    fun resumeDeclinedRemovesDraftAndOpensNames() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        drafts.loadResult = snapshotAwaitingTail()
        val model: AppViewModel = vm(drafts = drafts)
        model.resumeDeclined()
        val state = model.state.value
        assertFalse(state.showResumeDialog)
        assertEquals(Screen.NAMES, state.screen)
        assertEquals(1, drafts.clearedCount)
    }

    @Test
    fun resumeConfirmedWithoutPendingDraftKeepsRules() {
        val model: AppViewModel = vm()
        model.resumeConfirmed()
        assertEquals(Screen.RULES, model.state.value.screen)
        assertFalse(model.state.value.showResumeDialog)
    }

    @Test
    fun playClickedOpensNames() {
        val model: AppViewModel = vm()
        model.playClicked()
        assertEquals(Screen.NAMES, model.state.value.screen)
    }

    @Test
    fun namesBackReturnsToRules() {
        val model: AppViewModel = vm()
        model.playClicked()
        model.namesBackClicked()
        assertEquals(Screen.RULES, model.state.value.screen)
    }

    @Test
    fun confirmNamesStartsMatchAndReplacesDraft() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        drafts.loadResult = snapshotAwaitingTail()
        val model: AppViewModel = vm(drafts = drafts)
        model.resumeDeclined()
        model.namesViewModel.onNameChange(0, "Аня")
        model.confirmNames()
        assertEquals(Screen.MATCH, model.state.value.screen)
        assertNull(model.state.value.story)
        // storage R1.4: старт новой партии заменяет черновик состоянием новой партии.
        val lastSaved = drafts.saved.last()
        assertEquals("Аня", lastSaved.participants.first().name)
        assertEquals(0, lastSaved.turnIndex)
    }

    @Test
    fun confirmNamesWithInvalidRoundsStaysOnNames() {
        val model: AppViewModel = vm()
        model.playClicked()
        model.namesViewModel.onRoundsChange("0")
        model.confirmNames()
        assertEquals(Screen.NAMES, model.state.value.screen)
    }

    @Test
    fun fullMatchFlowFinishesOnStoryScreen() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val model: AppViewModel = matchSession(drafts, archive)
        assertEquals(Screen.MATCH, model.state.value.screen)
        model.matchViewModel.onChunkDraftChange("Первая реплика")
        model.chunkConfirmed()
        assertEquals(MatchViewModel.Screen.TAIL, model.matchViewModel.state.value.activeScreen)
        model.matchViewModel.onTailDraftChange("хвостик")
        model.tailConfirmed()
        // R6.10: после передачи хода открывается ход завершения — сразу screen-finish.
        assertEquals(MatchViewModel.Screen.FINISH, model.matchViewModel.state.value.activeScreen)
        model.matchViewModel.onFinishDraftChange("Конец истории")
        model.finishConfirmed()
        val state = model.state.value
        assertEquals(Screen.STORY, state.screen)
        assertEquals(StoryMode.FINISHED_MATCH, state.story?.mode)
        assertEquals("Первая реплика\nКонец истории", state.story?.story?.text)
        // storage R1.3 и R2.1: черновик удалён, рассказ в архиве.
        assertEquals(1, drafts.clearedCount)
        assertEquals(1, archive.added.size)
    }

    @Test
    fun templateFinishFlowFinishesOnStoryScreen() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val model: AppViewModel = matchSession(drafts, archive)
        model.matchViewModel.onChunkDraftChange("Реплика")
        model.chunkConfirmed()
        model.templateConfirmed(GameRules.templateEndings("ru").first().templateId)
        val state = model.state.value
        assertEquals(Screen.STORY, state.screen)
        assertEquals("Реплика\nВот такая история получилась.", state.story?.story?.text)
        assertEquals(1, drafts.clearedCount)
        assertEquals(1, archive.added.size)
    }

    @Test
    fun rejectedFinishStaysInMatch() {
        val model: AppViewModel = matchSession()
        model.matchViewModel.onFinishDraftChange(" ")
        model.finishConfirmed()
        assertEquals(Screen.MATCH, model.state.value.screen)
        assertNull(model.state.value.story)
    }

    @Test
    fun storyPlayAgainOpensNamesAndResetsSession() {
        val model: AppViewModel = matchSession()
        model.templateConfirmed(GameRules.templateEndings("ru").first().templateId)
        model.storyPlayAgain()
        val state = model.state.value
        assertEquals(Screen.NAMES, state.screen)
        assertNull(state.story)
        assertNull(model.matchViewModel.state.value.match)
        assertNull(model.matchViewModel.state.value.finishedStory)
    }

    @Test
    fun storyArchiveOpensArchiveWithStoryReturn() {
        val model: AppViewModel = matchSession()
        model.templateConfirmed(GameRules.templateEndings("ru").first().templateId)
        model.storyArchiveClicked()
        assertEquals(Screen.ARCHIVE, model.state.value.screen)
        model.archiveBack()
        assertEquals(Screen.STORY, model.state.value.screen)
        assertEquals(StoryMode.FINISHED_MATCH, model.state.value.story?.mode)
    }

    @Test
    fun archiveFromRulesReturnsToRules() {
        val model: AppViewModel = vm()
        model.archiveOpenedFrom(Screen.RULES)
        assertEquals(Screen.ARCHIVE, model.state.value.screen)
        model.archiveBack()
        assertEquals(Screen.RULES, model.state.value.screen)
    }

    @Test
    fun archiveFromNamesReturnsToNames() {
        val model: AppViewModel = vm()
        model.playClicked()
        model.archiveOpenedFrom(Screen.NAMES)
        assertEquals(Screen.ARCHIVE, model.state.value.screen)
        model.archiveBack()
        assertEquals(Screen.NAMES, model.state.value.screen)
    }

    @Test
    fun archiveEntryOpensStoryInArchiveModeAndReturns() {
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val added = archive.addFinishedStory(FinishedStory(emptyList()))
        val model: AppViewModel = vm(archive = archive)
        model.archiveOpenedFrom(Screen.RULES)
        model.archiveEntrySelected(ArchiveEntryId(added.value))
        val state = model.state.value
        assertEquals(Screen.STORY, state.screen)
        assertEquals(StoryMode.ARCHIVE_ENTRY, state.story?.mode)
        model.storyBack()
        assertEquals(Screen.ARCHIVE, model.state.value.screen)
    }

    @Test
    fun archiveEntryUnknownIdStaysInArchive() {
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val model: AppViewModel = vm(archive = archive)
        model.archiveOpenedFrom(Screen.RULES)
        model.archiveEntrySelected(ArchiveEntryId("entry-99"))
        assertEquals(Screen.ARCHIVE, model.state.value.screen)
        assertNull(model.state.value.story)
    }

    @Test
    fun rulesDialogOpensAndClosesOnMatchScreens() {
        val model: AppViewModel = matchSession()
        model.matchViewModel.onChunkDraftChange("набранный текст")
        model.rulesOpened()
        val open = model.state.value
        assertTrue(open.showRulesDialog)
        // R10.3: состояние вызывающего экрана и партии сохраняется.
        assertEquals("набранный текст", model.matchViewModel.state.value.chunkDraft)
        model.rulesBack()
        assertFalse(model.state.value.showRulesDialog)
        assertEquals(Screen.MATCH, model.state.value.screen)
    }

    @Test
    fun rulesDialogOpensFromNames() {
        val model: AppViewModel = vm()
        model.playClicked()
        model.rulesOpened()
        assertTrue(model.state.value.showRulesDialog)
        model.rulesBack()
        assertFalse(model.state.value.showRulesDialog)
        assertEquals(Screen.NAMES, model.state.value.screen)
    }

    @Test
    fun updateLanguageForwardsToMatchSession() {
        val model: AppViewModel = vm(language = "ru")
        model.updateLanguage("en")
        assertEquals(GameRules.templateEndings("en"), model.matchViewModel.state.value.templates)
    }
}

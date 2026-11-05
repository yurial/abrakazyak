package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.GameError
import com.yurial.abrakazyak.core.game.GameRules
import com.yurial.abrakazyak.core.game.Participant
import com.yurial.abrakazyak.core.game.TurnPhase
import com.yurial.abrakazyak.ui.MatchViewModel.Outcome
import com.yurial.abrakazyak.ui.MatchViewModel.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Контракт игровой сессии (specs/ui.md R4–R6) и автосохранения (storage R1.1,
 * R1.3, R1.4, R2.1, R1.6): подтверждение шагов, черновики неподтверждённого
 * ввода, экран завершения, оба способа завершения, восстановление из снимка.
 */
class MatchViewModelTest {

    private fun vm(
        drafts: FakeDraftStorage = FakeDraftStorage(),
        archive: FakeArchiveStorage = FakeArchiveStorage(),
        language: String = "ru",
    ): MatchViewModel = MatchViewModel(drafts, archive, language)

    private fun started(
        drafts: FakeDraftStorage = FakeDraftStorage(),
        archive: FakeArchiveStorage = FakeArchiveStorage(),
        language: String = "ru",
        players: Int = 2,
        rounds: Int = 1,
    ): MatchViewModel {
        val model: MatchViewModel = vm(drafts, archive, language)
        model.startNewMatch(List(players) { Participant(null) }, rounds)
        return model
    }

    @Test
    fun initialStateHasNoSessionAndRuTemplates() {
        val model: MatchViewModel = vm()
        assertNull(model.state.value.match)
        assertNull(model.state.value.activeScreen)
        assertEquals(GameRules.templateEndings("ru"), model.state.value.templates)
    }

    @Test
    fun startNewMatchOpensChunkScreenAndSavesDraft() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val model: MatchViewModel = started(drafts = drafts)
        val state = model.state.value
        assertEquals(Screen.CHUNK, state.activeScreen)
        assertEquals(TurnPhase.AWAITING_CHUNK, state.match?.phase)
        assertEquals(1, drafts.saved.size)
        assertEquals(state.match?.snapshot(), drafts.saved.last())
    }

    @Test
    fun startNewMatchRejectedKeepsNoSession() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val model: MatchViewModel = vm(drafts = drafts)
        val outcome: Outcome = model.startNewMatch(emptyList(), 1)
        assertIs<Outcome.Rejected>(outcome)
        assertEquals(GameError.EmptyParticipants, outcome.error)
        assertNull(model.state.value.match)
        assertTrue(drafts.saved.isEmpty())
    }

    @Test
    fun submitChunkAdvancesToTailScreenAndSavesDraft() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val model: MatchViewModel = started(drafts = drafts)
        model.onChunkDraftChange("Первая реплика")
        val outcome: Outcome = model.submitChunk()
        assertIs<Outcome.TurnAdvanced>(outcome)
        val state = model.state.value
        assertEquals(Screen.TAIL, state.activeScreen)
        assertEquals(TurnPhase.AWAITING_TAIL, state.match?.phase)
        assertEquals("", state.chunkDraft)
        assertEquals(2, drafts.saved.size)
        assertEquals(state.match?.snapshot(), drafts.saved.last())
    }

    @Test
    fun submitChunkWithBlankDraftIsRejectedWithoutSave() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val model: MatchViewModel = started(drafts = drafts)
        model.onChunkDraftChange("   ")
        val outcome: Outcome = model.submitChunk()
        assertIs<Outcome.Rejected>(outcome)
        assertEquals(GameError.EmptyText, outcome.error)
        assertEquals(Screen.CHUNK, model.state.value.activeScreen)
        assertEquals("   ", model.state.value.chunkDraft)
        assertEquals(1, drafts.saved.size)
    }

    @Test
    fun chunkDraftOverLimitIsNotAccepted() {
        val model: MatchViewModel = started()
        val atLimit: String = "а".repeat(GameRules.MAX_STORY_CHUNK_LENGTH)
        model.onChunkDraftChange(atLimit)
        model.onChunkDraftChange(atLimit + "б")
        assertEquals(atLimit, model.state.value.chunkDraft)
    }

    @Test
    fun chunkDraftWithinLimitIsAccepted() {
        val model: MatchViewModel = started()
        model.onChunkDraftChange("Начало истории")
        assertEquals("Начало истории", model.state.value.chunkDraft)
    }

    @Test
    fun tailScreenFirstTurnShowsConfirmedChunkWithoutPreviousTail() { // ui R5.7, R5.8, T5.3
        val model: MatchViewModel = started(players = 3, rounds = 1)
        model.onChunkDraftChange("реплика первого хода")
        model.submitChunk()
        val state = model.state.value
        assertEquals(Screen.TAIL, state.activeScreen)
        assertNull(state.match?.visibleContext, "первый ход: хвоста предыдущего участника нет (R5.7)")
        assertEquals(
            "реплика первого хода",
            state.match?.currentTurnChunk(),
            "подтверждённая реплика текущего хода доходит до экрана хвоста (R5.8)",
        )
    }

    @Test
    fun tailScreenNextTurnShowsPreviousTailAndCurrentChunk() { // ui R5.7, R5.8, T5.3
        val model: MatchViewModel = started(players = 3, rounds = 2)
        model.onChunkDraftChange("реплика 0")
        model.submitChunk()
        model.onTailDraftChange("хвост 0")
        model.submitTail()
        model.onChunkDraftChange("реплика 1")
        model.submitChunk()
        val state = model.state.value
        assertEquals(Screen.TAIL, state.activeScreen)
        assertEquals("хвост 0", state.match?.visibleContext, "хвост предыдущего участника в состоянии (R5.7)")
        assertEquals(
            "реплика 1",
            state.match?.currentTurnChunk(),
            "подтверждённая реплика второго хода в состоянии (R5.8); порядок чтения «хвост, затем реплика» задаёт компоновка экрана",
        )
    }

    @Test
    fun finishedStoryWithTailsReachesOutcomeAndArchiveInOrder() { // game R4.1, R4.2, ui R7.1, storage R2.1
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val model: MatchViewModel = started(archive = archive, players = 3, rounds = 1)
        model.onChunkDraftChange("реплика 0")
        model.submitChunk()
        model.onTailDraftChange("хвост 0")
        model.submitTail()
        model.onChunkDraftChange("реплика 1")
        model.submitChunk()
        model.openFinish()
        model.onFinishDraftChange("Финал")
        val outcome: Outcome = model.finishWithReply()
        val story = assertIs<Outcome.Finished>(outcome).story
        assertEquals(
            listOf("реплика 0", "хвост 0", "реплика 1", "Финал"),
            story.entries.map { it.text },
            "хвост входит в рассказ между репликами в порядке написания (game R4.1, R4.2)",
        )
        assertEquals(
            listOf(story),
            archive.added,
            "рассказ с хвостами и авторством строк попадает в архив тем же объектом (storage R2.1)",
        )
    }

    @Test
    fun submitTailReturnsToChunkOfNextTurn() {
        val model: MatchViewModel = started(players = 3, rounds = 1)
        model.onChunkDraftChange("реплика")
        model.submitChunk()
        model.onTailDraftChange("хвост")
        val outcome: Outcome = model.submitTail()
        assertIs<Outcome.TurnAdvanced>(outcome)
        val state = model.state.value
        assertEquals(Screen.CHUNK, state.activeScreen)
        assertEquals(1, state.match?.turnIndex)
        assertEquals("хвост", state.match?.visibleContext)
        assertEquals("", state.tailDraft)
    }

    @Test
    fun submitTailBeforeFinishingTurnOpensFinishScreen() {
        val model: MatchViewModel = started(players = 2, rounds = 1)
        model.onChunkDraftChange("реплика")
        model.submitChunk()
        model.onTailDraftChange("хвост")
        val outcome: Outcome = model.submitTail()
        assertIs<Outcome.TurnAdvanced>(outcome)
        val state = model.state.value
        assertEquals(TurnPhase.AWAITING_FINISH, state.match?.phase)
        assertEquals(Screen.FINISH, state.activeScreen)
        assertTrue(state.showFinishScreen)
    }

    @Test
    fun openFinishKeepsUnconfirmedDraftsAndAbandonRestoresThem() {
        val model: MatchViewModel = started()
        model.onChunkDraftChange("набрано")
        model.openFinish()
        assertEquals(Screen.FINISH, model.state.value.activeScreen)
        assertEquals("набрано", model.state.value.chunkDraft)
        model.abandonFinish()
        assertEquals(Screen.CHUNK, model.state.value.activeScreen)
        assertEquals("набрано", model.state.value.chunkDraft)
    }

    @Test
    fun abandonFinishIsGuardedOnFinishingTurn() {
        val model: MatchViewModel = started()
        model.onChunkDraftChange("реплика")
        model.submitChunk()
        model.onTailDraftChange("хвост")
        model.submitTail()
        model.abandonFinish()
        assertEquals(Screen.FINISH, model.state.value.activeScreen)
        assertTrue(model.state.value.showFinishScreen)
    }

    @Test
    fun finishWithReplyFromChunkReplacesTurn() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val model: MatchViewModel = started(drafts = drafts, archive = archive)
        model.onChunkDraftChange("набрано, но не подтверждено")
        model.openFinish()
        model.onFinishDraftChange("Финальная реплика")
        val outcome: Outcome = model.finishWithReply()
        val story = assertIs<Outcome.Finished>(outcome).story
        assertEquals("Финальная реплика", story.text)
        assertEquals("Финальная реплика", model.state.value.finishedStory?.text)
        assertEquals(1, drafts.clearedCount)
        assertEquals(listOf(story), archive.added)
    }

    @Test
    fun finishWithReplyFromTailKeepsConfirmedChunk() {
        val model: MatchViewModel = started()
        model.onChunkDraftChange("подтверждённая реплика")
        model.submitChunk()
        model.onTailDraftChange("недописанный хвост")
        model.openFinish()
        model.onFinishDraftChange("Финал")
        val outcome: Outcome = model.finishWithReply()
        val story = assertIs<Outcome.Finished>(outcome).story
        assertEquals(listOf("подтверждённая реплика", "Финал"), story.entries.map { it.text })
        // Неподтверждённый шаг хвоста этого хода в рассказ не входит (game R3.8, R4.2); сессия завершена.
        assertNull(model.state.value.activeScreen)
        assertEquals(story, model.state.value.finishedStory)
    }

    @Test
    fun finishWithReplyBlankIsRejectedWithoutStorageEffects() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val model: MatchViewModel = started(drafts = drafts, archive = archive)
        model.onFinishDraftChange(" ")
        val outcome: Outcome = model.finishWithReply()
        assertIs<Outcome.Rejected>(outcome)
        assertEquals(GameError.EmptyText, outcome.error)
        assertNull(model.state.value.finishedStory)
        assertEquals(0, drafts.clearedCount)
        assertTrue(archive.added.isEmpty())
    }

    @Test
    fun finishWithTemplateFinishesImmediately() {
        val drafts: FakeDraftStorage = FakeDraftStorage()
        val archive: FakeArchiveStorage = FakeArchiveStorage()
        val model: MatchViewModel = started(drafts = drafts, archive = archive)
        val template = GameRules.templateEndings("ru").first()
        val outcome: Outcome = model.finishWithTemplate(template.templateId)
        val story = assertIs<Outcome.Finished>(outcome).story
        assertEquals(template.text, story.text)
        assertEquals(1, drafts.clearedCount)
        assertEquals(listOf(story), archive.added)
    }

    @Test
    fun finishWithUnknownTemplateIsRejected() {
        val model: MatchViewModel = started()
        val outcome: Outcome = model.finishWithTemplate("zz-99")
        assertIs<Outcome.Rejected>(outcome)
        assertEquals(GameError.UnknownTemplate, outcome.error)
        assertNull(model.state.value.finishedStory)
    }

    @Test
    fun tailDraftOverLimitIsNotAccepted() {
        val model: MatchViewModel = started()
        val atLimit: String = "х".repeat(GameRules.MAX_VISIBLE_TAIL_LENGTH)
        model.onTailDraftChange(atLimit)
        model.onTailDraftChange(atLimit + "!")
        assertEquals(atLimit, model.state.value.tailDraft)
    }

    @Test
    fun tailDraftWithinLimitIsAccepted() {
        val model: MatchViewModel = started()
        model.onTailDraftChange("хвостик")
        assertEquals("хвостик", model.state.value.tailDraft)
    }

    @Test
    fun finishDraftOverLimitIsNotAccepted() {
        val model: MatchViewModel = started()
        val atLimit: String = "ф".repeat(GameRules.MAX_STORY_CHUNK_LENGTH)
        model.onFinishDraftChange(atLimit)
        model.onFinishDraftChange(atLimit + "!")
        assertEquals(atLimit, model.state.value.finishDraft)
    }

    @Test
    fun submitTailClearsFinishDraftOfAdvancedTurn() {
        val model: MatchViewModel = started(players = 3, rounds = 2)
        model.onChunkDraftChange("реплика 0")
        model.submitChunk()
        model.onFinishDraftChange("черновик финала")
        model.onTailDraftChange("хвост 0")
        model.submitTail()
        assertEquals("", model.state.value.finishDraft)
    }

    @Test
    fun operationsWithoutSessionReturnNoSession() {
        val model: MatchViewModel = vm()
        assertIs<Outcome.NoSession>(model.submitChunk())
        assertIs<Outcome.NoSession>(model.submitTail())
        assertIs<Outcome.NoSession>(model.finishWithReply())
        assertIs<Outcome.NoSession>(model.finishWithTemplate("ru-1"))
    }

    @Test
    fun restoreAwaitingTailOpensTailScreenWithoutDrafts() {
        val model: MatchViewModel = vm()
        model.restore(snapshotAwaitingTail())
        val state = model.state.value
        assertEquals(Screen.TAIL, state.activeScreen)
        assertEquals(TurnPhase.AWAITING_TAIL, state.match?.phase)
        assertEquals("", state.chunkDraft)
        assertEquals("", state.tailDraft)
        assertEquals(0, state.match?.turnIndex)
        assertNull(state.match?.visibleContext)
        assertEquals(
            "реплика 0",
            state.match?.currentTurnChunk(),
            "восстановленный ход несёт реплику для контекста экрана хвоста (R5.8): переход не теряет данные",
        )
    }

    @Test
    fun restoreSecondTurnShowsVisibleContextOnChunkScreen() {
        val model: MatchViewModel = vm()
        model.restore(snapshotSecondTurn())
        val state = model.state.value
        assertEquals(Screen.CHUNK, state.activeScreen)
        assertEquals("хвост 0", state.match?.visibleContext)
    }

    @Test
    fun restoreFinishingTurnOpensFinishScreen() {
        val model: MatchViewModel = vm()
        model.restore(snapshotFinishingTurn())
        val state = model.state.value
        assertEquals(Screen.FINISH, state.activeScreen)
        assertTrue(state.showFinishScreen)
        assertEquals("хвост 0", state.match?.visibleContext)
    }

    @Test
    fun finishedMatchHasNoActiveScreen() {
        val model: MatchViewModel = started()
        val template = GameRules.templateEndings("ru").first()
        model.finishWithTemplate(template.templateId)
        assertEquals(TurnPhase.FINISHED, model.state.value.match?.phase)
        assertNull(model.state.value.activeScreen)
    }

    @Test
    fun updateLanguageRecomputesTemplates() {
        val model: MatchViewModel = vm(language = "ru")
        assertEquals(GameRules.templateEndings("ru"), model.state.value.templates)
        model.updateLanguage("en")
        assertEquals(GameRules.templateEndings("en"), model.state.value.templates)
        model.updateLanguage("de")
        assertTrue(model.state.value.templates.isEmpty())
    }

    @Test
    fun resetClearsSession() {
        val model: MatchViewModel = started()
        model.onChunkDraftChange("черновик")
        model.reset()
        val state = model.state.value
        assertNull(state.match)
        assertNull(state.activeScreen)
        assertNull(state.finishedStory)
        assertEquals("", state.chunkDraft)
        assertFalse(state.showFinishScreen)
    }
}

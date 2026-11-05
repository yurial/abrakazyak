package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * T1.4, T3.1–T3.3 — проверки завершения: окно валидности, финальная реплика, шаблоны.
 */
class MatchFinishTest {

    private val squad: List<Participant> =
        listOf(Participant("Аня"), Participant("Боря"), Participant(null))

    @Test
    fun finishingTurnRejectsNormalSteps() { // R1.10, R3.4, T1.4: ход завершения без шагов реплики и хвоста
        var match: Match = startMatchOrFail(squad, 2)
        repeat(5) { index: Int ->
            match = playOrdinaryTurn(match, "реплика-$index", "хвост-$index")
        }
        assertEquals(TurnPhase.AWAITING_FINISH, match.phase)
        assertEquals(5, match.turnIndex)
        assertEquals(GameError.InvalidStep, match.submitChunk("обычный шаг на ходе завершения").requireFailure())
        assertEquals(GameError.InvalidStep, match.submitTail("шаг хвоста на ходе завершения").requireFailure())
    }

    @Test
    fun naturalCompletionCountsOrdinaryTurns() { // R1.9, R1.10, T1.4
        val finished: Match = playToNaturalFinish(squad, 2)
        assertTrue(finished.isFinished, "партия естественно завершена")
        assertEquals(null, finished.currentParticipant, "после завершения текущего участника нет")
        assertEquals(5, finished.chunks.size, "обычных ходов на единицу меньше участников × кругов")
        assertEquals(5, finished.turnIndex, "последний ход — последний участник последнего круга (5 % 3 = 2)")
        assertEquals(2, finished.finishedStory()?.entries?.lastOrNull()?.authorIndex, "концовка — автор хода завершения")
        assertEquals(TurnPhase.FINISHED, finished.phase)
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(11, story.entries.size, "реплики обычных ходов, их хвосты и концовка (R4.1); реплик и концовка — шесть (R1.11)")
        assertEquals(6, finished.snapshot().lines.count { line: StoryLine -> line !is StoryLine.Tail })
    }

    @Test
    fun operationsAfterNaturalFinishReturnMatchFinished() { // game/match-finished, T1.4
        val finished: Match = playToNaturalFinish(squad, 2)
        assertEquals(GameError.MatchFinished, finished.submitChunk("x").requireFailure())
        assertEquals(GameError.MatchFinished, finished.submitTail("x").requireFailure())
        assertEquals(GameError.MatchFinished, finished.finishWithReply("x").requireFailure())
        assertEquals(GameError.MatchFinished, finished.finishWithTemplate("ru-1").requireFailure())
    }

    @Test
    fun earlyFinishWithReplyOnFirstTurn() { // R3.1, R3.2, T3.1: досрочное завершение на первом ходе
        val fresh: Match = startMatchOrFail(squad, 2)
        assertEquals(GameError.TextTooLong, fresh.finishWithReply("ф".repeat(GameRules.MAX_STORY_CHUNK_LENGTH + 1)).requireFailure())
        assertEquals(GameError.EmptyText, fresh.finishWithReply("").requireFailure())
        assertEquals(GameError.EmptyText, fresh.finishWithReply("  ").requireFailure())
        val finished: Match = fresh.finishWithReply("Так и закончим.").requireSuccess()
        assertEquals(TurnPhase.FINISHED, finished.phase)
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(listOf("Так и закончим."), story.entries.map { it.text }, "реплика этого хода не пишется (R3.7)")
    }

    @Test
    fun earlyFinishWithReplyMidQueue() { // R3.1, R3.2, T3.1: досрочное завершение в середине очереди
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "первая реплика", "первый хвост")
        val finished: Match = match.finishWithReply("Хватит, читаем рассказ.").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(
            listOf("первая реплика", "первый хвост", "Хватит, читаем рассказ."),
            story.entries.map { it.text },
            "подтверждённый хвост первого хода входит в рассказ (R4.1)",
        )
    }

    @Test
    fun finishByTemplateForEachLanguage() { // R3.3, T3.2: текст шаблона становится концовкой, финальная реплика не пишется
        GameRules.templateLanguageKeys.forEach { languageKey: String ->
            val template: TemplateEnding = GameRules.templateEndings(languageKey).first()
            val match: Match = startMatchOrFail(squad, 2)
            val finished: Match = match.finishWithTemplate(template.templateId).requireSuccess()
            val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
            assertEquals(listOf(template.text), story.entries.map { it.text })
            assertEquals(GameError.MatchFinished, finished.submitTail("после завершения").requireFailure())
        }
    }

    @Test
    fun unknownTemplateRejected() { // game/unknown-template
        val fresh: Match = startMatchOrFail(squad, 2)
        assertEquals(GameError.UnknownTemplate, fresh.finishWithTemplate("no-such-id").requireFailure())
        val playable: Match = fresh // состояние после ошибки не изменилось: обычный ход доступен
            .submitChunk("партия продолжается").requireSuccess()
        assertEquals(TurnPhase.AWAITING_TAIL, playable.phase)
    }

    @Test
    fun finishBeforeChunkReplacesWholeTurn() { // R3.6, R3.7, T3.3: завершение до submitChunk
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "реплика-0", "хвост-0")
        val finished: Match = match.finishWithReply("вместо целого хода").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(
            listOf("реплика-0", "хвост-0", "вместо целого хода"),
            story.entries.map { it.text },
            "заменённый ход не внёс ни реплики, ни хвоста (R3.7)",
        )
    }

    @Test
    fun finishAfterChunkKeepsChunkAndDropsTailStep() { // R3.6, R3.8, T3.3: завершение после submitChunk до submitTail
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "реплика-0", "хвост-0")
        val withChunk: Match = match.submitChunk("подтверждённая реплика").requireSuccess()
        val finished: Match = withChunk.finishWithTemplate("ru-2").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(
            listOf("реплика-0", "хвост-0", "подтверждённая реплика", "Вот такие дела, собачка."),
            story.entries.map { it.text },
            "подтверждённая реплика осталась, неподтверждённый шаг хвоста в рассказ не вошёл (R3.8, R4.2)",
        )
        assertEquals(null, finished.visibleContext, "хвост этого хода не записан")
    }

    @Test
    fun finishingPassedTurnRejectedAsInvalidStep() { // R3.6, T3.3: завершение уже переданного хода
        val match: Match = startMatchOrFail(squad, 2)
        val passed: Match = match.submitChunk("реплика").requireSuccess().submitTail("хвост").requireSuccess()
        assertEquals(GameError.InvalidStep, match.finishWithReply("вдогонку переданному ходу").requireFailure())
        assertEquals(GameError.InvalidStep, match.finishWithTemplate("ru-1").requireFailure())
        assertEquals(GameError.InvalidStep, match.submitChunk("опоздавшая реплика").requireFailure())
        assertEquals(GameError.InvalidStep, match.submitTail("опоздавший хвост").requireFailure())
        assertEquals(TurnPhase.AWAITING_CHUNK, passed.phase, "состояние партии не изменилось")
    }

    @Test
    fun earlyFinishAvailableOnNextOwnTurn() { // R3.1, T3.3: досрочное завершение на следующем своём ходе
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "реплика-0", "хвост-0") // ход первого участника передан
        match = playOrdinaryTurn(match, "реплика-1", "хвост-1") // ход второго участника
        match = playOrdinaryTurn(match, "реплика-2", "хвост-2") // ход третьего участника
        assertEquals(0, match.turnIndex % match.participants.size, "круг замкнулся: снова ход первого участника")
        val finished: Match = match.finishWithReply("на своём следующем ходе").requireSuccess()
        assertEquals(TurnPhase.FINISHED, finished.phase)
    }
}

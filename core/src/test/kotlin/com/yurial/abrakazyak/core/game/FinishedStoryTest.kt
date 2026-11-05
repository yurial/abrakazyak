package com.yurial.abrakazyak.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * T4.1, T4.2 — проверки готового рассказа: порядок, строки, хвосты, авторство.
 */
class FinishedStoryTest {

    private val squad: List<Participant> =
        listOf(Participant("Аня"), Participant(null), Participant("Вера"))
    @Test
    fun storyAssemblesChunksTailsAndReplyInWritingOrder() { // R4.1, R4.2, T4.1, E4.1
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "Жил-был кот", "и он пошёл")
        match = playOrdinaryTurn(match, "по дорогам туманным", "и встретил сову")
        match = playOrdinaryTurn(match, "сова дала совет", "совет был прост")
        match = playOrdinaryTurn(match, "кот кивнул и пошёл домой", "дорога светилась")
        match = match.submitChunk("домой он пришёл героем").requireSuccess()
        val finished: Match = match.finishWithReply("Вот и вся история кота.").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(
            listOf(
                "Жил-был кот",
                "и он пошёл",
                "по дорогам туманным",
                "и встретил сову",
                "сова дала совет",
                "совет был прост",
                "кот кивнул и пошёл домой",
                "дорога светилась",
                "домой он пришёл героем",
                "Вот и вся история кота.",
            ),
            story.entries.map { it.text },
            "порядок «реплика, хвост, следующая реплика, …» с концовкой в конце: хвост предшествует реплике, которую он начал (R4.2, E4.2)",
        )
        assertEquals(10, story.entries.size, "все реплики, все хвосты и концовка — отдельные строки (R4.1)")
        assertEquals(
            "Жил-был кот\nи он пошёл\nпо дорогам туманным\nи встретил сову\nсова дала совет\n" +
                "совет был прост\nкот кивнул и пошёл домой\nдорога светилась\n" +
                "домой он пришёл героем\nВот и вся история кота.",
            story.text,
            "каждая часть с новой строки (R4.1)",
        )
    }

    @Test
    fun earlyFinishAfterChunkKeepsChunkWithoutTailStep() { // R3.8, R4.2, T4.1: неподтверждённый шаг хвоста не входит
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "реплика первого хода", "хвост первого хода")
        match = match.submitChunk("реплика второго хода").requireSuccess()
        val finished: Match = match.finishWithReply("Дочитали без хвоста.").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(
            listOf("реплика первого хода", "хвост первого хода", "реплика второго хода", "Дочитали без хвоста."),
            story.entries.map { it.text },
            "подтверждённые части входят по порядку написания; неподтверждённый шаг хвоста в рассказ не попадает (R3.8)",
        )
        assertEquals(0, story.entries[1].authorIndex, "хвост несёт авторство написавшего его участника (R4.3)")
    }

    @Test
    fun storyWithTemplateEndingJoinsSameWay() { // R4.1, T4.1 (вариант с шаблонной концовкой)
        var match: Match = startMatchOrFail(squad, 1)
        match = playOrdinaryTurn(match, "история началась", "и продолжилась")
        match = match.submitChunk("и вот развязка").requireSuccess()
        val finished: Match = match.finishWithTemplate("ru-1").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(
            "история началась\nи продолжилась\nи вот развязка\nВот такая история получилась.",
            story.text,
            "хвост первого хода входит перед репликой, которую он начал",
        )
    }

    @Test
    fun storyLinesCarryAuthorsIncludingUnnamed() { // R1.4, R4.3, T4.2
        val finished: Match = playToNaturalFinish(squad, 1)
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        val authors: List<Pair<Int, String?>> = story.entries.map { it.authorIndex to it.authorName }
        assertEquals(
            listOf(0 to "Аня", 0 to "Аня", 1 to null, 1 to null, 2 to "Вера"),
            authors,
            "каждая строка реплики и хвоста несёт связь с автором, включая участников без имён; концовка — автор хода завершения (R4.3)",
        )
        assertNull(story.entries[2].authorName, "имя необязательно; авторство без имени — область ui")
    }

    @Test
    fun unfinishedMatchHasNoFinishedStory() {
        val fresh: Match = startMatchOrFail(squad, 2)
        assertTrue(!fresh.isFinished, "партия только начата")
        assertNull(fresh.finishedStory())
        val midTurn: Match = fresh.submitChunk("черновик истории").requireSuccess()
        assertNull(midTurn.finishedStory())
    }
}

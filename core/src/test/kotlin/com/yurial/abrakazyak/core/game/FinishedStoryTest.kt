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
    fun storyAssemblesChunksAndReplyInWritingOrder() { // R4.1, R4.2, T4.1
        var match: Match = startMatchOrFail(squad, 2)
        match = playOrdinaryTurn(match, "Жил-был кот", "и он пошёл")
        match = playOrdinaryTurn(match, "по дорогам туманным", "и встретил сову")
        match = playOrdinaryTurn(match, "сова дала совет", "совет был прост")
        match = playOrdinaryTurn(match, "кот кивнул и пошёл домой", "дорога светилась")
        match = match.submitChunk("домой он пришёл героем").requireSuccess()
        val finished: Match = match.finishWithReply("Вот и вся история кота.").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals(6, story.entries.size)
        assertEquals(
            "Жил-был кот\nпо дорогам туманным\nсова дала совет\nкот кивнул и пошёл домой\nдомой он пришёл героем\nВот и вся история кота.",
            story.text,
            "каждая реплика и концовка с новой строки (R4.1)",
        )
        assertTrue(!story.text.contains("и он пошёл"), "хвост не входит в рассказ ни целиком (R4.2)")
        assertTrue(!story.text.contains("совет был прост"), "хвост не входит в рассказ фрагментом (R4.2)")
    }

    @Test
    fun storyWithTemplateEndingJoinsSameWay() { // R4.1, T4.1 (вариант с шаблонной концовкой)
        var match: Match = startMatchOrFail(squad, 1)
        match = playOrdinaryTurn(match, "история началась", "и продолжилась")
        match = match.submitChunk("и вот развязка").requireSuccess()
        val finished: Match = match.finishWithTemplate("ru-1").requireSuccess()
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        assertEquals("история началась\nи вот развязка\nВот такая история получилась.", story.text)
    }

    @Test
    fun storyLinesCarryAuthorsIncludingUnnamed() { // R1.4, R4.3, T4.2
        val finished: Match = playToNaturalFinish(squad, 1)
        val story: FinishedStory = finished.finishedStory() ?: error("рассказ должен существовать")
        val authors: List<Pair<Int, String?>> = story.entries.map { it.authorIndex to it.authorName }
        assertEquals(
            listOf(0 to "Аня", 1 to null, 2 to "Вера"),
            authors,
            "строка каждой реплики несёт связь с автором, включая участников без имён; концовка — автор хода завершения",
        )
        assertNull(story.entries[1].authorName, "имя необязательно; авторство без имени — область ui")
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

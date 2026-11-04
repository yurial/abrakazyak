package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.FinishedStory
import com.yurial.abrakazyak.core.game.FinishedStoryEntry
import com.yurial.abrakazyak.core.storage.ArchiveEntryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Контракт экрана архива (specs/ui.md R8.1, R8.2; storage R2.2). */
class ArchiveViewModelTest {

    @Test
    fun initialStateIsEmpty() {
        val vm: ArchiveViewModel = ArchiveViewModel(FakeArchiveStorage())
        assertTrue(vm.state.value.entries.isEmpty())
    }

    @Test
    fun refreshListsEntriesFromStorage() {
        val storage: FakeArchiveStorage = FakeArchiveStorage()
        val first: FinishedStory = story("первый рассказ")
        val second: FinishedStory = story("второй рассказ")
        val firstId: ArchiveEntryId = storage.addFinishedStory(first)
        val secondId: ArchiveEntryId = storage.addFinishedStory(second)
        val vm: ArchiveViewModel = ArchiveViewModel(storage)
        vm.refresh()
        val entries = vm.state.value.entries
        assertEquals(2, entries.size)
        // Порядок хранилища — от новых к старым (storage R2.2).
        assertEquals(secondId, entries.first().entryId)
        assertEquals(firstId, entries.last().entryId)
    }

    @Test
    fun selectReturnsEntryById() {
        val storage: FakeArchiveStorage = FakeArchiveStorage()
        val added: FinishedStory = story("архивный рассказ")
        val id: ArchiveEntryId = storage.addFinishedStory(added)
        val vm: ArchiveViewModel = ArchiveViewModel(storage)
        val entry = vm.select(id)
        assertEquals(id, entry?.entryId)
        assertEquals(added, entry?.story)
    }

    @Test
    fun selectUnknownIdReturnsNull() {
        val storage: FakeArchiveStorage = FakeArchiveStorage()
        storage.addFinishedStory(story("рассказ"))
        val vm: ArchiveViewModel = ArchiveViewModel(storage)
        vm.refresh()
        assertNull(vm.select(ArchiveEntryId("entry-99")))
        assertEquals(1, vm.state.value.entries.size)
    }

    private fun story(text: String): FinishedStory =
        FinishedStory(listOf(FinishedStoryEntry(text, 0, null)))
}

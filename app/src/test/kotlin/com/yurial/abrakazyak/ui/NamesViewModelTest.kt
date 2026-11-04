package com.yurial.abrakazyak.ui

import com.yurial.abrakazyak.core.game.GameRules
import com.yurial.abrakazyak.core.game.Participant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Контракт экрана участников (specs/ui.md R2): строки и имена (R2.2, R2.3),
 * число кругов с предзаполнением и защитой ручной правки (R2.4–R2.7).
 */
class NamesViewModelTest {

    @Test
    fun initialStateHasTwoRowsAndComputedRounds() {
        val vm: NamesViewModel = NamesViewModel()
        val state: NamesViewModel.UiState = vm.state.value
        assertEquals(listOf("", ""), state.names)
        assertFalse(state.roundsEditedManually)
        assertEquals(GameRules.defaultRoundCount(2), state.defaultRounds)
        assertEquals(state.defaultRounds.toString(), state.roundsText)
        assertTrue(state.canStart)
    }

    @Test
    fun nameChangeUpdatesRowOnly() {
        val vm: NamesViewModel = NamesViewModel()
        vm.onNameChange(1, "Аня")
        assertEquals(listOf("", "Аня"), vm.state.value.names)
    }

    @Test
    fun addRowAppendsAndRecomputesRoundsBeforeManualEdit() {
        val vm: NamesViewModel = NamesViewModel()
        vm.addRow()
        val state: NamesViewModel.UiState = vm.state.value
        assertEquals(listOf("", "", ""), state.names)
        assertEquals(GameRules.defaultRoundCount(3), state.defaultRounds)
        assertEquals(state.defaultRounds.toString(), state.roundsText)
    }

    @Test
    fun deleteRowRemovesAndRecomputesRoundsBeforeManualEdit() {
        val vm: NamesViewModel = NamesViewModel()
        vm.deleteRow(1)
        val state: NamesViewModel.UiState = vm.state.value
        assertEquals(listOf(""), state.names)
        assertEquals(GameRules.defaultRoundCount(1), state.defaultRounds)
        assertEquals(state.defaultRounds.toString(), state.roundsText)
    }

    @Test
    fun lastRowIsNeverDeleted() {
        val vm: NamesViewModel = NamesViewModel()
        vm.deleteRow(1)
        vm.onNameChange(0, "Один")
        vm.deleteRow(0)
        assertEquals(listOf("Один"), vm.state.value.names)
    }

    @Test
    fun manualRoundsEditSurvivesRowChanges() {
        val vm: NamesViewModel = NamesViewModel()
        vm.onRoundsChange("3")
        vm.addRow()
        vm.deleteRow(1)
        vm.addRow()
        val state: NamesViewModel.UiState = vm.state.value
        assertTrue(state.roundsEditedManually)
        assertEquals("3", state.roundsText)
    }

    @Test
    fun roundsChangeAcceptsDigitsOnly() {
        val vm: NamesViewModel = NamesViewModel()
        vm.onRoundsChange("4a2-7")
        val state: NamesViewModel.UiState = vm.state.value
        assertEquals("427", state.roundsText)
        assertTrue(state.roundsEditedManually)
        assertTrue(state.canStart)
    }

    @Test
    fun roundsBelowOneBlockStart() {
        val vm: NamesViewModel = NamesViewModel()
        vm.onRoundsChange("0")
        assertFalse(vm.state.value.canStart)
        vm.onRoundsChange("")
        assertFalse(vm.state.value.canStart)
    }

    @Test
    fun emptyRoundsTextBlocksStart() {
        val vm: NamesViewModel = NamesViewModel()
        vm.onRoundsChange("a")
        assertFalse(vm.state.value.canStart)
    }

    @Test
    fun startParamsMapRowsToParticipants() {
        val vm: NamesViewModel = NamesViewModel()
        vm.onNameChange(0, "Аня")
        vm.onNameChange(1, "   ")
        vm.onRoundsChange("2")
        val params: NamesViewModel.StartParams? = vm.startParams()
        assertEquals(
            listOf(Participant("Аня"), Participant(null)),
            params?.participants,
        )
        assertEquals(2, params?.roundCount)
    }

    @Test
    fun startParamsWithoutRoundsAreNull() {
        val vm: NamesViewModel = NamesViewModel()
        vm.onRoundsChange("0")
        assertNull(vm.startParams())
    }

    @Test
    fun startParamsUseRowsAtConfirmationMoment() {
        val vm: NamesViewModel = NamesViewModel()
        vm.addRow()
        vm.onNameChange(2, "Третий")
        val params: NamesViewModel.StartParams? = vm.startParams()
        assertEquals(3, params?.participants?.size)
        assertEquals(GameRules.defaultRoundCount(3).toString(), vm.state.value.roundsText)
    }
}

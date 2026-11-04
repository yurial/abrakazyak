package com.yurial.abrakazyak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yurial.abrakazyak.R

/**
 * `screen-names` (ui R2): строки участников с добавлением/удалением — минимум
 * одна строка (R2.2), опциональные имена (R2.2, game R1.3), число кругов с
 * предзаполнением `defaultRoundCount` (R2.6) и защитой ручной правки (R2.7),
 * подтверждение старта (R2.3). Доступен архив (R2.8) и правила (R10.1).
 */
@Composable
fun NamesScreen(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val state: NamesViewModel.UiState by appViewModel.namesViewModel.state.collectAsState()
    BackHandler { appViewModel.namesBackClicked() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.names_title),
            onBack = appViewModel::namesBackClicked,
            onRulesOpen = appViewModel::rulesOpened,
        )
        Text(
            text = stringResource(R.string.names_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        ParticipantRows(state, appViewModel.namesViewModel)
        TextButton(onClick = appViewModel.namesViewModel::addRow) {
            Text(text = stringResource(R.string.names_add))
        }
        RoundsField(state, appViewModel.namesViewModel)
        Button(
            onClick = appViewModel::confirmNames,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.names_start))
        }
        // R2.8: вход в архив с экрана участников; возврат — в NamesScreen-состояние (R8.3).
        OutlinedButton(
            onClick = { appViewModel.archiveOpenedFrom(AppViewModel.Screen.NAMES) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.rules_archive))
        }
    }
}

/** Строки участников: имя в каждой строке опционально, удаление при числе строк более одной (R2.2). */
@Composable
private fun ParticipantRows(state: NamesViewModel.UiState, viewModel: NamesViewModel) {
    state.names.forEachIndexed { rowIndex: Int, name: String ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { value: String -> viewModel.onNameChange(rowIndex, value) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            IconButton(
                onClick = { viewModel.deleteRow(rowIndex) },
                enabled = state.names.size > 1,
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
            }
        }
    }
}

/** Поле числа кругов: только цифры (R2.4), предзаполнение и пересчёт — в [NamesViewModel] (R2.6, R2.7). */
@Composable
private fun RoundsField(state: NamesViewModel.UiState, viewModel: NamesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.names_rounds),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = state.roundsText,
            onValueChange = viewModel::onRoundsChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

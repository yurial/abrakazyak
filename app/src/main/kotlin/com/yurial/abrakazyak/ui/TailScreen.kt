package com.yurial.abrakazyak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yurial.abrakazyak.R
import com.yurial.abrakazyak.core.game.GameRules

/**
 * `screen-tail` (ui R5): заголовок (R5.1), поле видимого хвоста с жёстким
 * лимитом (R5.2), счётчик (R5.3), кнопка `tail.done` (R5.4) и `tail.finish` (R5.6).
 * Перед полем ввода, в порядке чтения: видимый хвост предыдущего участника
 * (на первом ходе отсутствует, R5.7) и подтверждённая реплика текущего хода
 * (R5.8) — без подписей, различимые блоки. Подтверждённая реплика хода
 * остаётся в рассказе при завершении (R5.6).
 */
@Composable
fun TailScreen(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val state: MatchViewModel.UiState by appViewModel.matchViewModel.state.collectAsState()
    val turnIndex: Int = state.match?.turnIndex ?: 0
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.app_name),
            onRulesOpen = appViewModel::rulesOpened,
        )
        Text(
            text = stringResource(R.string.tail_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (turnIndex > 0) {
            VisibleContextCard(visibleContext = state.match?.visibleContext ?: "")
        }
        // Подтверждённая реплика текущего хода после хвоста предыдущего участника,
        // перед полем ввода (R5.8); рамочный блок отличает её от хвоста (R5.7).
        val confirmedChunk: String? = state.match?.currentTurnChunk()
        if (confirmedChunk != null) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = confirmedChunk,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        OutlinedTextField(
            value = state.tailDraft,
            onValueChange = appViewModel.matchViewModel::onTailDraftChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                R.string.tail_counter,
                GameRules.MAX_VISIBLE_TAIL_LENGTH - state.tailDraft.length,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = appViewModel::tailConfirmed,
            enabled = state.tailDraft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.tail_done))
        }
        TextButton(
            onClick = appViewModel::finishOpened,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.tail_finish))
        }
    }
}

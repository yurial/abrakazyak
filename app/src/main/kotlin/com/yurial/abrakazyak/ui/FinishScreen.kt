package com.yurial.abrakazyak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.yurial.abrakazyak.core.game.TurnPhase

/**
 * `screen-finish` (ui R6): два способа завершения (R6.1) — финальная реплика с
 * лимитом (R6.2) и счётчиком (R6.6) либо выбор шаблонной концовки текущего
 * языка с немедленным завершением (R6.3, R6.4); видимый хвост на ходе после
 * первого (R6.8); возврат к прерванному ходу (R6.5).
 */
@Composable
fun FinishScreen(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val state: MatchViewModel.UiState by appViewModel.matchViewModel.state.collectAsState()
    val turnIndex: Int = state.match?.turnIndex ?: 0
    val finishingTurn: Boolean = state.match?.phase == TurnPhase.AWAITING_FINISH
    BackHandler(enabled = !finishingTurn) { appViewModel.finishAbandoned() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.finish_title),
            onRulesOpen = appViewModel::rulesOpened,
        )
        if (turnIndex > 0) {
            VisibleContextCard(visibleContext = state.match?.visibleContext ?: "")
        }
        FinalReplySection(appViewModel, state)
        HorizontalDivider()
        TemplateSection(appViewModel, state)
        if (!finishingTurn) {
            TextButton(
                onClick = appViewModel::finishAbandoned,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.rules_back))
            }
        }
    }
}

/** Способ 1 — финальная реплика: поле с лимитом (R6.2), счётчик (R6.6), кнопка (R6.7). */
@Composable
private fun FinalReplySection(appViewModel: AppViewModel, state: MatchViewModel.UiState) {
    Text(
        text = stringResource(R.string.finish_write),
        style = MaterialTheme.typography.titleSmall,
    )
    OutlinedTextField(
        value = state.finishDraft,
        onValueChange = appViewModel.matchViewModel::onFinishDraftChange,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(
            R.string.finish_counter,
            GameRules.MAX_STORY_CHUNK_LENGTH - state.finishDraft.length,
        ),
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        onClick = appViewModel::finishConfirmed,
        enabled = state.finishDraft.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.finish_done))
    }
}

/** Способ 2 — шаблонные концовки текущего языка интерфейса (R6.3); выбор завершает партию (R6.4). */
@Composable
private fun TemplateSection(appViewModel: AppViewModel, state: MatchViewModel.UiState) {
    Text(
        text = stringResource(R.string.finish_template),
        style = MaterialTheme.typography.titleSmall,
    )
    for (template in state.templates) {
        OutlinedButton(
            onClick = { appViewModel.templateConfirmed(template.templateId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = template.text)
        }
    }
}

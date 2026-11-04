package com.yurial.abrakazyak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
 * `screen-chunk` (ui R4): заголовок первого/последующего хода (R4.1), видимый
 * хвост предыдущего участника — только он (R4.2), поле реплики с жёстким
 * лимитом (R4.3), счётчик (R4.4), кнопка `chunk.done` (R4.5) и `chunk.finish` (R4.7).
 */
@Composable
fun ChunkScreen(
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
            text = if (turnIndex == 0) {
                stringResource(R.string.chunk_title_first)
            } else {
                stringResource(R.string.chunk_title_next)
            },
            style = MaterialTheme.typography.titleMedium,
        )
        if (turnIndex > 0) {
            VisibleContextCard(visibleContext = state.match?.visibleContext ?: "")
        }
        OutlinedTextField(
            value = state.chunkDraft,
            onValueChange = appViewModel.matchViewModel::onChunkDraftChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            // Счётчик оставшихся символов (R4.4): лимит минус длина введённого.
            text = stringResource(
                R.string.chunk_counter,
                GameRules.MAX_STORY_CHUNK_LENGTH - state.chunkDraft.length,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = appViewModel::chunkConfirmed,
            enabled = state.chunkDraft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.chunk_done))
        }
        TextButton(
            onClick = appViewModel::finishOpened,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.chunk_finish))
        }
    }
}

/** Видимый контекст хода: `game/visible-tail` предыдущего участника и только он (R4.2, R6.8). */
@Composable
fun VisibleContextCard(visibleContext: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Text(
            text = visibleContext,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(12.dp),
        )
    }
}

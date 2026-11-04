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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yurial.abrakazyak.R
import com.yurial.abrakazyak.core.game.GameRules

/**
 * `screen-rules` (ui R1): описание правил с рекомендацией числа участников
 * (R1.2) и кнопки `rules.play` (R2.1) и `rules.archive` (R1.3).
 */
@Composable
fun RulesScreen(
    onPlay: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.rules_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            // Конвенция подстановки: `recommendedMaxPlayers` — числовой аргумент (ui Interface).
            text = stringResource(R.string.rules_body, GameRules.RECOMMENDED_MAX_PLAYERS),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.rules_play))
        }
        OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.rules_archive))
        }
    }
}

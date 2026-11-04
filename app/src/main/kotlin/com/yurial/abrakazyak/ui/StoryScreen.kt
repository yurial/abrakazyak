package com.yurial.abrakazyak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.yurial.abrakazyak.core.game.FinishedStoryEntry

/**
 * `screen-story` (ui R7): построчный текст готового рассказа (R7.1), авторство
 * только для поименованных участников (R7.2), «Поделиться» (R7.3), «Сыграть ещё»
 * и «Архив историй» (R7.4). Режим архивной записи добавляет возврат в архив
 * (R8.2) и скрывает кнопки новой партии.
 */
@Composable
fun StoryScreen(
    view: AppViewModel.StoryView,
    onShare: (String) -> Unit,
    onPlayAgain: () -> Unit,
    onArchive: () -> Unit,
    onRules: () -> Unit,
    onBackToArchive: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onBackToArchive != null) {
        BackHandler { onBackToArchive() }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.story_title),
            onBack = onBackToArchive,
            onRulesOpen = onRules,
        )
        StoryLines(view.story.entries, Modifier.weight(1f))
        Button(
            onClick = { onShare(view.story.text) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.story_share))
        }
        if (view.mode == AppViewModel.StoryMode.FINISHED_MATCH) {
            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.story_play_again))
            }
            OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.story_archive))
            }
        }
    }
}

/** Построчный рассказ (R7.1): текст строки; под строкой — автор, только если имя задано (R7.2). */
@Composable
private fun StoryLines(entries: List<FinishedStoryEntry>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(entries) { index: Int, entry: FinishedStoryEntry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                Text(text = entry.text, style = MaterialTheme.typography.bodyLarge)
                if (entry.authorName != null) {
                    Text(
                        text = "— ${entry.authorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

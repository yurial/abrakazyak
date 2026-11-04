package com.yurial.abrakazyak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yurial.abrakazyak.R
import com.yurial.abrakazyak.core.storage.ArchiveEntry
import com.yurial.abrakazyak.core.storage.ArchiveEntryId
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * `screen-archive` (ui R8): записи от новых к старым (R8.1), выбор записи
 * открывает чтение (R8.2), возврат к экрану вызова (R8.3), правила (R10.1).
 */
@Composable
fun ArchiveScreen(
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: ArchiveViewModel.UiState by appViewModel.archiveViewModel.state.collectAsState()
    BackHandler { onBack() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.archive_title),
            onBack = onBack,
            onRulesOpen = appViewModel::rulesOpened,
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(state.entries) { entry: ArchiveEntry ->
                ArchiveEntryCard(entry = entry, onSelect = appViewModel::archiveEntrySelected)
            }
        }
    }
}

/** Карточка записи архива: первая строка рассказа и дата и время завершения (storage R2.1). */
@Composable
private fun ArchiveEntryCard(entry: ArchiveEntry, onSelect: (ArchiveEntryId) -> Unit) {
    Card(
        onClick = { onSelect(entry.entryId) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                text = entry.story.entries.firstOrNull()?.text ?: "",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = finishedAtText(entry.finishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Дата и время завершения в системной зоне, формат по локали устройства. */
private fun finishedAtText(finishedAt: java.time.Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(finishedAt)

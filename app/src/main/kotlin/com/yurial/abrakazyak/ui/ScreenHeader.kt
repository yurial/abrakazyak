package com.yurial.abrakazyak.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yurial.abrakazyak.R

/**
 * Заголовок экрана: текст названия, необязательная стрелка возврата и кнопка
 * `rules.open` — вызов правил с игрового экрана (ui R10.1).
 *
 * Стрелка возврата — декоративная (у неё нет текстового эквивалента в каталоге
 * строк спеки), поэтому contentDescription пуст.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRulesOpen: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        }
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (onRulesOpen != null) {
            TextButton(onClick = onRulesOpen) {
                Text(text = stringResource(R.string.rules_open))
            }
        }
    }
}

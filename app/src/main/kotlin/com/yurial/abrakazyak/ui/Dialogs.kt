package com.yurial.abrakazyak.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yurial.abrakazyak.R
import com.yurial.abrakazyak.core.game.GameRules

/**
 * `dialog-resume` (ui R3): модальное предложение продолжить прерванную партию
 * поверх `screen-rules` (R3.3); согласие восстанавливает партию (R3.1), отказ
 * удаляет черновик и открывает `screen-names` (R3.2). Закрытие мимо кнопок
 * запрещено — диалог модален (R3.3).
 */
@Composable
fun ResumeDialog(
    onContinue: () -> Unit,
    onNew: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.resume_title)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(text = stringResource(R.string.resume_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onNew) {
                Text(text = stringResource(R.string.resume_new))
            }
        },
    )
}

/**
 * `dialog-rules` (ui R10): правила игры на текущем языке интерфейса с явным
 * возвратом `rules.back` (R10.2); возврат восстанавливает состояние вызвавшего
 * экрана без потерь (R10.3 — состояние хранится в ViewModel). Диалог модален.
 */
@Composable
fun RulesDialog(onBack: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.rules_title)) },
        text = {
            Text(
                // Конвенция подстановки: `recommendedMaxPlayers` — числовой аргумент (ui Interface).
                text = stringResource(R.string.rules_body, GameRules.RECOMMENDED_MAX_PLAYERS),
            )
        },
        confirmButton = {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.rules_back))
            }
        },
    )
}

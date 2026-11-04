package com.yurial.abrakazyak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Корневой Composable приложения: маршрутизация экранов верхнего уровня
 * по состоянию [AppViewModel] и модальные диалоги поверх активного экрана.
 *
 * Выбор роутинга: state-based маршрутизация вместо navigation-compose —
 * все решения навигации (R1–R10) живут в чистых JVM-ViewModel, покрытых
 * юнит-тестами, без Android-зависимости и лишнего артефакта; дерево экранов
 * приложения линейно, глубокой иерархии переходов нет.
 */
@Composable
fun AbrakazyakApp(
    appViewModel: AppViewModel,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: AppViewModel.UiState by appViewModel.state.collectAsState()
    MaterialTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            ScreenRouter(appViewModel, state, onShare)
            if (state.showResumeDialog) {
                ResumeDialog(
                    onContinue = appViewModel::resumeConfirmed,
                    onNew = appViewModel::resumeDeclined,
                )
            }
            if (state.showRulesDialog) {
                RulesDialog(onBack = appViewModel::rulesBack)
            }
        }
    }
}

/** Переключение экранов верхнего уровня; содержимое MATCH выбирает [MatchViewModel]. */
@Composable
private fun ScreenRouter(
    appViewModel: AppViewModel,
    state: AppViewModel.UiState,
    onShare: (String) -> Unit,
) {
    when (state.screen) {
        AppViewModel.Screen.RULES -> RulesScreen(
            onPlay = appViewModel::playClicked,
            onArchive = { appViewModel.archiveOpenedFrom(AppViewModel.Screen.RULES) },
        )
        AppViewModel.Screen.NAMES -> NamesScreen(appViewModel = appViewModel)
        AppViewModel.Screen.MATCH -> MatchArea(appViewModel)
        AppViewModel.Screen.STORY -> StoryArea(appViewModel, state.story, onShare)
        AppViewModel.Screen.ARCHIVE -> ArchiveScreen(
            appViewModel = appViewModel,
            onBack = appViewModel::archiveBack,
        )
    }
}

/** Экран чтения: только что завершённая партия либо архивная запись (R7, R8.2). */
@Composable
private fun StoryArea(
    appViewModel: AppViewModel,
    story: AppViewModel.StoryView?,
    onShare: (String) -> Unit,
) {
    if (story == null) return
    val archiveMode: Boolean = story.mode == AppViewModel.StoryMode.ARCHIVE_ENTRY
    StoryScreen(
        view = story,
        onShare = onShare,
        onPlayAgain = appViewModel::storyPlayAgain,
        onArchive = appViewModel::storyArchiveClicked,
        onRules = appViewModel::rulesOpened,
        onBackToArchive = if (archiveMode) {
            appViewModel::storyBack
        } else {
            null
        },
    )
}

/** Игровая область (R4–R6): активный экран выбирается по фазе хода и флагу экрана завершения. */
@Composable
private fun MatchArea(appViewModel: AppViewModel) {
    val state: MatchViewModel.UiState by appViewModel.matchViewModel.state.collectAsState()
    when (state.activeScreen) {
        MatchViewModel.Screen.CHUNK -> ChunkScreen(appViewModel)
        MatchViewModel.Screen.TAIL -> TailScreen(appViewModel)
        MatchViewModel.Screen.FINISH -> FinishScreen(appViewModel)
        null -> Unit
    }
}

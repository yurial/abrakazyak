package com.yurial.abrakazyak

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.yurial.abrakazyak.ui.AbrakazyakApp
import com.yurial.abrakazyak.ui.AppViewModel
import com.yurial.abrakazyak.ui.UiLanguages

/**
 * Единственная Activity приложения: собирает [AppContainer] и [AppViewModel],
 * удерживает их при смене конфигурации и передаёт системный «Поделиться» (R7.3).
 * Вся логика — в чистых JVM-ViewModel; Activity — только wiring.
 */
class MainActivity : ComponentActivity() {

    private val retainedModels: RetainedModels by lazy {
        @Suppress("DEPRECATION")
        (lastCustomNonConfigurationInstance as? RetainedModels)
            ?: createRetainedModels()
    }

    @Suppress("DEPRECATION")
    override fun onRetainCustomNonConfigurationInstance(): Any = retainedModels

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Локаль могла измениться при пересоздании Activity — концовки пересчитываются (R9.4).
        retainedModels.appViewModel.updateLanguage(currentLanguageKey())
        setContent {
            MaterialTheme {
                AbrakazyakApp(
                    appViewModel = retainedModels.appViewModel,
                    onShare = ::shareStoryText,
                )
            }
        }
    }

    /** Новый контейнер и корневой ViewModel: вызывается только при первом создании. */
    private fun createRetainedModels(): RetainedModels {
        val container = AppContainer(filesDir)
        val appViewModel = AppViewModel(
            draftStorage = container.draftStorage,
            archiveStorage = container.archiveStorage,
            languageKey = currentLanguageKey(),
        )
        return RetainedModels(container, appViewModel)
    }

    /** Ключ языка интерфейса по системной локали с резервным языком (ui R9.4). */
    private fun currentLanguageKey(): String =
        UiLanguages.resolve(resources.configuration.locales[0].language)

    /** Системный диалог «Поделиться» с полным текстом рассказа (R7.3). */
    private fun shareStoryText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, null))
    }
}

/** Держатель вручную собранных зависимостей, переживает пересоздание Activity. */
private class RetainedModels(
    val container: AppContainer,
    val appViewModel: AppViewModel,
)

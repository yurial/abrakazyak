package com.yurial.abrakazyak

import android.annotation.SuppressLint
import com.yurial.abrakazyak.core.storage.FileMatchDraftStorage
import com.yurial.abrakazyak.core.storage.FileStoryArchiveStorage
import com.yurial.abrakazyak.core.storage.MatchDraftStorage
import com.yurial.abrakazyak.core.storage.StoryArchiveStorage
import java.io.File
import java.nio.file.Path
import java.time.Clock

/**
 * Вручную собранный контейнер зависимостей приложения (без DI-фреймворка):
 * файловые хранилища `:core` в каталоге данных приложения. ViewModel-фабрики
 * получают хранилища отсюда.
 *
 * java.nio.file (File#toPath, Path#resolve) исполняется через core library
 * desugaring — desugar_jdk_libs 2.x поддерживает java.nio.file на minSdk 24;
 * lint помечает вызовы из-за неполноты своей базы desugaring-API, поэтому
 * NewApi подавлен точечно на этом wiring-классе.
 */
@SuppressLint("NewApi")
class AppContainer(dataDirectory: File) {

    private val dataPath: Path = dataDirectory.toPath()

    /** Хранилище черновика партии (storage R1). */
    val draftStorage: MatchDraftStorage by lazy {
        FileMatchDraftStorage(dataPath.resolve("draft"))
    }

    /** Архив завершённых рассказов (storage R2); моменты завершения — системные часы. */
    val archiveStorage: StoryArchiveStorage by lazy {
        FileStoryArchiveStorage(dataPath.resolve("archive"), Clock.systemDefaultZone())
    }
}

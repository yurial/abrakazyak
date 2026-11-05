package com.yurial.abrakazyak

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
 * java.nio.file (File#toPath, Path#resolve) и java.time исполняются на minSdk 24
 * через core library desugaring — артефакт desugar_jdk_libs_nio покрывает
 * java.nio.file; дефолтный desugar_jdk_libs его не покрывает и в релизе v0.1.0
 * приводил к крашу при старте игры (см. DesugaringConformanceTest и TODO.md).
 */
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

package com.yurial.abrakazyak.core.storage

import com.yurial.abrakazyak.core.game.MatchSnapshot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Файловая реализация [MatchDraftStorage]: один файл `draft.json` в каталоге
 * данных, инжектируемом конструктором (`:app` передаёт filesDir в фазе C).
 * Запись выполняется через временный файл с последующей заменой.
 * Повреждённый (нечитаемый) черновик трактуется как отсутствующий (storage R1.5).
 * Контракт по операциям: [saveDraft] и [clearDraft] пробрасывают
 * [java.io.IOException] при сбое файловой системы; [loadDraft] сбоев наружу
 * не отдаёт — любой сбой чтения даёт null (storage R1.5).
 */
class FileMatchDraftStorage(private val dataDirectory: Path) : MatchDraftStorage {

    override fun saveDraft(snapshot: MatchSnapshot) {
        Files.createDirectories(dataDirectory)
        val tempFile: Path = dataDirectory.resolve("$DRAFT_FILE_NAME.tmp")
        Files.writeString(tempFile, Json.encodeToString(snapshot))
        Files.move(tempFile, draftFile(), StandardCopyOption.REPLACE_EXISTING)
    }

    override fun loadDraft(): MatchSnapshot? = try {
        if (Files.exists(draftFile())) {
            Json.decodeFromString<MatchSnapshot>(Files.readString(draftFile()))
        } else {
            null
        }
    } catch (error: IOException) {
        null // сбой чтения трактуется как отсутствие черновика (storage R1.5)
    } catch (error: SerializationException) {
        null // повреждённый черновик трактуется как отсутствующий (storage R1.5)
    }

    override fun clearDraft() {
        Files.deleteIfExists(draftFile())
    }

    private fun draftFile(): Path = dataDirectory.resolve(DRAFT_FILE_NAME)

    private companion object {
        const val DRAFT_FILE_NAME = "draft.json"
    }
}

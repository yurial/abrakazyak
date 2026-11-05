package com.yurial.abrakazyak

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Регрессионный тест краша релиза v0.1.0 («после ввода имён участников кнопка
 * „Начать игру“ падает»). Root cause: файловые хранилища `:core` исполняют
 * java.nio.file, а core library desugaring был подключён дефолтным артефактом
 * `desugar_jdk_libs`, который java.nio.file не покрывает (для этого нужен
 * `desugar_jdk_libs_nio`). Первый же `saveDraft` вызывал `Files.writeString`,
 * отсутствующий на всех уровнях Android API, — `NoSuchMethodError` на
 * устройстве; на API 24–25 приложение падало ещё раньше, на `File#toPath`
 * (java.nio.file появился в платформе только с API 26). На JVM java.nio.file
 * берётся из JDK, поэтому юнит-тесты и Robolectric этот класс причин не ловят.
 *
 * Контракт конформности: каждый java.nio/java.time API, используемый
 * продакшн-кодом `:core` и `:app`, входит в набор, поддерживаемый активной
 * конфигурацией десугаринга (`desugar_jdk_libs_nio`). Поддерживаемый набор
 * читается из lint-базы артефакта `desugar_jdk_libs_configuration_nio` на
 * тестовом classpath — это та же база, по которой lint гасит NewApi.
 * Соответствие wiring-а проверяет guard в `app/build.gradle.kts`.
 */
class DesugaringConformanceTest {

    @Test
    fun everyApiUsedByProductionCodeIsDesugared() {
        // Таблица всех java.nio/java.time API продакшн-кода `:core` и `:app`.
        // Члены-получатели с неопределимым типом (path.resolve, clock.instant,
        // formatter.withZone, instant.toEpochMilli) распознаются только
        // таблицей; статические вызовы и типы дополнительно сканирует второй
        // тест. Новый API сначала вносится сюда, затем используется в коде.
        val usedApis: Map<String, List<String?>> = mapOf(
            // core/storage/FileMatchDraftStorage.kt, FileStoryArchiveStorage.kt
            "java/nio/file/Files" to listOf(
                "writeString", "readString", "createDirectories", "move", "exists", "deleteIfExists",
            ),
            // Path как тип + dataDirectory.resolve(...) в обоих хранилищах и AppContainer
            "java/nio/file/Path" to listOf(null, "resolve"),
            // StandardCopyOption.REPLACE_EXISTING (тип + поле перечисления)
            "java/nio/file/StandardCopyOption" to listOf(null),
            // app/AppContainer.kt: dataDirectory.toPath()
            "java/io/File" to listOf("toPath"),
            // java.time — покрывается любой конфигурацией десугаринга; в таблице
            // для полноты контракта (FileStoryArchiveStorage.kt, AppContainer.kt,
            // ArchiveScreen.kt, ArchiveEntry.kt).
            "java/time/Clock" to listOf(null, "systemDefaultZone", "instant"),
            "java/time/Instant" to listOf(null, "ofEpochMilli", "toEpochMilli"),
            "java/time/ZoneId" to listOf("systemDefault"),
            "java/time/format/DateTimeFormatter" to listOf("ofLocalizedDateTime", "withZone", "format"),
            "java/time/format/FormatStyle" to listOf(null),
        )
        val uncovered: List<String> = usedApis.flatMap { (classBinary: String, members: List<String?>) ->
            members.filter { member: String? -> !isCovered(classBinary, member) }
                .map { member: String? -> "$classBinary#${member ?: "<class>"}" }
        }
        assertTrue(
            uncovered.isEmpty(),
            "API не покрыты активной конфигурацией десугаринга: $uncovered. " +
                "Либо замените API на поддерживаемые, либо проверьте артефакт desugar_jdk_libs_nio.",
        )
    }

    @Test
    fun productionSourcesUseNoUnsupportedJavaNioOrTimeApis() {
        val violations: List<String> = productionKotlinFiles().flatMap(::violationsIn)
        assertTrue(
            violations.isEmpty(),
            "В продакшн-коде есть java.nio/java.time API вне набора активной конфигурации " +
                "десугаринга (они не воспроизводятся JVM-тестами и падают на устройстве): $violations",
        )
    }

    /** Читает lint-базу поддерживаемых API из артефакта конфигурации на classpath. */
    private fun loadSupportedApis(): Set<String> {
        val loader: ClassLoader = javaClass.classLoader
            ?: error("Test classloader is unavailable")
        val stream = loader.getResourceAsStream(SUPPORTED_APIS_RESOURCE)
            ?: error(
                "Lint-база desugared APIs не найдена на тестовом classpath: артефакт " +
                    "desugar_jdk_libs_configuration_nio должен быть подключён как testImplementation " +
                    "синхронно с coreLibraryDesugaring (см. app/build.gradle.kts).",
            )
        return stream.bufferedReader().readLines().filter(String::isNotBlank).toSet()
    }

    /** Класс покрыт целиком (строка класса или любой член), либо покрыт конкретный член. */
    private fun isCovered(classBinary: String, member: String?): Boolean = when (member) {
        null -> supportedApis.any { it == classBinary || it.startsWith("$classBinary#") }
        else -> classBinary in supportedApis || supportedApis.any { it.startsWith("$classBinary#$member(") }
    }

    /** Скан одного файла: импорты, статические вызовы/поля, полностью квалифицированные ссылки. */
    private fun violationsIn(file: File): List<String> {
        val text: String = file.readText()
        val importedClasses: Map<String, String> = importedDesugaredClasses(text)
        val staticCalls: List<String> = Regex("""\b([A-Z]\w*)\.(\w+)""")
            .findAll(text)
            .filter { importedClasses.containsKey(it.groupValues[1]) }
            .map { match ->
                val classBinary: String = importedClasses.getValue(match.groupValues[1])
                "$classBinary#${match.groupValues[2]}" to isCovered(classBinary, match.groupValues[2])
            }
            .filter { (_, covered: Boolean) -> !covered }
            .map { (api: String, _) -> "${file.name}: $api" }
            .toList()
        val qualifiedRefs: List<String> = Regex("""\bjava\.(?:nio|time)(?:\.\w+)+""")
            .findAll(text)
            .map { match -> match.value to qualifiedRefCovered(match.value) }
            .filter { (_, covered: Boolean) -> !covered }
            .map { (ref: String, _) -> "${file.name}: $ref" }
            .toList()
        return staticCalls + qualifiedRefs
    }

    /** Импортированные java.nio/java.time классы файла: простое имя → бинарное имя класса. */
    private fun importedDesugaredClasses(text: String): Map<String, String> =
        Regex("""^import\s+(java\.(?:nio|time)[\w.]*)$""", RegexOption.MULTILINE)
            .findAll(text)
            .associate { match: MatchResult ->
                val fqn: String = match.groupValues[1]
                fqn.substringAfterLast('.') to fqn.replace('.', '/')
            }

    /** Покрытие полностью квалифицированной ссылки: тип либо статический член класса. */
    private fun qualifiedRefCovered(reference: String): Boolean {
        val segments: List<String> = reference.split('.')
        val (classFqn: String, member: String?) =
            if (segments.last().first().isLowerCase()) {
                segments.dropLast(1).joinToString(".") to segments.last()
            } else {
                reference to null
            }
        // Последний сегмент строчный — имя пакета (упоминание в комментарии),
        // а не класс: API-использованием не является.
        if (classFqn.substringAfterLast('.').first().isLowerCase()) return true
        return isCovered(classFqn.replace('.', '/'), member)
    }

    /** Продакшн-исходники :app и :core; пути поддерживают запуск из каталога модуля и из корня. */
    private fun productionKotlinFiles(): List<File> {
        val appRoot: File = firstExisting(
            "Продакшн-исходники :app не найдены",
            File("src/main/kotlin"),
            File("app/src/main/kotlin"),
        )
        val coreRoot: File = firstExisting(
            "Продакшн-исходники :core не найдены",
            File("../core/src/main/kotlin"),
            File("core/src/main/kotlin"),
        )
        return listOf(appRoot, coreRoot).flatMap { root: File ->
            root.walkTopDown().filter(File::isFile).filter { it.extension == "kt" }.toList()
        }
    }

    /** Первый существующий каталог из перечисленных; отсутствие всех — ошибка с сообщением. */
    private fun firstExisting(message: String, vararg candidates: File): File {
        val found: File = candidates.firstOrNull(File::exists)
            ?: error("$message: ищите ${candidates.joinToString()}; проверьте рабочий каталог теста")
        return found
    }

    private val supportedApis: Set<String> by lazy { loadSupportedApis() }

    private companion object {
        /** Lint-база поддерживаемых API внутри артефакта конфигурации десугаринга. */
        const val SUPPORTED_APIS_RESOURCE =
            "META-INF/desugar/d8/lint/compile_api_level_30/desugared_apis_30_1.txt"
    }
}

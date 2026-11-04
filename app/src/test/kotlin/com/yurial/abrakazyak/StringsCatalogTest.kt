package com.yurial.abrakazyak

import kotlin.test.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Каталог строк интерфейса — plain JVM-тест, читающий res/values/strings.xml
 * (русский, по умолчанию) и res/values-en/strings.xml как файлы: parity ключей
 * (ui R9.2), полнота относительно таблицы Interface specs/ui.md, дословность
 * русского текста и конвенция подстановки (ui Interface).
 */
class StringsCatalogTest {

    private val defaultCatalog: Map<String, String> = readCatalog("values")
    private val englishCatalog: Map<String, String> = readCatalog("values-en")

    @Test
    fun keySetsAreIdenticalAcrossLanguages() {
        // ui R9.2: множества ключей наборов ui/string-resource всех языков совпадают.
        kotlin.test.assertEquals(defaultCatalog.keys, englishCatalog.keys)
    }

    @Test
    fun allSpecTableKeysArePresent() {
        val specKeys: Set<String> = setOf(
            "rules.title", "rules.body", "rules.play", "rules.open", "rules.back",
            "rules.archive", "names.title", "names.hint", "names.add", "names.rounds",
            "names.start", "chunk.title.first", "chunk.title.next", "chunk.counter",
            "chunk.done", "chunk.finish", "tail.title", "tail.counter", "tail.done",
            "tail.finish", "finish.title", "finish.write", "finish.template",
            "finish.counter", "finish.done", "story.title", "story.share",
            "story.play-again", "story.archive", "archive.title", "resume.title",
            "resume.continue", "resume.new",
        )
        // Ключи спеки отображаются на имена ресурсов заменой точки подчёркиванием
        // (rules.title -> rules_title); дефис в story.play-again -> story_play_again.
        fun resKey(specKey: String): String = specKey.replace('.', '_').replace('-', '_')
        kotlin.test.assertTrue(
            specKeys.all { resKey(it) in defaultCatalog },
            "missing RU keys: ${specKeys.filterNot { resKey(it) in defaultCatalog }}",
        )
        kotlin.test.assertTrue(
            specKeys.all { resKey(it) in englishCatalog },
            "missing EN keys: ${specKeys.filterNot { resKey(it) in englishCatalog }}",
        )
    }

    @Test
    fun russianValuesMatchSpecVerbatim() {
        // Русский текст — дословно из таблицы Interface specs/ui.md; фрагменты в
        // обратных кавычках (`recommendedMaxPlayers`, счётчик N) — формат %1$d.
        val expected: Map<String, String> = mapOf(
            "rules.title" to "Правила игры",
            "rules.body" to "Передавайте телефон по кругу. Каждый игрок продолжает общую историю, видя только маленький кусочек предыдущей фразы, и оставляет кусочек для следующего. Рекомендуем играть компанией до %1\$d человек.",
            "rules.play" to "Играть",
            "rules.open" to "Правила",
            "rules.back" to "Вернуться к игре",
            "rules.archive" to "Архив историй",
            "names.title" to "Участники",
            "names.hint" to "Имена по желанию: с ними при чтении видно авторство",
            "names.add" to "Добавить участника",
            "names.rounds" to "Число кругов",
            "names.start" to "Начать игру",
            "chunk.title.first" to "Начни свою историю! То, что ты сейчас пишешь, видишь только ты",
            "chunk.title.next" to "Продолжи историю",
            "chunk.counter" to "Осталось символов: %1\$d",
            "chunk.done" to "Готово",
            "chunk.finish" to "Завершить историю",
            "tail.title" to "Напиши маленький кусочек, который продолжит следующий участник",
            "tail.counter" to "Осталось символов: %1\$d",
            "tail.done" to "Готово, передаю телефон другу",
            "tail.finish" to "Завершить историю",
            "finish.title" to "Как завершим историю?",
            "finish.write" to "Написать финальную реплику",
            "finish.template" to "Выбрать готовую концовку",
            "finish.counter" to "Осталось символов: %1\$d",
            "finish.done" to "Завершить",
            "story.title" to "Ваш рассказ",
            "story.share" to "Поделиться",
            "story.play-again" to "Сыграть ещё",
            "story.archive" to "Архив историй",
            "archive.title" to "Архив историй",
            "resume.title" to "Продолжить прерванную игру?",
            "resume.continue" to "Продолжить",
            "resume.new" to "Начать новую",
        )
        val mismatches: Map<String, Pair<String?, String>> = expected.mapValues { (key, value) ->
            defaultCatalog[key.replace('.', '_').replace('-', '_')] to value
        }.filter { it.value.first != it.value.second }
        kotlin.test.assertEquals(emptyMap(), mismatches.mapValues { it.value.first to it.value.second })
    }

    @Test
    fun englishValuesArePresent() {
        val specKeys: Set<String> = defaultCatalog.keys - "app_name"
        kotlin.test.assertTrue(specKeys.all { englishCatalog[it]?.isNotBlank() == true })
    }

    @Test
    fun substitutionConventionUsesFormatArguments() {
        fun resKey(specKey: String): String = specKey.replace('.', '_').replace('-', '_')
        for (catalog in listOf(defaultCatalog, englishCatalog)) {
            for (key in listOf("rules.body", "chunk.counter", "tail.counter", "finish.counter")) {
                kotlin.test.assertTrue(
                    catalog[resKey(key)]?.contains("%1\$d") == true,
                    "$key must contain a %1\$d format argument",
                )
            }
            for ((key, value) in catalog) {
                if (key !in listOf("rules_body", "chunk_counter", "tail_counter", "finish_counter")) {
                    kotlin.test.assertTrue(!value.contains('%'), "$key must not contain stray format specifiers")
                }
            }
        }
    }

    @Test
    fun placeholderStringsAreRemoved() {
        kotlin.test.assertTrue(defaultCatalog.keys.none { it.startsWith("placeholder") })
        kotlin.test.assertTrue(englishCatalog.keys.none { it.startsWith("placeholder") })
    }

    /** Читает strings.xml каталога [valuesDir] в словарь «ключ — текст». */
    private fun readCatalog(valuesDir: String): Map<String, String> {
        val file: File = resDirectory().resolve(valuesDir).resolve("strings.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val result: MutableMap<String, String> = mutableMapOf()
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            result[element.getAttribute("name")] = element.textContent
        }
        return result
    }

    /** Каталог ресурсов модуля :app: тест-раннер исполняется из каталога модуля. */
    private fun resDirectory(): File {
        var dir: File = File(System.getProperty("user.dir")).absoluteFile
        repeat(4) {
            val candidate: File = dir.resolve("app/src/main/res")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile ?: dir
        }
        error("app/src/main/res not found from ${System.getProperty("user.dir")}")
    }
}

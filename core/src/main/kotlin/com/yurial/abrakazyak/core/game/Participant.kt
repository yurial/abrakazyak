package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Участник партии (game/participant, R1.3): выполняет ходы в порядке очереди.
 * Имя необязательно ([name] равно null); отсутствие имён у части или всех
 * участников не влияет на выполнение ходов (R1.3, T1.2). Ограничения на имя
 * спецификация не задаёт.
 */
@Serializable
data class Participant(val name: String?)

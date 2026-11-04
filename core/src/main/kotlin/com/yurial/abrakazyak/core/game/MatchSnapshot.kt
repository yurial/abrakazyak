package com.yurial.abrakazyak.core.game

import kotlinx.serialization.Serializable

/**
 * Снимок состояния партии для сохранения (storage R1.2): список и порядок
 * участников с именами, число кругов, текущий ход, видимый контекст текущего
 * хода ([visibleContext]) и все написанные тексты ([lines]).
 * Набранный, но не подтверждённый текст шага в снимок не входит (storage R1.6):
 * снимок строится только из подтверждённого состояния [Match].
 *
 * Формат данных сериализации — деталь хранилища; восстановление —
 * [Match.fromSnapshot].
 */
@Serializable
data class MatchSnapshot(
    val participants: List<Participant>,
    val roundCount: Int,
    val turnIndex: Int,
    val phase: TurnPhase,
    val lines: List<StoryLine>,
    val visibleContext: String?,
)

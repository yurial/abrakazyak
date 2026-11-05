package com.yurial.abrakazyak.core.game

/**
 * Партия «Abrakazyak» (game/match): участники, порядок ходов, подтверждённые
 * тексты и завершение — по specs/game.md.
 *
 * Операции соответствуют Interface specs/game.md: [startMatch], [submitChunk],
 * [submitTail], [finishWithReply], [finishWithTemplate], свойство [visibleContext],
 * [currentTurnChunk], [finishedStory]; списки шаблонных концовок —
 * [GameRules.templateEndings].
 *
 * Каждая операция возвращает новое неизменяемое состояние [Match]; состояние
 * после ошибки не изменяется (Error handling). Возвращённое последним состояние
 * является единственным актуальным: операции с ранее снятым состоянием
 * отклоняются кодом game/invalid-step (R3.6) — окно текущего хода уже закрыто.
 * Класс не потокобезопасен: партия офлайн, устройство передаётся по кругу
 * (Usage constraints specs/game.md).
 */
class Match internal constructor(
    internal val identity: MatchIdentity,
    val participants: List<Participant>,
    val roundCount: Int,
    val turnIndex: Int,
    val phase: TurnPhase,
    internal val lines: List<StoryLine>,
    internal val lastTail: String?,
) {

    /** Всего ходов партии: произведение числа участников на число кругов (R1.9, R1.10). */
    val totalTurns: Int
        get() = roundCount * participants.size

    /** Партия завершена (R3.2 или R3.3): обычные операции недоступны. */
    val isFinished: Boolean
        get() = phase == TurnPhase.FINISHED

    /** Подтверждённые реплики ходов в порядке написания (R2.1, R1.4). */
    internal val chunks: List<StoryChunk>
        get() = lines.filterIsInstance<StoryLine.Chunk>().map { line: StoryLine.Chunk -> line.chunk }

    /**
     * Участник, чей ход текущий; null после завершения партии.
     * Порядок ходов — по кругу: за последним участником следует первый (R1.5).
     */
    val currentParticipant: Participant?
        get() = if (phase == TurnPhase.FINISHED) null else participants[turnIndex % participants.size]

    /**
     * Видимый контекст текущего хода (game/visible-context, R2.2, R2.3):
     * пуст (null) на первом ходе; далее — только видимый хвост предыдущего
     * участника, остальные тексты партии не раскрываются. После завершения
     * партии контекста нет (null).
     */
    val visibleContext: String?
        get() = if (phase == TurnPhase.FINISHED) null else lastTail

    /**
     * Подтверждённая реплика текущего хода — контекст экрана ввода хвоста
     * (ui R5.8): текст последней подтверждённой реплики, пока ход находится
     * на шаге хвоста (фаза AWAITING_TAIL). Вне этого шага возвращается null:
     * до подтверждения реплики (AWAITING_CHUNK — в том числе на первом ходе),
     * на ходе завершения (AWAITING_FINISH) и после завершения партии (FINISHED).
     * На шаге хвоста реплика гарантированно существует: фаза достигается
     * только успешным [submitChunk].
     */
    fun currentTurnChunk(): String? {
        if (phase != TurnPhase.AWAITING_TAIL) return null
        return chunks.last().text
    }

    /**
     * Фиксирует реплику текущего хода (R2.1) после валидации текста (R2.4, R2.5).
     * Допустима только на шаге реплики; отклоняется кодами game/invalid-step,
     * game/match-finished, game/empty-text, game/text-too-long.
     */
    fun submitChunk(text: String): GameResult<Match> {
        val windowError: GameError? = windowErrorFor(TurnPhase.AWAITING_CHUNK)
        if (windowError != null) return GameResult.Failure(windowError)
        val textError: GameError? = textErrorFor(text, GameRules.MAX_STORY_CHUNK_LENGTH)
        if (textError != null) return GameResult.Failure(textError)
        identity.phase = TurnPhase.AWAITING_TAIL
        val next: Match = Match(
            identity,
            participants,
            roundCount,
            turnIndex,
            TurnPhase.AWAITING_TAIL,
            lines + StoryLine.Chunk(StoryChunk(text, turnIndex % participants.size)),
            lastTail,
        )
        return GameResult.Success(next)
    }

    /**
     * Фиксирует видимый хвост текущего хода (R2.1) после валидации текста
     * (R2.6, R2.7) и передаёт ход следующему участнику (R1.5). Хвост входит
     * в список написанных строк партии в порядке написания (R4.1) и
     * предшествует реплике, которую он начинает (R4.2).
     */
    fun submitTail(text: String): GameResult<Match> {
        val windowError: GameError? = windowErrorFor(TurnPhase.AWAITING_TAIL)
        if (windowError != null) return GameResult.Failure(windowError)
        val textError: GameError? = textErrorFor(text, GameRules.MAX_VISIBLE_TAIL_LENGTH)
        if (textError != null) return GameResult.Failure(textError)
        val nextTurnIndex: Int = turnIndex + 1
        val nextPhase: TurnPhase = phaseForTurn(nextTurnIndex)
        identity.turnIndex = nextTurnIndex
        identity.phase = nextPhase
        val next: Match = Match(
            identity,
            participants,
            roundCount,
            nextTurnIndex,
            nextPhase,
            lines + StoryLine.Tail(text, turnIndex % participants.size),
            text,
        )
        return GameResult.Success(next)
    }

    /**
     * Завершает партию финальной репликой (R3.1, R3.2) после валидации текста
     * по правилам реплики (R2.4, R2.5). Окно валидности — текущий ход до его
     * [submitTail] (R3.6): до [submitChunk] заменяет ход целиком (R3.7), после
     * [submitChunk] сохраняет подтверждённую реплику и отбрасывает шаг хвоста (R3.8).
     */
    fun finishWithReply(text: String): GameResult<Match> = finishWithEnding(text)

    /**
     * Завершает партию выбором шаблонной концовки (R3.3); окно валидности
     * совпадает с [finishWithReply] (R3.6). [templateId] — идентификатор из
     * [GameRules.templateEndings]; неизвестный идентификатор отклоняется
     * кодом game/unknown-template.
     */
    fun finishWithTemplate(templateId: String): GameResult<Match> {
        val windowError: GameError? = finishWindowError()
        if (windowError != null) return GameResult.Failure(windowError)
        val template: TemplateEnding = GameRules.findTemplateEnding(templateId)
            ?: return GameResult.Failure(GameError.UnknownTemplate)
        return finishWithEnding(template.text)
    }

    /**
     * Готовый рассказ (R4.1–R4.3) или null, пока партия не завершена.
     */
    fun finishedStory(): FinishedStory? {
        if (phase != TurnPhase.FINISHED) return null
        val endingAuthorIndex: Int = turnIndex % participants.size
        val entries: List<FinishedStoryEntry> = lines.map { line: StoryLine ->
            when (line) {
                is StoryLine.Chunk -> {
                    val chunk: StoryChunk = line.chunk
                    FinishedStoryEntry(chunk.text, chunk.authorIndex, participants[chunk.authorIndex].name)
                }
                is StoryLine.Tail -> FinishedStoryEntry(line.text, line.authorIndex, participants[line.authorIndex].name)
                is StoryLine.Ending -> FinishedStoryEntry(line.text, endingAuthorIndex, participants[endingAuthorIndex].name)
            }
        }
        return FinishedStory(entries)
    }

    /**
     * Снимок подтверждённого состояния для хранилища (storage R1.2, R1.6).
     */
    fun snapshot(): MatchSnapshot = MatchSnapshot(
        participants = participants,
        roundCount = roundCount,
        turnIndex = turnIndex,
        phase = phase,
        lines = lines,
        visibleContext = visibleContext,
    )

    override fun toString(): String =
        "Match(turnIndex=$turnIndex, phase=$phase, rounds=$roundCount, participants=${participants.size})"

    /** Общие проверки окна завершения: партия жива и снимок актуален (R3.6). */
    private fun finishWindowError(): GameError? {
        if (identity.phase == TurnPhase.FINISHED) return GameError.MatchFinished
        return staleStateError()
    }

    /** Проверки окна обычного шага: актуальность снимка и ожидаемая фаза хода. */
    private fun windowErrorFor(expectedPhase: TurnPhase): GameError? {
        val commonError: GameError? = finishWindowError()
        if (commonError != null) return commonError
        if (phase != expectedPhase) return GameError.InvalidStep
        return null
    }

    /**
     * Операция вне окна текущего хода (game/invalid-step, R3.6): снимок устарел —
     * ход уже передан дальше (R1.5) либо внутри хода подтверждён другой шаг.
     */
    private fun staleStateError(): GameError? {
        if (identity.turnIndex != turnIndex || identity.phase != phase) return GameError.InvalidStep
        return null
    }

    /** Валидация текста (Error handling): пустота проверяется до длины (R2.4–R2.7). */
    private fun textErrorFor(text: String, maxLength: Int): GameError? {
        if (text.isBlank()) return GameError.EmptyText
        if (text.length > maxLength) return GameError.TextTooLong
        return null
    }

    /** Фаза хода с индексом [turn]: последний ход партии — ход завершения (R1.10). */
    private fun phaseForTurn(turn: Int): TurnPhase =
        if (turn == totalTurns - 1) TurnPhase.AWAITING_FINISH else TurnPhase.AWAITING_CHUNK

    /** Общая ветка завершения: фиксирует концовку и закрывает партию (R3.2, R3.3). */
    private fun finishWithEnding(endingText: String): GameResult<Match> {
        val windowError: GameError? = finishWindowError()
        if (windowError != null) return GameResult.Failure(windowError)
        val textError: GameError? = textErrorFor(endingText, GameRules.MAX_STORY_CHUNK_LENGTH)
        if (textError != null) return GameResult.Failure(textError)
        identity.phase = TurnPhase.FINISHED
        val next: Match = Match(
            identity,
            participants,
            roundCount,
            turnIndex,
            TurnPhase.FINISHED,
            lines + StoryLine.Ending(endingText),
            lastTail,
        )
        return GameResult.Success(next)
    }

    companion object {

        /**
         * Восстанавливает партию из снимка хранилища (storage R1.2): восстановленное
         * состояние продолжает партию с последнего подтверждённого шага.
         * Снимок доверяется структурно — он создаётся [Match.snapshot].
         */
        fun fromSnapshot(snapshot: MatchSnapshot): Match {
            val identity = MatchIdentity(snapshot.turnIndex, snapshot.phase)
            return Match(
                identity,
                snapshot.participants,
                snapshot.roundCount,
                snapshot.turnIndex,
                snapshot.phase,
                snapshot.lines,
                snapshot.visibleContext,
            )
        }
    }
}

/**
 * `startMatch` (Interface specs/game.md): создаёт партию; [participants] —
 * непустой упорядоченный список участников (R1.1, R1.5, R1.6); [roundCount] —
 * необязательное число кругов не менее единицы (R1.7), при null используется
 * [GameRules.defaultRoundCount] (R1.8). Отклоняется кодами
 * game/empty-participants и game/invalid-round-count; проверка пустоты списка
 * выполняется до проверки числа кругов.
 *
 * Нецелые значения числа кругов в типизированном API непредставимы
 * (параметр `Int?`); отклоняется только значение менее единицы (R1.7).
 */
fun startMatch(participants: List<Participant>, roundCount: Int? = null): GameResult<Match> {
    if (participants.isEmpty()) return GameResult.Failure(GameError.EmptyParticipants)
    val effectiveRoundCount: Int = roundCount ?: GameRules.defaultRoundCount(participants.size)
    if (effectiveRoundCount < 1) return GameResult.Failure(GameError.InvalidRoundCount)
    val identity = MatchIdentity(0, initialPhase(participants.size, effectiveRoundCount))
    val match: Match = Match(
        identity,
        participants.toList(),
        effectiveRoundCount,
        0,
        identity.phase,
        emptyList(),
        null,
    )
    return GameResult.Success(match)
}

/** Фаза первого хода: партия из одного круга с одним участником стартует ходом завершения (R1.10). */
private fun initialPhase(participantCount: Int, rounds: Int): TurnPhase =
    if (participantCount * rounds == 1) TurnPhase.AWAITING_FINISH else TurnPhase.AWAITING_CHUNK

/**
 * Служебная неизменяемая позиция партии в общей последовательности операций:
 * операции сравнивают её с [Match.identity] и отклоняют устаревшие состояния
 * (game/invalid-step, R3.6).
 */
internal class MatchIdentity(var turnIndex: Int, var phase: TurnPhase)

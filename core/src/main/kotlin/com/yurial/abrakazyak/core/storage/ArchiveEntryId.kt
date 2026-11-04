package com.yurial.abrakazyak.core.storage

/**
 * Идентификатор записи архива (storage/archive-entry); возвращается
 * [StoryArchiveStorage.addFinishedStory] и принимается операциями чтения.
 */
@JvmInline
value class ArchiveEntryId(val value: String)

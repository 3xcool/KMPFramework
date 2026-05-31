package com.tekmoon.session

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Persisted draft store.
 *
 * Normally backed by a local database or repository layer.
 * The session does not care how it is stored.
 */
interface DraftStore<Model : Any> {
    fun observe(sessionId: String): Flow<Model?>
    suspend fun save(sessionId: String, value: Model)
    suspend fun clear(sessionId: String)
}

/**
 * Persisted ephemeral state store.
 *
 * This is where small UI/session restoration state lives:
 * selected tab, pager page, expanded section, etc.
 */
interface SavedStateStore {
    fun observe(sessionId: String): Flow<Map<String, String>>
    suspend fun update(
        sessionId: String,
        reducer: (Map<String, String>) -> Map<String, String>,
    )
    suspend fun clear(sessionId: String)
}

/**
 * Implementation backed by [SavedStateHandle].
 *
 * Good for:
 * - process death restoration
 * - small serializable state
 *
 * Do not store large objects here.
 */
class DataSavedStateHandleStore(
    private val savedStateHandle: SavedStateHandle,
    private val keyPrefix: String = "session.saved.state",
) : SavedStateStore {

    private fun storageKey(sessionId: String): String = "$keyPrefix:$sessionId"

    override fun observe(sessionId: String): Flow<Map<String, String>> {
        return savedStateHandle
            .getStateFlow<Map<String, String>>(storageKey(sessionId), emptyMap())
            .map { it.toMap() }
    }

    override suspend fun update(
        sessionId: String,
        reducer: (Map<String, String>) -> Map<String, String>,
    ) {
        val key = storageKey(sessionId)
        val current = savedStateHandle.get<Map<String, String>>(key).orEmpty()
        savedStateHandle[key] = LinkedHashMap(reducer(current))
    }

    override suspend fun clear(sessionId: String) {
        savedStateHandle.remove<Map<String, String>>(storageKey(sessionId))
    }
}

/**
 * Useful for tests or simple prototypes.
 */
class InMemoryDraftStore<Model : Any> : DraftStore<Model> {
    private val store = MutableStateFlow<Map<String, Model>>(emptyMap())

    override fun observe(sessionId: String): Flow<Model?> {
        return store.map { it[sessionId] }
    }

    override suspend fun save(sessionId: String, value: Model) {
        store.update { current ->
            current + (sessionId to value)
        }
    }

    override suspend fun clear(sessionId: String) {
        store.update { current ->
            current - sessionId
        }
    }
}

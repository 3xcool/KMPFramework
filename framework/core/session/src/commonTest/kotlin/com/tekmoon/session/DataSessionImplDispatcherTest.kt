package com.tekmoon.session

import com.tekmoon.utilities.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Verifies that every blocking store/source call inside [DataSessionImpl] is
 * dispatched on [DispatcherProvider.io], not on the session scope's
 * `mainImmediate` dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataSessionImplDispatcherTest {

    @Test
    fun updateDraft_runs_draftStore_save_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.updateDraft { "$it-edited" }
        advanceUntilIdle()

        assertSame(env.io, env.draftStore.lastSaveInterceptor)
    }

    @Test
    fun updateInitialAndDraft_runs_draftStore_save_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.updateInitialAndDraft { initial, draft -> initial to "$draft-edited" }
        advanceUntilIdle()

        assertSame(env.io, env.draftStore.lastSaveInterceptor)
    }

    @Test
    fun commit_model_runs_draftStore_save_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.commit("committed")
        advanceUntilIdle()

        assertSame(env.io, env.draftStore.lastSaveInterceptor)
    }

    @Test
    fun commit_reducer_runs_draftStore_save_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.commit { "$it-committed" }
        advanceUntilIdle()

        assertSame(env.io, env.draftStore.lastSaveInterceptor)
    }

    @Test
    fun syncRemoteChange_runs_saveLocal_and_draftStore_save_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.syncRemoteChange { initial, draft -> "$initial-synced" to "$draft-synced" }
        advanceUntilIdle()

        assertSame(env.io, env.source.lastSaveLocalInterceptor)
        assertSame(env.io, env.draftStore.lastSaveInterceptor)
    }

    @Test
    fun discardDraft_runs_draftStore_clear_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.discardDraft()
        advanceUntilIdle()

        assertSame(env.io, env.draftStore.lastClearInterceptor)
    }

    @Test
    fun updateSavedState_runs_savedStateStore_update_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.updateSavedState(stringKey) { "$it-updated" }
        advanceUntilIdle()

        assertSame(env.io, env.savedStateStore.lastUpdateInterceptor)
    }

    @Test
    fun clearSession_runs_savedStateStore_clear_on_io() = runTest {
        val env = newEnv(this)
        env.session.attach(env.source)
        advanceUntilIdle()

        env.session.clearSession(clearSavedState = true)

        assertSame(env.io, env.savedStateStore.lastClearInterceptor)
    }
}

private val stringKey = SimpleSessionValueKey(
    key = "k",
    serializer = String.serializer(),
    defaultValue = "default",
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun newEnv(testScope: TestScope): Env {
    val scheduler = testScope.testScheduler
    val io = UnconfinedTestDispatcher(scheduler, "io")
    val mainImmediate = UnconfinedTestDispatcher(scheduler, "mainImmediate")
    val main = UnconfinedTestDispatcher(scheduler, "main")
    val default = UnconfinedTestDispatcher(scheduler, "default")
    val dispatchers = TestDispatcherProvider(main, mainImmediate, io, default)
    val draftStore = RecordingDraftStore<String>()
    val savedStateStore = RecordingSavedStateStore()
    val source = RecordingSource()
    val session = DataSessionImpl(
        draftStore = draftStore,
        savedStateStore = savedStateStore,
        json = Json,
        dispatchers = dispatchers,
        scope = CoroutineScope(SupervisorJob() + mainImmediate),
    )
    return Env(io, draftStore, savedStateStore, source, session)
}

private class Env(
    val io: CoroutineDispatcher,
    val draftStore: RecordingDraftStore<String>,
    val savedStateStore: RecordingSavedStateStore,
    val source: RecordingSource,
    val session: DataSessionImpl<String>,
)

// region Fakes

private class TestDispatcherProvider(
    override val main: CoroutineDispatcher,
    override val mainImmediate: CoroutineDispatcher,
    override val io: CoroutineDispatcher,
    override val default: CoroutineDispatcher,
) : DispatcherProvider

private class RecordingDraftStore<Model : Any> : DraftStore<Model> {
    private val store = MutableStateFlow<Map<String, Model>>(emptyMap())

    var lastSaveInterceptor: ContinuationInterceptor? = null
        private set
    var lastClearInterceptor: ContinuationInterceptor? = null
        private set

    override fun observe(sessionId: String): Flow<Model?> = store.map { it[sessionId] }

    override suspend fun save(sessionId: String, value: Model) {
        lastSaveInterceptor = coroutineContext[ContinuationInterceptor]
        store.update { it + (sessionId to value) }
    }

    override suspend fun clear(sessionId: String) {
        lastClearInterceptor = coroutineContext[ContinuationInterceptor]
        store.update { it - sessionId }
    }
}

private class RecordingSavedStateStore : SavedStateStore {
    private val store = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())

    var lastUpdateInterceptor: ContinuationInterceptor? = null
        private set
    var lastClearInterceptor: ContinuationInterceptor? = null
        private set

    override fun observe(sessionId: String): Flow<Map<String, String>> =
        store.map { it[sessionId].orEmpty() }

    override suspend fun update(
        sessionId: String,
        reducer: (Map<String, String>) -> Map<String, String>,
    ) {
        lastUpdateInterceptor = coroutineContext[ContinuationInterceptor]
        store.update { current ->
            current + (sessionId to reducer(current[sessionId].orEmpty()))
        }
    }

    override suspend fun clear(sessionId: String) {
        lastClearInterceptor = coroutineContext[ContinuationInterceptor]
        store.update { it - sessionId }
    }
}

private class RecordingSource(
    override val sessionId: String = "session-1",
    override val entityId: String = "entity-1",
    override val loadPolicy: DataSessionLoadPolicy = DataSessionLoadPolicy.LocalOnly,
) : DataSessionSource<String> {

    var lastSaveLocalInterceptor: ContinuationInterceptor? = null
        private set

    private val local = MutableStateFlow<String?>("model-v1")
    private val remote = MutableStateFlow<String?>(null)

    override fun localFlow(): Flow<String?> = local
    override fun remoteFlow(): Flow<String?> = remote

    override fun resolveConflict(
        local: String,
        remote: String,
    ): Pair<String, DataSessionResolvedSource> = local to DataSessionResolvedSource.Local

    override fun hasChanges(initial: String, current: String): Boolean = initial != current

    override suspend fun saveLocal(model: String) {
        lastSaveLocalInterceptor = coroutineContext[ContinuationInterceptor]
        local.value = model
    }
}

// endregion

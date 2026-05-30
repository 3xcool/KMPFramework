package com.tekmoon.session

import com.tekmoon.utilities.DispatcherProvider
import com.tekmoon.utilities.StandardDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Default [DataSession] implementation.
 *
 * Combines local, remote, draft, and saved-state sources into a single reactive
 * session state, while handling refresh requests, conflict resolution, and
 * draft persistence.
 *
 * Callers should provide a [scope] tied to the host lifecycle (e.g. `viewModelScope`)
 * so the session is cancelled when the host is destroyed.
 */
class DataSessionImpl<Model : Any>(
    private val draftStore: DraftStore<Model>,
    private val savedStateStore: SavedStateStore,
    private val json: Json,
    private val dispatchers: DispatcherProvider = StandardDispatchers,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.mainImmediate),
) : DataSession<Model> {

    private val _state = MutableStateFlow(DataSessionState<Model>())
    override val state: StateFlow<DataSessionState<Model>> = _state

    private val mutationMutex = Mutex()

    private var currentSource: DataSessionSource<Model>? = null
    private var syncJob: Job? = null
    private var refreshRequests = Channel<RefreshRequest>(Channel.CONFLATED)

    private var attachJob: Job? = null

    override fun attach(source: DataSessionSource<Model>) {
        val current = _state.value
        val sameSession =
            current.isAttached &&
                current.sessionId == source.sessionId &&
                current.entityId == source.entityId

        if (sameSession) return

        currentSource = source

        attachJob?.cancel()
        attachJob = scope.launch {
            syncJob?.cancelAndJoin()
            if (isActive) startSync(source)
        }
    }

    override fun refresh(policy: DataSessionLoadPolicy?) {
        val source = currentSource ?: return
        refreshRequests.trySend(RefreshRequest(policy ?: source.loadPolicy))
    }

    override fun updateDraft(reducer: (Model) -> Model) {
        scope.launch {
            mutationMutex.withLock {
                val sessionId = _state.value.sessionId ?: return@withLock
                val current = _state.value.resolved ?: return@withLock
                val updatedDraft = reducer(current)
                withContext(dispatchers.io) {
                    draftStore.save(sessionId, updatedDraft)
                }
            }
        }
    }

    override fun updateInitialAndDraft(
        reducer: (initial: Model, draft: Model) -> Pair<Model, Model>,
    ) {
        scope.launch {
            mutationMutex.withLock {
                val current = _state.value
                val sessionId = current.sessionId ?: return@withLock
                val initial = current.initial ?: return@withLock
                val draft = current.resolved ?: return@withLock

                val (updatedInitial, updatedDraft) = reducer(initial, draft)

                _state.update { it.copy(initial = updatedInitial) }
                withContext(dispatchers.io) {
                    draftStore.save(sessionId, updatedDraft)
                }
            }
        }
    }

    override fun syncRemoteChange(
        reducer: (initial: Model, draft: Model) -> Pair<Model, Model>,
    ) {
        scope.launch {
            mutationMutex.withLock {
                val source = currentSource ?: return@withLock
                val current = _state.value
                val sessionId = current.sessionId ?: return@withLock
                val initial = current.initial ?: return@withLock
                val draft = current.resolved ?: return@withLock

                val (updatedInitial, updatedDraft) = reducer(initial, draft)

                _state.update {
                    it.copy(
                        initial = updatedInitial,
                        hasChanges = source.hasChanges(updatedInitial, updatedDraft),
                    )
                }
                withContext(dispatchers.io) {
                    source.saveLocal(updatedInitial) // triggers localFlow -> sync reruns
                    draftStore.save(sessionId, updatedDraft) // triggers draftFlow -> sync reruns
                }
            }
        }
    }

    override fun commit(reducer: (Model) -> Model) {
        scope.launch {
            mutationMutex.withLock {
                val sessionId = _state.value.sessionId ?: return@withLock
                val resolved = _state.value.resolved ?: return@withLock
                val updatedResolved = reducer(resolved)
                _state.update { it.copy(initial = updatedResolved, draft = updatedResolved) }
                withContext(dispatchers.io) {
                    draftStore.save(sessionId, updatedResolved)
                }
            }
        }
    }

    override fun commit(model: Model) {
        scope.launch {
            mutationMutex.withLock {
                val sessionId = _state.value.sessionId ?: return@withLock
                _state.update { it.copy(initial = model, draft = model) }
                withContext(dispatchers.io) {
                    draftStore.save(sessionId, model)
                }
            }
        }
    }

    override fun discardDraft() {
        scope.launch {
            mutationMutex.withLock {
                val sessionId = _state.value.sessionId ?: return@withLock
                withContext(dispatchers.io) {
                    draftStore.clear(sessionId)
                }
            }
        }
    }

    override suspend fun clearSession(clearSavedState: Boolean) {
        syncJob?.cancelAndJoin()
        syncJob = null
        mutationMutex.withLock {
            val sessionId = _state.value.sessionId
            currentSource = null

            refreshRequests.close()
            refreshRequests = Channel(Channel.CONFLATED)

            if (clearSavedState && sessionId != null) {
                withContext(dispatchers.io) {
                    savedStateStore.clear(sessionId)
                }
            }

            _state.value = DataSessionState()
        }
    }

    override fun <T : Any> savedStateFlow(key: DataSessionValueKey<T>): Flow<T> {
        return state.map { session ->
            session.savedState[key.key]
                ?.let { json.decodeFromString(key.serializer, it) }
                ?: key.defaultValue
        }
    }

    override fun <T : Any> updateSavedState(
        key: DataSessionValueKey<T>,
        reducer: (T) -> T,
    ) {
        scope.launch {
            mutationMutex.withLock {
                val sessionId = _state.value.sessionId ?: return@withLock

                withContext(dispatchers.io) {
                    savedStateStore.update(sessionId) { current ->
                        val previous = current[key.key]
                            ?.let { json.decodeFromString(key.serializer, it) }
                            ?: key.defaultValue

                        val updated = reducer(previous)

                        current + (key.key to json.encodeToString(key.serializer, updated))
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startSync(source: DataSessionSource<Model>) {
        syncJob?.cancel()

        refreshRequests.close()
        refreshRequests = Channel(Channel.CONFLATED)

        _state.update {
            it.copy(
                sessionId = source.sessionId,
                entityId = source.entityId,
                isAttached = true,
                isRefreshing = shouldUseRemote(source.loadPolicy),
            )
        }

        val draftFlow = draftStore.observe(source.sessionId)
            .distinctUntilChanged()
            .onStart { emit(null) }

        val localFlow = localFlow(source, source.loadPolicy)
            .distinctUntilChanged()
            .onStart { emit(null) }

        val savedStateFlow = savedStateStore.observe(source.sessionId)
            .distinctUntilChanged()
            .onStart { emit(emptyMap()) }

        val remoteFlow = refreshRequests
            .receiveAsFlow()
            .onStart { emit(RefreshRequest(source.loadPolicy)) }
            .flatMapLatest { request ->
                remoteValueFlow(source, request.policy)
                    .map { RemoteSnapshot(value = it, isRefreshing = false) }
                    .onStart { emit(RemoteSnapshot(value = null, isRefreshing = true)) }
            }
            .distinctUntilChanged()

        syncJob = combine(
            draftFlow,
            localFlow,
            remoteFlow,
            savedStateFlow,
        ) { draft, local, remoteSnapshot, savedState ->
            val remote = remoteSnapshot.value

            val conflictResult by lazy {
                if (local != null && remote != null) source.resolveConflict(local, remote) else null
            }

            val (resolved, resolvedSource) = when {
                draft != null -> draft to DataSessionResolvedSource.Draft
                local != null && remote == null -> local to DataSessionResolvedSource.Local
                local == null && remote != null -> remote to DataSessionResolvedSource.Remote
                conflictResult != null -> conflictResult!!
                else -> null to DataSessionResolvedSource.None
            }

            val initial = source.createInitialBaseline(
                currentInitial = _state.value.initial,
                local = local,
                remote = remote,
                resolved = resolved,
            )

            val hasChanges = initial != null && resolved != null && source.hasChanges(initial, resolved)

            val correctedSource = if (resolvedSource == DataSessionResolvedSource.Draft && !hasChanges) {
                conflictResult?.second ?: when {
                    local != null -> DataSessionResolvedSource.Local
                    remote != null -> DataSessionResolvedSource.Remote
                    else -> DataSessionResolvedSource.None
                }
            } else resolvedSource

            DataSessionState(
                sessionId = source.sessionId,
                entityId = source.entityId,
                isAttached = true,
                initial = initial,
                resolved = resolved,
                resolvedSource = correctedSource,
                draft = draft,
                local = local,
                remote = remote,
                savedState = savedState,
                hasLoadedDraft = draft != null,
                hasLoadedLocal = local != null,
                hasLoadedRemote = remote != null,
                hasChanges = hasChanges,
                isRefreshing = remoteSnapshot.isRefreshing,
                isInitialized = resolved != null || initial != null,
            )
        }
            .distinctUntilChanged()
            .onEach { _state.value = it }
            .launchIn(scope)
    }

    private fun localFlow(
        source: DataSessionSource<Model>,
        policy: DataSessionLoadPolicy,
    ): Flow<Model?> {
        return when (policy) {
            DataSessionLoadPolicy.LocalThenRemote,
            DataSessionLoadPolicy.RemoteThenLocal,
            DataSessionLoadPolicy.LocalOnly -> source.localFlow()
            DataSessionLoadPolicy.RemoteOnly,
            DataSessionLoadPolicy.SessionOnly -> flowOf(null)
        }
    }

    private fun remoteValueFlow(
        source: DataSessionSource<Model>,
        policy: DataSessionLoadPolicy,
    ): Flow<Model?> {
        return when (policy) {
            DataSessionLoadPolicy.LocalThenRemote,
            DataSessionLoadPolicy.RemoteThenLocal,
            DataSessionLoadPolicy.RemoteOnly -> source.remoteFlow()
            DataSessionLoadPolicy.LocalOnly,
            DataSessionLoadPolicy.SessionOnly -> flowOf(null)
        }
    }

    private fun shouldUseRemote(policy: DataSessionLoadPolicy): Boolean {
        return when (policy) {
            DataSessionLoadPolicy.LocalThenRemote,
            DataSessionLoadPolicy.RemoteThenLocal,
            DataSessionLoadPolicy.RemoteOnly -> true
            DataSessionLoadPolicy.LocalOnly,
            DataSessionLoadPolicy.SessionOnly -> false
        }
    }

    private data class RefreshRequest(
        val policy: DataSessionLoadPolicy,
    )

    private data class RemoteSnapshot<Model : Any>(
        val value: Model?,
        val isRefreshing: Boolean,
    )
}

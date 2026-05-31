package com.tekmoon.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Used as a mock for Previews and tests.
 */
class FakeDataSession<Model : Any>(
    initialState: DataSessionState<Model> = DataSessionState(),
) : DataSession<Model> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<DataSessionState<Model>> = _state

    private val savedState = MutableStateFlow<Map<String, Any>>(emptyMap())

    fun setState(state: DataSessionState<Model>) {
        _state.value = state
    }

    fun setResolved(model: Model) {
        _state.update { it.copy(resolved = model, initial = it.initial ?: model) }
    }

    // Fake session: attach/refresh are no-ops; tests drive state directly via
    // setLoading / setResolved / setError.
    @Suppress("EmptyFunctionBlock")
    override fun attach(source: DataSessionSource<Model>) {}

    @Suppress("EmptyFunctionBlock")
    override fun refresh(policy: DataSessionLoadPolicy?) {}

    override fun updateDraft(reducer: (Model) -> Model) {
        val current = _state.value.resolved ?: return
        _state.update { it.copy(resolved = reducer(current)) }
    }

    override fun updateInitialAndDraft(reducer: (Model, Model) -> Pair<Model, Model>) {
        val initial = _state.value.initial ?: return
        val draft = _state.value.resolved ?: return
        val (newInitial, newDraft) = reducer(initial, draft)
        _state.update { it.copy(initial = newInitial, resolved = newDraft) }
    }

    override fun commit(model: Model) {
        _state.update { it.copy(initial = model, resolved = model) }
    }

    override fun commit(reducer: (Model) -> Model) {
        val resolved = _state.value.resolved ?: return
        val newResolved = reducer(resolved)
        _state.update { it.copy(initial = newResolved, resolved = newResolved) }
    }

    override fun syncRemoteChange(reducer: (Model, Model) -> Pair<Model, Model>) {
        val initial = _state.value.initial ?: return
        val draft = _state.value.resolved ?: return
        val (newInitial, newDraft) = reducer(initial, draft)
        _state.update { it.copy(initial = newInitial, resolved = newDraft) }
    }

    override fun discardDraft() {
        _state.update { it.copy(resolved = it.initial) }
    }

    override suspend fun clearSession(clearSavedState: Boolean) {
        _state.value = DataSessionState()
        if (clearSavedState) savedState.value = emptyMap()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> savedStateFlow(key: DataSessionValueKey<T>): Flow<T> {
        return savedState.map {
            (it[key.key] as? T) ?: key.defaultValue
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> updateSavedState(key: DataSessionValueKey<T>, reducer: (T) -> T) {
        val current = (savedState.value[key.key] as? T) ?: key.defaultValue
        savedState.update { it + (key.key to reducer(current)) }
    }
}

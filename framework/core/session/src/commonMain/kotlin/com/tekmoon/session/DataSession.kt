package com.tekmoon.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the lifecycle of a stateful editing session for a single entity.
 *
 * A [DataSession] coordinates multiple data sources involved in an editing flow:
 * - the persisted local value
 * - the remote value
 * - a draft value containing in-progress user edits
 * - feature-specific saved state that must survive recreation
 *
 * The session exposes a unified [state] that resolves those sources into the
 * current model shown by the UI, while also tracking metadata such as
 * initialization, refresh state, and whether the user has unsaved changes.
 *
 * Typical flow:
 * 1. Attach the session to a [DataSessionSource].
 * 2. Observe [state] from the UI.
 * 3. Call [updateDraft] as the user edits fields.
 * 4. Call [updateInitialAndDraft] when you need to update both initial and draft without losing their differences.
 * 5. Call [commit] when the new value should become the new baseline.
 * 6. Call [discardDraft] to abandon pending edits.
 * 7. Call [clearSession] when the feature is finished with the session.
 *
 * This abstraction is intended for feature flows where:
 * - data may come from local and/or remote sources
 * - the user may edit data before saving
 * - draft and saved UI state must survive configuration changes or process death
 */
interface DataSession<Model : Any> {

    /**
     * Current state of the session, including resolved data, source metadata,
     * refresh state, and draft/saved-state information.
     */
    val state: StateFlow<DataSessionState<Model>>

    /**
     * Attaches this session to the given [source].
     *
     * Once attached, the session starts observing local, remote, draft, and saved-state
     * sources for the provided entity/session identifiers.
     *
     * Re-attaching the same session/entity pair is ignored.
     */
    fun attach(source: DataSessionSource<Model>)

    /**
     * Requests a refresh of the attached source.
     *
     * @param policy Optional override for the load policy used during this refresh.
     * If null, the source default policy is used.
     */
    fun refresh(policy: DataSessionLoadPolicy? = null)

    /**
     * Updates the current draft by applying [reducer] to the currently resolved model.
     *
     * Use this for user edits that should be treated as unsaved changes.
     */
    fun updateDraft(reducer: (Model) -> Model)

    /**
     * Updates both the frozen baseline (`initial`) and the draft in one operation.
     *
     * Useful when a feature needs to apply a transformation that should affect both
     * the comparison baseline and the current editable model at the same time.
     */
    fun updateInitialAndDraft(
        reducer: (initial: Model, draft: Model) -> Pair<Model, Model>,
    )

    /**
     * Commits the provided [model] as the new session baseline and current draft.
     */
    fun commit(model: Model)

    /**
     * Commits a transformed version of the currently resolved model as the new
     * session baseline and current draft.
     */
    fun commit(reducer: (Model) -> Model)

    /**
     * Called when a remote operation succeeded and the result needs to be reflected
     * in both the local store and the draft.
     */
    fun syncRemoteChange(reducer: (initial: Model, draft: Model) -> Pair<Model, Model>)

    /**
     * Clears any persisted draft for the current session, effectively discarding
     * unsaved user changes.
     */
    fun discardDraft()

    /**
     * Clears the current session and stops synchronization.
     *
     * @param clearSavedState When true, feature-specific persisted saved state is
     * also removed in addition to the session state itself.
     */
    suspend fun clearSession(clearSavedState: Boolean = false)

    /**
     * Returns a [Flow] of a saved value associated with [key].
     *
     * If no value was previously stored, the key default value is emitted.
     */
    fun <T : Any> savedStateFlow(key: DataSessionValueKey<T>): Flow<T>

    /**
     * Updates the saved value associated with [key] by applying [reducer]
     * to the current value.
     */
    fun <T : Any> updateSavedState(
        key: DataSessionValueKey<T>,
        reducer: (T) -> T,
    )
}

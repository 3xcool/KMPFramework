package com.tekmoon.session

import kotlinx.coroutines.flow.Flow

/**
 * Feature-provided source contract.
 *
 * [DataSession] stays generic and does not know anything about repositories,
 * database schemas, or network layers.
 */
interface DataSessionSource<Model : Any> {
    /**
     * Stable session identifier.
     * Usually restored by the host screen from SavedStateHandle.
     */
    val sessionId: String

    /**
     * Stable entity identifier.
     * Example: botId, roomId, draftId, profileId.
     */
    val entityId: String

    /**
     * Initial attach policy.
     */
    val loadPolicy: DataSessionLoadPolicy

    /**
     * Local source for the entity.
     */
    fun localFlow(): Flow<Model?>

    /**
     * Remote source for the entity.
     */
    fun remoteFlow(): Flow<Model?>

    /**
     * Lets the feature implementation decide how the baseline is created.
     * Default keeps the first stable non-null value.
     */
    fun createInitialBaseline(
        currentInitial: Model?,
        local: Model?,
        remote: Model?,
        resolved: Model?,
    ): Model? = currentInitial ?: local ?: remote ?: resolved

    /**
     * Called when both local and remote are available and differ.
     * Default: remote wins (remote is source of truth).
     * Override to implement merge logic (e.g., preserve optimistic fields).
     */
    fun resolveConflict(
        local: Model,
        remote: Model,
    ): Pair<Model, DataSessionResolvedSource>

    /**
     * Domain-specific change detection.
     */
    fun hasChanges(initial: Model, current: Model): Boolean

    /**
     * Persists [model] to the local store after a successful remote operation.
     * Default is no-op for sources that don't need local persistence.
     */
    suspend fun saveLocal(model: Model) {}
}

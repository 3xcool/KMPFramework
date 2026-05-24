package com.tekmoon.session

data class DataSessionState<Model : Any>(
    val sessionId: String? = null,
    val entityId: String? = null,
    val isAttached: Boolean = false,
    val initial: Model? = null,
    val resolved: Model? = null,
    val resolvedSource: DataSessionResolvedSource = DataSessionResolvedSource.None,
    val draft: Model? = null,
    val local: Model? = null,
    val remote: Model? = null,
    val savedState: Map<String, String> = emptyMap(),
    val hasLoadedDraft: Boolean = false,
    val hasLoadedLocal: Boolean = false,
    val hasLoadedRemote: Boolean = false,
    val hasChanges: Boolean = false,
    val isRefreshing: Boolean = false,
    val isInitialized: Boolean = false,
)

enum class DataSessionResolvedSource {
    None,
    Draft,
    Local,
    Remote,
}

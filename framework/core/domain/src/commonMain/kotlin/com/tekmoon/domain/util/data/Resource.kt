package com.tekmoon.domain.util.data

/**
 * Represents the full lifecycle of a data-loading operation as observed by the UI.
 *
 * Repositories return `Flow<Resource<T>>`; ViewModels collect it into a `StateFlow`
 * and expose it to the UI with no further mapping needed.
 *
 * ### State machine
 * ```
 *  Idle ──► Loading ──► Success
 *                   └──► Error(data = null)          ← no prior data
 *                   └──► Error(data = staleData)      ← refresh failed
 *  Success ──► Paginating(data) ──► Success(data++)
 *                               └──► Error(data = current) ← pagination failed
 * ```
 *
 * ### Usage
 * ```kotlin
 * // Repository
 * fun getPosts(): Flow<Resource<List<Post>>> = flow {
 *     emit(Resource.Loading)
 *     emit(when (val r = remote.fetchPage(1, 20)) {
 *         is Result.Success -> Resource.Success(r.data)
 *         is Result.Failure -> Resource.Error(r.error)
 *     })
 * }
 *
 * // ViewModel
 * val posts = repository.getPosts()
 *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Idle)
 *
 * // UI (Compose)
 * when (val s = posts.collectAsStateWithLifecycle().value) {
 *     Resource.Idle         -> { }
 *     Resource.Loading      -> CircularProgressIndicator()
 *     is Resource.Success   -> LazyColumn { items(s.data) { PostRow(it) } }
 *     is Resource.Paginating -> {
 *         LazyColumn { items(s.data) { PostRow(it) } }
 *         LinearProgressIndicator()
 *     }
 *     is Resource.Error     -> ErrorView(s.errorType, s.message)
 * }
 * ```
 */
sealed interface Resource<out T> {

    /** Initial state — no load has been attempted yet. */
    data object Idle : Resource<Nothing>

    /** First load is in flight; no data is available yet. */
    data object Loading : Resource<Nothing>

    /**
     * Data is available and up-to-date.
     *
     * @param data The loaded value.
     */
    data class Success<out T>(val data: T) : Resource<T>

    /**
     * Data is available AND a subsequent page / refresh is in flight.
     *
     * The UI should render [data] while showing a secondary loading indicator.
     *
     * @param data The current (possibly partial) data while more is loading.
     */
    data class Paginating<out T>(val data: T) : Resource<T>

    /**
     * The last operation failed.
     *
     * @param errorType  Structured error — can be a framework type ([ClientError],
     *                   [ServerError], [NoInternet], …) or an app-defined [DataError].
     * @param message    Optional human-readable message from the server or a
     *                   pre-resolved app string. Null when the error handler
     *                   decides the ViewModel / UI should provide its own copy.
     * @param data       Stale data from the previous successful load, if any.
     *                   Present when a refresh or pagination call fails so the UI
     *                   can keep displaying existing content instead of going blank.
     */
    data class Error<out T>(
        val errorType: DataError,
        val message: String? = null,
        val data: T? = null,
    ) : Resource<T>
}

// ─── Convenience extensions ───────────────────────────────────────────────────

/** Returns the data if this is [Resource.Success] or [Resource.Paginating], otherwise null. */
val <T> Resource<T>.dataOrNull: T?
    get() = when (this) {
        is Resource.Success    -> data
        is Resource.Paginating -> data
        is Resource.Error      -> data
        else                   -> null
    }

/** `true` while any load is in progress ([Loading] or [Paginating]). */
val Resource<*>.isLoading: Boolean
    get() = this is Resource.Loading || this is Resource.Paginating

/** `true` when the resource is in an error state. */
val Resource<*>.isError: Boolean
    get() = this is Resource.Error

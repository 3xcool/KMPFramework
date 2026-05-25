package com.tekmoon.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tekmoon.logger.Loggers
import com.tekmoon.logger.ShowMeLoggerK
import com.tekmoon.utilities.DispatcherProvider
import com.tekmoon.utilities.StandardDispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base class for framework ViewModels using a simple MVI-ish contract:
 *
 * - [state]   : `StateFlow<State>` backed by `MutableStateFlow(initialState())`
 * - [uiEvents]: `SharedFlow<Event>` for one-off effects
 * - [onAction]: entry point for UI actions
 * - [setup]   : suspend hook run once when [state] begins being collected
 *
 * Threading is provided via [DispatcherProvider] (defaults to [StandardDispatchers]) so tests
 * can swap in a test dispatcher. Logging goes through [ShowMeLoggerK] (no direct logger imports).
 */
abstract class CommonViewModel<Action : Any, Event : Any, State : Any>(
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    replayEvents: Int = 0,
    extraBufferCapacity: Int = 64,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
    protected val dispatchers: DispatcherProvider = StandardDispatchers,
    protected val logger: ShowMeLoggerK? = Loggers.current,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { ctx, t ->
        val name = ctx[CoroutineName]?.name ?: "no-name"
        logger?.e("Coroutine error [$name] -> ${t.stackTraceToString()}")
    }

    protected val scope: CoroutineScope = viewModelScope
        .plus(exceptionHandler)
        .plus(CoroutineName(this::class.simpleName ?: "ViewModel"))

    protected inline fun launch(
        name: String? = null,
        crossinline block: suspend CoroutineScope.() -> Unit,
    ) = scope.launch(if (name != null) CoroutineName(name) else EmptyCoroutineContext) {
        block()
    }

    protected inline fun launchIO(
        name: String? = null,
        crossinline block: suspend () -> Unit,
    ) = scope.launch(
        dispatchers.io + (if (name != null) CoroutineName(name) else EmptyCoroutineContext)
    ) {
        block()
    }

    private val _state by lazy { MutableStateFlow(initialState()) }
    val state: StateFlow<State> by lazy {
        _state
            .onStart { setup() }
            .stateIn(scope, sharingStarted, _state.value)
    }

    private val _uiEvents = MutableSharedFlow<Event>(
        replay = replayEvents,
        extraBufferCapacity = extraBufferCapacity,
        onBufferOverflow = onBufferOverflow,
    )
    val uiEvents: SharedFlow<Event> = _uiEvents.asSharedFlow()

    protected abstract fun initialState(): State
    protected open suspend fun setup() {}
    abstract fun onAction(action: Action)

    protected fun emit(event: Event) {
        scope.launch { _uiEvents.emit(event) }
    }

    protected fun updateState(reducer: (State) -> State) {
        _state.update { reducer(it) }
    }

    protected fun withState(block: (State) -> Unit) = block(_state.value)

    protected fun <T> withCatching(
        block: () -> T,
    ) {
        try {
            block()
        } catch (throwable: Throwable) {
            logger?.e("Exception in ${this::class.simpleName} -> ${throwable.stackTraceToString()}")
        }
    }
}

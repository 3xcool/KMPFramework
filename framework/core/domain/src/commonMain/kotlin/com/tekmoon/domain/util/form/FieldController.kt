package com.tekmoon.domain.util.form

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages the state of a single form field.
 *
 * Instantiate in a ViewModel (using `viewModelScope`) or in Compose
 * via `rememberFieldController(...)`.
 *
 * ```kotlin
 * // ViewModel
 * val email = FieldController(
 *     initialValue = "",
 *     validators    = listOf(Validators.required() and Validators.email()),
 *     trigger       = ValidationTrigger.OnBlur,
 *     scope         = viewModelScope,
 * )
 * ```
 *
 * @param initialValue   Starting value for the field.
 * @param validators     Synchronous validators run in order; first failure wins.
 * @param asyncValidators Async validators run after all sync validators pass.
 * @param trigger        When to run validation automatically.
 * @param scope          Coroutine scope for async validation jobs.
 */
class FieldController<T>(
    initialValue: T,
    private val validators: List<Validator<T>> = emptyList(),
    private val asyncValidators: List<AsyncValidator<T>> = emptyList(),
    val trigger: ValidationTrigger = ValidationTrigger.OnChange,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(FieldState(value = initialValue))
    val state: StateFlow<FieldState<T>> = _state.asStateFlow()

    private var asyncJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** Called whenever the field value changes (e.g. on every keystroke). */
    fun onChange(value: T) {
        val wasBlurred = _state.value.isTouched
        _state.update { it.copy(value = value) }
        when (trigger) {
            ValidationTrigger.OnChange                            -> runValidation()
            ValidationTrigger.OnChangeAfterBlur if (wasBlurred)  -> runValidation()
            else                                                  -> Unit
        }
    }

    /** Called when the field loses focus. */
    fun onBlur() {
        _state.update { it.copy(isTouched = true) }
        when (trigger) {
            ValidationTrigger.OnBlur,
            ValidationTrigger.OnChangeAfterBlur -> runValidation()
            else                                -> Unit
        }
    }

    /**
     * Runs all validators immediately regardless of [trigger].
     * Also marks the field as touched.
     *
     * @return `true` when the field is valid after sync validation
     *         (async validation may still be in flight).
     */
    fun validate(): Boolean {
        _state.update { it.copy(isTouched = true) }
        return runValidation()
    }

    /** Resets the field to [initialValue] and clears all errors and touched state. */
    fun reset(value: T) {
        asyncJob?.cancel()
        _state.value = FieldState(value = value)
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Runs sync validators then, if all pass, kicks off async validators.
     * @return `true` when all sync validators pass (regardless of async status).
     */
    private fun runValidation(): Boolean {
        val value = _state.value.value
        val syncError = validators.firstNotNullOfOrNull { v ->
            v.validate(value).errorOrNull
        }
        _state.update { it.copy(error = syncError, isValidating = false) }

        if (syncError == null && asyncValidators.isNotEmpty()) {
            runAsyncValidation(value)
        }
        return syncError == null
    }

    private fun runAsyncValidation(value: T) {
        asyncJob?.cancel()
        _state.update { it.copy(isValidating = true) }
        asyncJob = scope.launch {
            val asyncError = asyncValidators.firstNotNullOfOrNull { v ->
                v.validate(value).errorOrNull
            }
            _state.update { it.copy(error = asyncError, isValidating = false) }
        }
    }
}

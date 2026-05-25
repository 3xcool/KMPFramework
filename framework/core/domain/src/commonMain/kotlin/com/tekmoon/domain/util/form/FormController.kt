package com.tekmoon.domain.util.form

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aggregates multiple [FieldController]s into a single form.
 *
 * ```kotlin
 * class LoginViewModel : CommonViewModel<...>() {
 *
 *     val email    = FieldController("", listOf(Validators.required() and Validators.email()), scope = viewModelScope)
 *     val password = FieldController("", listOf(Validators.required(), Validators.minLength(8)), scope = viewModelScope)
 *
 *     private val form = FormController(email, password)
 *
 *     fun onSubmit() {
 *         if (!form.validateAll()) return
 *         viewModelScope.launch {
 *             form.whileSubmitting {
 *                 repository.login(email.state.value.value, password.state.value.value)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param fields All [FieldController]s that belong to this form.
 */
class FormController(
    private vararg val fields: FieldController<*>,
) {
    private val _isSubmitting = MutableStateFlow(false)

    /** `true` while a submit action is running inside [whileSubmitting]. */
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    /**
     * `true` when every field is currently valid (no errors, no async validation in flight).
     *
     * Note: fields that have not been validated yet are considered valid here.
     * Call [validateAll] first to force a full check.
     */
    val isValid: Boolean
        get() = fields.all { it.state.value.isValid }

    /**
     * Runs [FieldController.validate] on every field and returns `true` when all pass.
     *
     * Use this before submitting to surface errors on untouched fields.
     */
    fun validateAll(): Boolean = fields.map { it.validate() }.all { it }

    /**
     * Sets [isSubmitting] to `true`, runs [block], then resets it to `false`.
     *
     * ```kotlin
     * fun onSubmit() {
     *     if (!form.validateAll()) return
     *     viewModelScope.launch {
     *         form.whileSubmitting { repository.login(...) }
     *     }
     * }
     * ```
     */
    suspend fun whileSubmitting(block: suspend () -> Unit) {
        _isSubmitting.value = true
        try {
            block()
        } finally {
            _isSubmitting.value = false
        }
    }

    /** Resets all fields to their given values and clears submit state. */
    fun reset(vararg values: Pair<FieldController<*>, Any?>) {
        @Suppress("UNCHECKED_CAST")
        values.forEach { (controller, value) ->
            (controller as FieldController<Any?>).reset(value)
        }
        _isSubmitting.value = false
    }
}

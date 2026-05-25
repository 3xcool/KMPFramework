package com.tekmoon.domain.util.form

/**
 * Immutable snapshot of a single form field's state.
 *
 * Owned and updated by [FieldController]; consumed by the UI layer.
 *
 * @param value        Current typed value of the field.
 * @param error        Active [ValidationError], or `null` when valid.
 * @param isTouched    `true` after the user has interacted with the field at least once.
 * @param isValidating `true` while an [AsyncValidator] is running.
 */
data class FieldState<out T>(
    val value: T,
    val error: ValidationError? = null,
    val isTouched: Boolean = false,
    val isValidating: Boolean = false,
) {
    /**
     * `true` when there is no error and no async validation is in progress.
     * Does NOT require the field to have been touched.
     */
    val isValid: Boolean get() = error == null && !isValidating

    /**
     * `true` when an error should be surfaced to the user.
     * Suppressed until the field has been touched so the UI does not
     * show errors on a pristine form.
     */
    val showError: Boolean get() = error != null && isTouched
}

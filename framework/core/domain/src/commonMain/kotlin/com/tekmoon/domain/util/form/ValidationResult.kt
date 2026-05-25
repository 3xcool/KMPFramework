package com.tekmoon.domain.util.form

/** Result of a single [Validator] run. */
sealed interface ValidationResult {
    /** The value passed all validators. */
    data object Valid : ValidationResult

    /** The value failed a validator. */
    data class Invalid(val error: ValidationError) : ValidationResult
}

/** `true` when this is [ValidationResult.Valid]. */
val ValidationResult.isValid: Boolean
    get() = this is ValidationResult.Valid

/** The [ValidationError] if [Invalid], otherwise `null`. */
val ValidationResult.errorOrNull: ValidationError?
    get() = (this as? ValidationResult.Invalid)?.error

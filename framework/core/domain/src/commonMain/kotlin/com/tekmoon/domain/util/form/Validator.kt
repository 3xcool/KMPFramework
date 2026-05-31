package com.tekmoon.domain.util.form

/**
 * Synchronous validator for a value of type [T].
 *
 * ```kotlin
 * val notBlank = Validator<String> { value ->
 *     if (value.isBlank()) ValidationResult.Invalid(BuiltInValidationError.Required)
 *     else ValidationResult.Valid
 * }
 * ```
 */
fun interface Validator<in T> {
    fun validate(value: T): ValidationResult
}

/**
 * Asynchronous validator — use for server-side checks (e.g. username availability).
 *
 * ```kotlin
 * val usernameAvailable = AsyncValidator<String> { username ->
 *     val taken = api.isUsernameTaken(username)
 *     if (taken) ValidationResult.Invalid(BuiltInValidationError.Custom("Username already taken"))
 *     else ValidationResult.Valid
 * }
 * ```
 */
fun interface AsyncValidator<in T> {
    suspend fun validate(value: T): ValidationResult
}

/**
 * Chains two validators — [other] is only evaluated when [this] returns [ValidationResult.Valid].
 *
 * ```kotlin
 * val emailField = Validators.required() and Validators.email()
 * ```
 */
infix fun <T> Validator<T>.and(other: Validator<T>): Validator<T> = Validator { value ->
    when (val r = validate(value)) {
        is ValidationResult.Invalid -> r
        ValidationResult.Valid      -> other.validate(value)
    }
}

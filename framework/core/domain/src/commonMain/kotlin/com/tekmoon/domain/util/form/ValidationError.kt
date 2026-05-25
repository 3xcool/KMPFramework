package com.tekmoon.domain.util.form

/**
 * Typed validation failure.
 *
 * Domain-level — no string resources or Compose dependencies.
 * The presentation layer maps these to [UiText] via `ValidationError.toUiText()`.
 *
 * Apps can define their own errors by implementing this interface:
 * ```kotlin
 * sealed interface MyError : ValidationError {
 *     data object PasswordMismatch : MyError
 *     data class ServerRejected(val reason: String) : MyError
 * }
 * ```
 */
interface ValidationError

/** Built-in framework errors. */
sealed interface BuiltInValidationError : ValidationError {
    /** Field was blank / null when a value was required. */
    data object Required : BuiltInValidationError

    /** String is shorter than [min] characters. */
    data class TooShort(val min: Int) : BuiltInValidationError

    /** String is longer than [max] characters. */
    data class TooLong(val max: Int) : BuiltInValidationError

    /** String is not a valid e-mail address. */
    data object InvalidEmail : BuiltInValidationError

    /** String does not match the expected pattern. [hint] is an optional human-readable description. */
    data class InvalidPattern(val hint: String? = null) : BuiltInValidationError

    /** A pre-resolved error message — use for server-side validation responses. */
    data class Custom(val message: String) : BuiltInValidationError
}

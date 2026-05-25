package com.tekmoon.domain.util.form

/**
 * Built-in [Validator] factory functions.
 *
 * Combine with [and] to build multi-rule validators:
 * ```kotlin
 * val emailRule  = Validators.required() and Validators.email()
 * val nameRule   = Validators.required() and Validators.minLength(2) and Validators.maxLength(50)
 * ```
 */
object Validators {

    // ── Presence ──────────────────────────────────────────────────────────────

    /**
     * Fails when the [String] value is blank (empty or whitespace-only).
     */
    fun required(): Validator<String> = Validator { value ->
        if (value.isBlank()) ValidationResult.Invalid(BuiltInValidationError.Required)
        else ValidationResult.Valid
    }

    /**
     * Fails when the nullable value is `null`.
     */
    fun <T> requiredNotNull(): Validator<T?> = Validator { value ->
        if (value == null) ValidationResult.Invalid(BuiltInValidationError.Required)
        else ValidationResult.Valid
    }

    // ── Length ────────────────────────────────────────────────────────────────

    /**
     * Fails when the string has fewer than [min] characters (after trimming).
     */
    fun minLength(min: Int): Validator<String> = Validator { value ->
        if (value.trim().length < min) ValidationResult.Invalid(BuiltInValidationError.TooShort(min))
        else ValidationResult.Valid
    }

    /**
     * Fails when the string exceeds [max] characters.
     */
    fun maxLength(max: Int): Validator<String> = Validator { value ->
        if (value.length > max) ValidationResult.Invalid(BuiltInValidationError.TooLong(max))
        else ValidationResult.Valid
    }

    // ── Format ────────────────────────────────────────────────────────────────

    /**
     * Fails when the string is not a syntactically valid e-mail address.
     *
     * Uses a standard RFC 5322-simplified regex — does NOT verify deliverability.
     */
    fun email(): Validator<String> = Validator { value ->
        if (!EMAIL_REGEX.matches(value.trim()))
            ValidationResult.Invalid(BuiltInValidationError.InvalidEmail)
        else ValidationResult.Valid
    }

    /**
     * Fails when the string does not fully match [regex].
     *
     * @param hint Optional human-readable description shown in the error (e.g. "only letters allowed").
     */
    fun pattern(regex: Regex, hint: String? = null): Validator<String> = Validator { value ->
        if (!regex.matches(value))
            ValidationResult.Invalid(BuiltInValidationError.InvalidPattern(hint))
        else ValidationResult.Valid
    }

    // ── Numeric ───────────────────────────────────────────────────────────────

    /**
     * Fails when the [Int] value is less than [min].
     */
    fun min(min: Int): Validator<Int> = Validator { value ->
        if (value < min) ValidationResult.Invalid(BuiltInValidationError.TooShort(min))
        else ValidationResult.Valid
    }

    /**
     * Fails when the [Int] value is greater than [max].
     */
    fun max(max: Int): Validator<Int> = Validator { value ->
        if (value > max) ValidationResult.Invalid(BuiltInValidationError.TooLong(max))
        else ValidationResult.Valid
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    )
}

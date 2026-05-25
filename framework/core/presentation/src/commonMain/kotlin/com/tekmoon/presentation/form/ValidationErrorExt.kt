package com.tekmoon.presentation.form

import com.tekmoon.designsystem.ui.UiText
import com.tekmoon.designsystem.ui.uiText
import com.tekmoon.domain.util.form.BuiltInValidationError
import com.tekmoon.domain.util.form.ValidationError
import com.tekmoon.presentation.generated.resources.Res
import com.tekmoon.presentation.generated.resources.validation_invalid_email
import com.tekmoon.presentation.generated.resources.validation_invalid_pattern
import com.tekmoon.presentation.generated.resources.validation_required
import com.tekmoon.presentation.generated.resources.validation_too_long
import com.tekmoon.presentation.generated.resources.validation_too_short

/**
 * Maps a [ValidationError] to a [UiText] suitable for display in the UI.
 *
 * For [BuiltInValidationError] types the framework provides default string resources
 * (see `presentation/composeResources/values/strings.xml`).
 *
 * For custom app-level errors, add your own extension or override in the call site:
 * ```kotlin
 * val message = when (val e = fieldState.error) {
 *     is MyAppError.PasswordMismatch -> uiText(Res.string.passwords_do_not_match)
 *     else                           -> e?.toUiText()
 * }
 * ```
 */
fun ValidationError.toUiText(): UiText = when (this) {
    is BuiltInValidationError.Required        -> uiText(Res.string.validation_required)
    is BuiltInValidationError.TooShort        -> uiText(Res.string.validation_too_short, min)
    is BuiltInValidationError.TooLong         -> uiText(Res.string.validation_too_long, max)
    is BuiltInValidationError.InvalidEmail    -> uiText(Res.string.validation_invalid_email)
    is BuiltInValidationError.InvalidPattern  -> uiText(Res.string.validation_invalid_pattern)
    is BuiltInValidationError.Custom          -> uiText(message)
    else                                      -> uiText(toString())
}

/** Convenience — returns `null` when there is no error. */
fun ValidationError?.toUiTextOrNull(): UiText? = this?.toUiText()

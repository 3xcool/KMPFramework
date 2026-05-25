package com.tekmoon.domain.util.form

/**
 * Controls when a [FieldController] runs its validators.
 *
 * | Trigger              | Behaviour                                                        |
 * |----------------------|------------------------------------------------------------------|
 * | [OnChange]           | Validate on every value change.                                  |
 * | [OnBlur]             | Validate when the field loses focus.                             |
 * | [OnSubmit]           | Validate only when [FormController.validateAll] is called.       |
 * | [OnChangeAfterBlur]  | Silent until first blur; then validate on every subsequent change. |
 */
enum class ValidationTrigger {
    OnChange,
    OnBlur,
    OnSubmit,
    OnChangeAfterBlur,
}

package com.tekmoon.presentation.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tekmoon.domain.util.form.AsyncValidator
import com.tekmoon.domain.util.form.FieldController
import com.tekmoon.domain.util.form.FieldState
import com.tekmoon.domain.util.form.ValidationTrigger
import com.tekmoon.domain.util.form.Validator
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Creates and remembers a [FieldController] scoped to the current composition.
 *
 * The controller is recreated only when [initialValue] changes identity,
 * so stable initial values (empty string, `false`, `null`) persist correctly
 * across recompositions.
 *
 * ### Usage
 * ```kotlin
 * @Composable
 * fun LoginScreen(viewModel: LoginViewModel) {
 *     // Option A — controller lives in ViewModel (recommended for shared/complex forms)
 *     val emailState by viewModel.email.rememberState()
 *
 *     // Option B — lightweight local field (no ViewModel needed)
 *     val nameController = rememberFieldController(
 *         initialValue = "",
 *         validators   = listOf(Validators.required() and Validators.minLength(2)),
 *         trigger      = ValidationTrigger.OnBlur,
 *     )
 *     val nameState by nameController.rememberState()
 *
 *     DsTextField(
 *         value        = nameState.value,
 *         onValueChange = nameController::onChange,
 *         onFocusLost  = nameController::onBlur,
 *         error        = nameState.error?.toUiText(),
 *     )
 * }
 * ```
 *
 * @param initialValue   Starting value; changing this reference recreates the controller.
 * @param validators     Synchronous validators, evaluated in order.
 * @param asyncValidators Async validators run after all sync validators pass.
 * @param trigger        Determines when validation fires automatically.
 */
@Composable
fun <T> rememberFieldController(
    initialValue: T,
    validators: List<Validator<T>> = emptyList(),
    asyncValidators: List<AsyncValidator<T>> = emptyList(),
    trigger: ValidationTrigger = ValidationTrigger.OnChange,
): FieldController<T> {
    val scope = rememberCoroutineScope()
    return remember(initialValue) {
        FieldController(
            initialValue    = initialValue,
            validators      = validators,
            asyncValidators = asyncValidators,
            trigger         = trigger,
            scope           = scope,
        )
    }
}

/**
 * Collects this controller's [FieldController.state] as Compose [State],
 * respecting the lifecycle of the current composition.
 *
 * ```kotlin
 * val emailState by viewModel.emailController.rememberState()
 * ```
 */
@Composable
fun <T> FieldController<T>.rememberState(): State<FieldState<T>> =
    state.collectAsStateWithLifecycle()

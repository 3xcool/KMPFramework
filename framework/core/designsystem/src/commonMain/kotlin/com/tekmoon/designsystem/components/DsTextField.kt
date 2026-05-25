package com.tekmoon.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.foundation.DsShapesDefault
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageSource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset


data class FieldState(
    val value: String,
    val error: String? = null
)

@Composable
fun DsTextField(
    state: FieldState,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    DsTextField(
        value = state.value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        isError = state.error != null,
        helperText = state.error
    )
}


@Composable
fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,

    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    isError: Boolean = false,

    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,

    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,

    leadingIcon: DsTextFieldIcon? = null,
    trailingIcon: DsTextFieldIcon? = null,

    visualVariant: DsTextFieldVariant = DsTextFieldVariant.Outlined,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,

    textStyle: TextStyle = DsTheme.typography.base,
    colors: DsTextFieldColors = DsTextFieldDefaults.colors(),
    shape: Shape = DsShapesDefault.md
) {
    DsTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualVariant = visualVariant,
        isPassword = isPassword,
        isPasswordVisible = isPasswordVisible,
        textStyle = textStyle,
        colors = colors,
        shape = shape
    )
}

sealed class DsTextFieldVariant {
    data object Filled : DsTextFieldVariant()
    data object Outlined : DsTextFieldVariant()
    data object Flat : DsTextFieldVariant()
    data object Underline : DsTextFieldVariant()
}

@Composable
internal fun DsTextFieldImpl(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,

    label: String?,
    placeholder: String?,
    helperText: String?,
    isError: Boolean,

    enabled: Boolean,
    readOnly: Boolean,
    singleLine: Boolean,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,

    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,

    leadingIcon: DsTextFieldIcon? = null,
    trailingIcon: DsTextFieldIcon? = null,

    visualVariant: DsTextFieldVariant,
    isPassword: Boolean,
    isPasswordVisible: Boolean,

    textStyle: TextStyle,
    colors: DsTextFieldColors,
    shape: Shape
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var passwordVisible by remember { mutableStateOf(isPasswordVisible) }

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledBorder
            isError -> colors.errorBorder
            focused -> colors.focusedBorder
            else -> colors.border
        }
    )

    val backgroundColor = when (visualVariant) {
        DsTextFieldVariant.Filled -> colors.background
        DsTextFieldVariant.Outlined -> Color.Transparent
        DsTextFieldVariant.Flat -> Color.Transparent
        DsTextFieldVariant.Underline -> Color.Transparent
    }

    val decorationModifier = when (visualVariant) {
        DsTextFieldVariant.Outlined ->
            Modifier.border(1.dp, animatedBorderColor, shape)

        DsTextFieldVariant.Underline ->
            Modifier.drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2

                drawLine(
                    color = animatedBorderColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }

        else -> Modifier
    }

    Column(modifier = modifier) {

        if (label != null) {
            BasicText(
                text = label,
                style = DsTheme.typography.sm.copy(
                    color = if (isError) colors.errorText else colors.label
                )
            )
            Spacer(Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier
                .background(backgroundColor, shape)
                .then(decorationModifier)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            leadingIcon?.let {
                DsTextFieldIcon(it)
                Spacer(Modifier.width(8.dp))
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                readOnly = readOnly,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                textStyle = textStyle.copy(
                    color = if (enabled) colors.text else colors.disabledText
                ),
                cursorBrush = SolidColor(colors.cursor),
                visualTransformation =
                    if (isPassword && !passwordVisible)
                        PasswordVisualTransformation()
                    else VisualTransformation.None,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        BasicText(
                            text = placeholder,
                            style = textStyle.copy(
                                color = colors.placeholder
                            ),
                        )
                    }
                    inner()
                }
            )

            when {
                isPassword -> {
                    DsPasswordToggle(
                        visible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible }
                    )
                }
                trailingIcon != null -> {
                    DsTextFieldIcon(trailingIcon)
                    Spacer(Modifier.width(8.dp))
                }
            }
        }

        if (helperText != null) {
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = helperText,
                style = DsTheme.typography.xs.copy(
                    color = if (isError) colors.errorText else colors.helper
                )
            )
        }
    }
}


@Composable
private fun DsPasswordToggle(
    visible: Boolean,
    onToggle: () -> Unit
) {
    BasicText(
        text = if (visible) "🙈" else "👁",
        modifier = Modifier
            .padding(start = 8.dp)
            .clickable(onClick = onToggle)
    )
}


@Immutable
data class DsTextFieldColors(
    val text: Color,
    val disabledText: Color,
    val placeholder: Color,
    val cursor: Color,

    val label: Color,
    val helper: Color,
    val errorText: Color,

    val background: Color,

    val border: Color,
    val focusedBorder: Color,
    val disabledBorder: Color,
    val errorBorder: Color
)


object DsTextFieldDefaults {

    @Composable
    fun colors() = DsTextFieldColors(
        text = DsTheme.textColors.primary,
        disabledText = DsTheme.textColors.disabled,
        placeholder = DsTheme.textColors.tertiary,
        cursor = DsTheme.colors.primary,

        label = DsTheme.textColors.secondary,
        helper = DsTheme.textColors.tertiary,
        errorText = DsTheme.colors.danger,

        background = DsTheme.colors.bgLight,

        border = DsTheme.colors.bgDark,
        focusedBorder = DsTheme.colors.primary,
        disabledBorder = DsTheme.colors.bg,
        errorBorder = DsTheme.colors.danger
    )
}



@Immutable
data class DsTextFieldIcon(
    val source: DsImageSource,
    val size: Dp = 20.dp,
    val tint: Color
)

@Composable
private fun DsTextFieldIcon(
    icon: DsTextFieldIcon
) {
    DsImage(
        source = icon.source,
        contentDescription = null,
        modifier = Modifier.size(icon.size),
        tint = icon.tint
    )
}
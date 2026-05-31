package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsTextField
import com.tekmoon.designsystem.components.DsTextFieldVariant
import org.jetbrains.compose.ui.tooling.preview.Preview


@Preview(
    showBackground = true,
    heightDp = 1200
)
@Composable
fun DsTextFieldPreview_Empty() {

    DsPreviewScaffold(
        layout = { content ->
            Column {
                content()
            }
        }
    ) {
        var text by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Username",
                placeholder = "Enter username"
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Password",
                placeholder = "••••••",
                isPassword = true,
                passwordShowContentDescription = "Show password",
                passwordHideContentDescription = "Hide password",
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Password",
                placeholder = "••••••",
                isPassword = true,
                isPasswordVisible = true,
                passwordShowContentDescription = "Show password",
                passwordHideContentDescription = "Hide password",
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Email",
                helperText = "Invalid email",
                isError = true
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Flat",
                visualVariant = DsTextFieldVariant.Flat
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Filled",
                visualVariant = DsTextFieldVariant.Filled
            )
            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Underline",
                visualVariant = DsTextFieldVariant.Underline
            )
        }
    }
}


@Preview(
    showBackground = true,
    heightDp = 1200
)
@Composable
fun DsTextFieldPreview_WithText() {

    DsPreviewScaffold(
        layout = { content ->
            Column {
                content()
            }
        }
    ) {
        var text by remember { mutableStateOf("Luke I'm your father") }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Username",
                placeholder = "Enter username"
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Password",
                placeholder = "••••••",
                isPassword = true,
                passwordShowContentDescription = "Show password",
                passwordHideContentDescription = "Hide password",
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Password",
                placeholder = "••••••",
                isPassword = true,
                isPasswordVisible = true,
                passwordShowContentDescription = "Show password",
                passwordHideContentDescription = "Hide password",
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Email",
                helperText = "Invalid email",
                isError = true
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Flat",
                visualVariant = DsTextFieldVariant.Flat
            )

            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Filled",
                visualVariant = DsTextFieldVariant.Filled
            )
            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Underline",
                visualVariant = DsTextFieldVariant.Underline
            )
        }
    }
}

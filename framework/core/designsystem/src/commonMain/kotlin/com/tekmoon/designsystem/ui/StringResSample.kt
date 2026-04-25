package com.tekmoon.designsystem.ui

import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kmpframework.framework.core.designsystem.generated.resources.fk_core_ds_preview_dsl
import kmpframework.framework.core.designsystem.generated.resources.fk_core_ds_preview_items
import kmpframework.framework.core.designsystem.generated.resources.fk_core_ds_preview_title
import kmpframework.framework.core.designsystem.generated.resources.fk_core_ds_preview_welcome

/* -------------------------------------------------------------------------- */
/* Compose runtime & UI                                                       */
/* -------------------------------------------------------------------------- */
import androidx.compose.runtime.Composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.tekmoon.designsystem.components.DsText

/* -------------------------------------------------------------------------- */
/* Compose Multiplatform Preview                                              */
/* -------------------------------------------------------------------------- */
import org.jetbrains.compose.ui.tooling.preview.Preview
import kmpframework.framework.core.designsystem.generated.resources.Res

/* -------------------------------------------------------------------------- */
/* Compose Multiplatform Resources                                            */
/* -------------------------------------------------------------------------- */

/* -------------------------------------------------------------------------- */
/* UiText infra                                                               */
/* -------------------------------------------------------------------------- */
import kmpframework.framework.core.designsystem.generated.resources.fk_core_ds_name

/**
 * To create String res
 * ./gradlew :framework:core:presentation:compileCommonMainKotlinMetadata
 */

@Composable
fun StringResSample() {
    DsText(stringResource(Res.string.fk_core_ds_name))
}


@Preview(showBackground = true)
@Composable
fun UiText_AllUseCases_Preview() {
    CompositionLocalProvider(
        LocalUiTextFallbackResolver provides PreviewUiTextFallbackResolver
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Dynamic string
            BasicText(
                text = uiText("Dynamic string").asStringOrEmpty()
            )

            // Annotated string
            BasicText(
                text = uiText(
                    buildAnnotatedString {
                        append("Annotated ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Text")
                        }
                    }
                ).asAnnotatedStringOrEmpty()
            )

            // Plain string resource
            BasicText(
                text = uiText(Res.string.fk_core_ds_preview_title).asStringOrEmpty()
            )

            // String resource with arguments
            BasicText(
                text = uiText(
                    Res.string.fk_core_ds_preview_welcome,
                    "Andre"
                ).asStringOrEmpty()
            )

            // Quantity / plural string
            BasicText(
                text = uiTextQuantity(
                    Res.plurals.fk_core_ds_preview_items,
                    quantity = 3,
                    3
                ).asStringOrEmpty()
            )

            // DSL usage
            BasicText(
                text = uiText {
                    string(Res.string.fk_core_ds_preview_dsl, "DSL")
                }.asStringOrEmpty()
            )
        }
    }
}

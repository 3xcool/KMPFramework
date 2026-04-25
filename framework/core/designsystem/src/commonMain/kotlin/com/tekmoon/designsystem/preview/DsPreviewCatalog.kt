package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.components.DsButton
import com.tekmoon.designsystem.components.DsButtonIconPosition
import com.tekmoon.designsystem.components.DsText
import com.tekmoon.designsystem.components.DsTextField
import com.tekmoon.designsystem.components.DsTextFieldVariant
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageSource
import kmpframework.framework.core.designsystem.generated.resources.Res
import kmpframework.framework.core.designsystem.generated.resources.dog_man_image
import kmpframework.framework.core.designsystem.generated.resources.ic_outline_check_24

@Composable
fun DsPreviewCatalog() {

    LazyColumn(
        modifier = Modifier
//                .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            // App UI
            DsText(
                text = "Hey guys"
            )
            DsImage(
                source = DsImageSource.DrawableImage(Res.drawable.dog_man_image),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )

            DsImagePreviewSample()

            DsButton(
                text = "Icon End",
                icon = DsImageSource.DrawableImage(Res.drawable.ic_outline_check_24),
                iconPosition = DsButtonIconPosition.End,
//                contentColor = Color.Red,
                onClick = {}
            )
        }

        item {
            var text by remember { mutableStateOf("Luke I'm your father") }
            DsTextField(
                modifier = Modifier,
                value = text,
                onValueChange = { text = it },
                visualVariant = DsTextFieldVariant.Underline,
                label = "Email",
            )
        }

        item {
            DsSurface_StaticMatrix_Preview()
        }

        item {
            DsSurface_Interactive_Preview()
        }

        item {
            DsSurface_RealWorld_Preview()
        }

    }
//        DsSurface_Performance_List_Preview()
}
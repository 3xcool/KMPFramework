package com.tekmoon.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.tekmoon.designsystem.image.DsAsyncImage
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageLocal
import com.tekmoon.designsystem.image.DsImageSource
import com.tekmoon.designsystem.image.LocalDsImageLoader
import com.tekmoon.designsystem.platform.currentPlatformContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.tekmoon.designsystem.generated.resources.Res
import com.tekmoon.designsystem.generated.resources.dog_man_image

// Unfortunately CMP Previews doesn't show the real image =/

@Composable
fun rememberPreviewImageLoader(): ImageLoader {
    val context = currentPlatformContext()

    return remember {
        ImageLoader.Builder(context)
            // No network fetchers → forces Error state for remote URLs
            .build()
    }
}

@Composable
private fun DsImagePreviewWrapper(
    content: @Composable () -> Unit
) {
    val previewImageLoader = rememberPreviewImageLoader()

    DsPreviewScaffold {
        CompositionLocalProvider(
            LocalDsImageLoader provides previewImageLoader
        ) {
            content()
        }
    }
}

@Preview
@Composable
fun DsImage_Local_Preview() {
    DsImagePreviewWrapper {
        DsImage(
            source = DsImageSource.DrawableImage(Res.drawable.dog_man_image),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
    }
}


@Preview
@Composable
fun DsAsyncImage_Loading_Preview() {
    DsImagePreviewWrapper {
        DsAsyncImage(
            url = "https://example.com/image.jpg",
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
    }
}

@Preview
@Composable
fun DsAsyncImage_Error_Preview() {
    DsImagePreviewWrapper {
        DsAsyncImage(
            url = "https://this-will-fail.com/image.jpg",
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            retryOnTap = false
        )
    }
}



@Preview
@Composable
fun DsAsyncImage_Success_Local_Preview() {
    DsImagePreviewWrapper {
        DsImageLocal(
            drawable = Res.drawable.dog_man_image, // stand-in for real image
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview
@Composable
fun DsAsyncImage_Success_Remote_Preview() {
    DsImagePreviewWrapper {
        DsImage(
            source = DsImageSource.Remote("https://picsum.photos/200/300"),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DsImage_AllStates_Preview() {
    DsImagePreviewWrapper {
        Column {
            DsImageLocal(
                drawable = Res.drawable.dog_man_image,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            DsImage(
                source = DsImageSource.Remote("https://picsum.photos/200/300"),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )

            DsAsyncImage(
                url = "https://loading.preview",
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            DsAsyncImage(
                url = "https://error.preview",
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                retryOnTap = false
            )
        }
    }
}

@Composable
fun DsImagePreviewSample() {
    DsImagePreviewWrapper{
        Column {
            DsImageLocal(
                drawable = Res.drawable.dog_man_image,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            DsImage(
                source = DsImageSource.Remote("https://picsum.photos/200/300"),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )

            DsAsyncImage(
                url = "https://loading.preview",
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            DsAsyncImage(
                url = "https://error.preview",
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                retryOnTap = false
            )
        }
    }
}
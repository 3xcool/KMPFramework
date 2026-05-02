package com.tekmoon.designsystem.image


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.extensions.applyIf
import com.tekmoon.designsystem.platform.currentPlatformContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.tekmoon.designsystem.generated.resources.Res
import com.tekmoon.designsystem.generated.resources.fk_core_design_system_retry_image
import com.tekmoon.designsystem.generated.resources.img_error
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.ImageLoader

val LocalDsImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("DsImageLoader not provided")
}

sealed interface DsImageSource {

    /** Local bundled image (png, jpg, webp, illustration, photo) */
    data class DrawableImage(
        val resource: DrawableResource
    ) : DsImageSource

    /** Local bundled icon (vector or icon-like drawable) */
    data class DrawableIcon(
        val resource: DrawableResource
    ) : DsImageSource

    /** Remote image (future-proof) */
    data class Remote(
        val url: String
    ) : DsImageSource
}


object DsImageDefaults {

    enum class DsIconSize(val size: Dp) {
        Small(16.dp),
        Medium(24.dp),
        Large(32.dp),
        Dialog(96.dp)
    }

    val defaultShape: Shape = RoundedCornerShape(0.dp)
    val defaultIconSize: Dp = DsIconSize.Medium.size
    val defaultImageSize: Dp = 40.dp
}

data class DsImagePlaceholders(
    val loading: DrawableResource?,
    val error: DrawableResource?
)

/**
 * Usage:
 * DsVisualImage(
 *     visual = DsVisual.Drawable(Res.drawable.img_profile),
 *     contentDescription = null,
 *     contentScale = ContentScale.Crop
 * )
 */
@Composable
fun DsImage(
    source: DsImageSource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    iconSize: Dp? = DsImageDefaults.defaultIconSize,
    imageSize: Dp? = DsImageDefaults.defaultImageSize,
    contentScale: ContentScale = ContentScale.Fit
) {
    val resolvedModifier = when (source) {
        is DsImageSource.DrawableIcon ->
            modifier.applyDefaultSizeIfUnspecified(iconSize)

        is DsImageSource.DrawableImage ->
            modifier.applyDefaultSizeIfUnspecified(imageSize)

        is DsImageSource.Remote ->
            modifier.applyDefaultSizeIfUnspecified(imageSize)
    }

    when (source) {
        is DsImageSource.DrawableIcon ->
            DsImageLocal(
                drawable = source.resource,
                contentDescription = contentDescription,
                modifier = resolvedModifier,
                tint = tint,
                contentScale = contentScale
            )

        is DsImageSource.DrawableImage ->
            DsImageLocal(
                drawable = source.resource,
                contentDescription = contentDescription,
                modifier = resolvedModifier,
                contentScale = contentScale
            )

        is DsImageSource.Remote ->
            DsAsyncImage(
                url = source.url,
                contentDescription = contentDescription,
                modifier = resolvedModifier,
                contentScale = contentScale
            )
    }
}

@Composable
fun DsImageLocal(
    drawable: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentScale: ContentScale = ContentScale.Fit,
    shape: Shape = DsImageDefaults.defaultShape
) {
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        // Preview-safe fallback
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color.Red)
        )
        return
    }

    Image(
        painter = painterResource(drawable),
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        colorFilter = tint?.let(ColorFilter::tint)
    )
}


/**
 * Tap error → retry
 */
@Composable
fun DsAsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    retryOnTap: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = DsSkeletonDefaults.shape,
    errorDrawable: DrawableResource = Res.drawable.img_error,
    cacheKey: String? = null
) {
    var measuredSize by remember { mutableStateOf<IntSize?>(null) }
    var retryToken by remember { mutableStateOf(0) }

    val imageLoader = LocalDsImageLoader.current

    val request = rememberDsImageRequest(
        url = url,
        size = measuredSize,
        cacheKey = cacheKey?.let { "$it-$retryToken" }
    )

    val painter = rememberAsyncImagePainter(
        model = request,
        imageLoader = imageLoader,
        contentScale = contentScale
    )

    val state by painter.state.collectAsState()

    Box(
        modifier = modifier
            .clip(shape)
            .onSizeChanged { measuredSize = it }
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale
        )

        when (state) {
            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading -> {
                DsShimmer(
                    modifier = Modifier.matchParentSize(),
                    shape = shape
                )
            }

            is AsyncImagePainter.State.Error -> {
                DsIcon(
                    drawable = errorDrawable,
                    contentDescription = stringResource(
                        Res.string.fk_core_design_system_retry_image
                    ),
                    modifier = Modifier
                        .matchParentSize()
                        .applyIf(retryOnTap) {
                            clickable { retryToken++ }
                        }
                )
            }

            is AsyncImagePainter.State.Success -> Unit
        }
    }
}



/**
 * Usage:
 * DsIcon(
 *     drawable = Res.drawable.ic_arrow_back,
 *     contentDescription = "Back"
 * )
 */
@Composable
fun DsIcon(
    drawable: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = DsImageDefaults.DsIconSize.Medium.size
) {
    DsImageLocal(
        drawable = drawable,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = DsTheme.colors.content
    )
}

@Composable
internal fun rememberDsImageRequest(
    url: String,
    size: IntSize?,
    cacheKey: String? = null
): ImageRequest {
    val context = currentPlatformContext()

    return ImageRequest.Builder(context)
        .data(url)
        .apply {
            size?.let {
                size(it.width, it.height)

                cacheKey?.let {
                    memoryCacheKey(it)
                    diskCacheKey(it)
                }
            }
        }
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .crossfade(false) // skeleton already animates
        .build()
}

enum class DsMotionLevel {
    Full,
    Reduced
}

val LocalDsMotion = staticCompositionLocalOf { DsMotionLevel.Full }


private fun Modifier.applyDefaultSizeIfUnspecified(
    size: Dp?
): Modifier {
    return size?.let{
        this.size(it)
    } ?: this
}

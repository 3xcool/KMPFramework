package com.tekmoon.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.image.DsImage
import com.tekmoon.designsystem.image.DsImageDefaults
import com.tekmoon.designsystem.image.DsImageSource
import com.tekmoon.designsystem.image.LocalDsImageLoader
import com.tekmoon.designsystem.preview.DsPreviewScaffold
import com.tekmoon.designsystem.preview.rememberPreviewImageLoader
import com.tekmoon.presentation.clickableWithoutIndication
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.tekmoon.designsystem.generated.resources.Res
import com.tekmoon.designsystem.generated.resources.ic_outline_check_24


data class CoreBottomNavItem(
    val icon: DsImageSource,
    val isSelected: Boolean,
    val title: String? = null,
    val badge: CoreBottomNavBadge? = null,
    val onClick: () -> Unit
)

sealed interface CoreBottomNavBadge {
    object Dot : CoreBottomNavBadge
    data class Count(val value: Int) : CoreBottomNavBadge
}


@Composable
fun CoreBottomNavBar(
    items: ImmutableList<CoreBottomNavItem>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DsTheme.colors.bg,
    selectedColor: Color = DsTheme.colors.primary,
    unselectedColor: Color = DsTheme.colors.content.copy(alpha = 0.6f),
    showGlowEffect: Boolean = true,
    elevation: Dp = 8.dp
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                CoreBottomNavItemView(
                    item = item,
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                    showGlowEffect = showGlowEffect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CoreBottomNavItemView(
    item: CoreBottomNavItem,
    selectedColor: Color,
    unselectedColor: Color,
    showGlowEffect: Boolean = true,
    modifier: Modifier = Modifier
) {
    val contentColor = if (item.isSelected) selectedColor else unselectedColor

    val glowAlpha by animateFloatAsState(
        targetValue = if (item.isSelected) 0.35f else 0f,
        label = "glow"
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .clickableWithoutIndication(item.onClick),
        contentAlignment = Alignment.Center
    ) {

        // Glow background
        if (showGlowEffect && item.isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                selectedColor.copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            radius = 100f
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                DsImage(
                    source = item.icon,
                    tint = contentColor,
                    contentDescription = item.title,
                    iconSize = DsImageDefaults.DsIconSize.Medium.size
                )

                // Badge
                item.badge?.let { badge ->
                    CoreBottomNavBadgeView(
                        badge = badge,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                    )
                }
            }

            item.title?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = contentColor,
                    style = DsTheme.typography.xs,
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun CoreBottomNavBadgeView(
    badge: CoreBottomNavBadge,
    modifier: Modifier = Modifier
) {
    when (badge) {
        CoreBottomNavBadge.Dot -> {
            Box(
                modifier = modifier
                    .size(8.dp)
                    .background(
                        color = DsTheme.colors.danger,
                        shape = CircleShape
                    )
            )
        }

        is CoreBottomNavBadge.Count -> {
            Box(
                modifier = modifier
                    .background(
                        color = DsTheme.colors.danger,
                        shape = CircleShape
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.value.toString(),
                    style = DsTheme.typography.xs,
                    color = Color.White
                )
            }
        }
    }
}


/**
 * @suppress
 */
@Preview(
    group = "CoreBottomNavBar",
    name = "Base - Light - ",
    showBackground = true,
)
@Composable
private fun CoreBottomNavBarPreview() {
    val previewImageLoader = rememberPreviewImageLoader()

    DsPreviewScaffold(
        padding = 0.dp,
        layout = { content ->
            Column {
                content()
            }
        }
    ) {
        CompositionLocalProvider(
            LocalDsImageLoader provides previewImageLoader
        ) {
            CoreBottomNavBar(
                items = persistentListOf(
                    CoreBottomNavItem(
                        icon = DsImageSource.DrawableIcon(
                            Res.drawable.ic_outline_check_24
                        ),
                        title = "Home",
                        badge = CoreBottomNavBadge.Count(99),
                        isSelected = false,
                        onClick = {}
                    ),
                    CoreBottomNavItem(
                        icon = DsImageSource.DrawableIcon(
                            Res.drawable.ic_outline_check_24
                        ),
                        badge = CoreBottomNavBadge.Dot,
                        title = "Profile",
                        isSelected = true,
                        onClick = {}
                    ),
                    CoreBottomNavItem(
                        icon = DsImageSource.DrawableIcon(
                            Res.drawable.ic_outline_check_24
                        ),
                        title = "Settings",
                        badge = CoreBottomNavBadge.Count(1),
                        isSelected = false,
                        onClick = {}
                    )
                )
            )
        }
    }
}
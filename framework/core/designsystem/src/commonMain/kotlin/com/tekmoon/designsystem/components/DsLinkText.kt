package com.tekmoon.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.analytics.LocalAnalytics

/**
 * @param contentDescription Accessibility label for the link. Pass a descriptive sentence
 *                           that reads well in isolation ("Open privacy policy") rather than
 *                           a bare URL.
 * @param analyticsId Stable identifier emitted with the `"ds_link_clicked"` analytics event
 *                    when the link is followed. The resolved URL is included in the event
 *                    payload as `url`. `null` (default) disables analytics for this link.
 * @param analyticsParams Extra params merged into the event alongside `id` + `url`.
 */
@Composable
fun DsLinkText(
    text: AnnotatedString,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = DsTheme.typography.sm,
    analyticsId: String? = null,
    analyticsParams: Map<String, Any?> = emptyMap(),
    onLinkClick: (String) -> Unit,
) {
    val analytics = LocalAnalytics.current
    DsClickableText(
        text = text,
        contentDescription = contentDescription,
        modifier = modifier,
        style = style,
        // Explicitly disable DsClickableText's own tracking so DsLinkText is the single
        // source of "ds_link_clicked" events.
        analyticsId = null,
        onClick = {
            val url = text
                .getStringAnnotations("URL", 0, text.length)
                .firstOrNull()
                ?.item
            if (url != null) {
                if (analyticsId != null) {
                    analytics.track(
                        event = "ds_link_clicked",
                        params = mapOf(
                            "id" to analyticsId,
                            "url" to url,
                        ) + analyticsParams,
                    )
                }
                onLinkClick(url)
            }
        },
    )
}

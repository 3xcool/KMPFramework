package com.tekmoon.designsystem.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode

/* -------------------------------------------------------------------------- */
/* UiText model                                                               */
/* -------------------------------------------------------------------------- */

@Stable
sealed class UiText {

    data class DynamicString(val text: String?) : UiText()

    data class Annotated(val annotatedString: AnnotatedString) : UiText()

    data class StringRes(val resource: StringResource) : UiText()

    data class StringResArgs(
        val resource: StringResource,
        val args: ImmutableList<Any?> = persistentListOf()
    ) : UiText() {
        @Suppress("SpreadOperator")
        constructor(resource: StringResource, vararg args: Any?) : this(
            resource,
            // persistentListOf takes vararg; the spread is the only way to
            // forward a constructor vararg into it.
            persistentListOf(*args)
        )
    }

    data class QuantityStringRes(
        val resource: PluralStringResource,
        val quantity: Int,
        val args: ImmutableList<Any?> = persistentListOf()
    ) : UiText()
}

/* -------------------------------------------------------------------------- */
/* Inline helpers                                                             */
/* -------------------------------------------------------------------------- */

fun uiText(text: String?): UiText =
    UiText.DynamicString(text)

fun uiText(annotatedString: AnnotatedString): UiText =
    UiText.Annotated(annotatedString)

fun uiText(resource: StringResource): UiText =
    UiText.StringRes(resource)

fun uiText(resource: StringResource, vararg args: Any?): UiText =
    UiText.StringResArgs(resource, *args)

fun uiTextQuantity(
    resource: PluralStringResource,
    quantity: Int,
    vararg args: Any?
): UiText =
    UiText.QuantityStringRes(resource, quantity, persistentListOf(*args))

/* -------------------------------------------------------------------------- */
/* DSL                                                                        */
/* -------------------------------------------------------------------------- */

@UiTextDsl
fun uiText(block: UiTextBuilder.() -> Unit): UiText =
    UiTextBuilder().apply(block).build()

@DslMarker
annotation class UiTextDsl

class UiTextBuilder {

    private var value: UiText? = null

    fun text(value: String?) {
        this.value = UiText.DynamicString(value)
    }

    fun annotated(value: AnnotatedString) {
        this.value = UiText.Annotated(value)
    }

    fun string(resource: StringResource, vararg args: Any?) {
        this.value =
            if (args.isEmpty()) UiText.StringRes(resource)
            else UiText.StringResArgs(resource, *args)
    }

    fun quantity(
        resource: PluralStringResource,
        quantity: Int,
        vararg args: Any?
    ) {
        this.value = UiText.QuantityStringRes(
            resource = resource,
            quantity = quantity,
            args = persistentListOf(*args)
        )
    }

    internal fun build(): UiText =
        requireNotNull(value) {
            "UiText DSL must define exactly one value"
        }
}

/* -------------------------------------------------------------------------- */
/* Fallback resolver (preview / tests)                                        */
/* -------------------------------------------------------------------------- */

fun interface UiTextFallbackResolver {
    fun resolve(uiText: UiText): String?
}

/**
 * Preview-safe default fallback.
 */
object PreviewUiTextFallbackResolver : UiTextFallbackResolver {

    override fun resolve(uiText: UiText): String =
        when (uiText) {
            is UiText.StringRes ->
                "⟪${uiText.resource.key}⟫"

            is UiText.StringResArgs ->
                "⟪${uiText.resource.key}: ${uiText.args.joinToString()}⟫"

            is UiText.QuantityStringRes ->
                "⟪${uiText.resource.key} x${uiText.quantity}⟫"

            else ->
                "⟪UiText⟫"
        }
}

/**
 * CompositionLocal fallback resolver.
 *
 * Must be explicitly provided (e.g. in previews).
 */
val LocalUiTextFallbackResolver =
    staticCompositionLocalOf<UiTextFallbackResolver?> { null }

/* -------------------------------------------------------------------------- */
/* Resolvers                                                                  */
/* -------------------------------------------------------------------------- */
@Composable
private fun UiText.resolveCompose(): String? {
    val isPreview = LocalInspectionMode.current
    val fallback = LocalUiTextFallbackResolver.current

    return when (this) {
        is UiText.DynamicString -> text
        is UiText.Annotated -> annotatedString.text

        is UiText.StringRes ->
            if (isPreview) {
                fallback?.resolve(this)
            } else {
                stringResource(resource)
            }

        is UiText.StringResArgs ->
            if (isPreview) {
                fallback?.resolve(this)
            } else {
                @Suppress("SpreadOperator")
                // stringResource takes vararg Any?; the spread is the only way
                // to forward our already-materialized argument list.
                stringResource(
                    resource,
                    *args.filterNotNull().toTypedArray()
                )
            }

        is UiText.QuantityStringRes ->
            if (isPreview) {
                fallback?.resolve(this)
            } else {
                @Suppress("SpreadOperator")
                // pluralStringResource takes vararg Any?; same as above.
                pluralStringResource(
                    resource,
                    quantity,
                    *args.filterNotNull().toTypedArray()
                )
            }
    }
}

/**
 * Non-Compose resolver.
 *
 * Fails fast unless a fallback is provided.
 */
fun UiText.resolveRaw(
    fallback: UiTextFallbackResolver? = null
): String? =
    when (this) {
        is UiText.DynamicString -> text
        is UiText.Annotated -> annotatedString.text
        else -> fallback?.resolve(this)
            ?: error("Cannot resolve UiText without Compose or fallback: $this")
    }

/* -------------------------------------------------------------------------- */
/* Public API                                                                 */
/* -------------------------------------------------------------------------- */

@Composable
fun UiText.asString(): String? =
    resolveCompose()

@Composable
fun UiText?.asStringOrEmpty(): String =
    this?.resolveCompose().orEmpty()

@Composable
fun UiText.asAnnotatedString(): AnnotatedString? =
    resolveCompose()?.let(::AnnotatedString)

@Composable
fun UiText.asAnnotatedStringOrEmpty(): AnnotatedString =
    AnnotatedString(resolveCompose().orEmpty())


/* -------------------------------------------------------------------------- */
/* Usage                                                                 */
/* -------------------------------------------------------------------------- */
//@Preview
//@Composable
//fun MyPreview() {
//    CompositionLocalProvider(
//        LocalUiTextFallbackResolver provides PreviewUiTextFallbackResolver
//    ) {
////        MyScreen()
//    }
//}

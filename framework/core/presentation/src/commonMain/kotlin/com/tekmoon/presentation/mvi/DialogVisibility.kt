package com.tekmoon.presentation.mvi

/** Open/closed wrapper for a dialog, carrying optional args when open. */
sealed interface DialogVisibility<out T> {
    data object Closed : DialogVisibility<Nothing>
    data class Open<T>(val args: T? = null) : DialogVisibility<T>
}

inline val DialogVisibility<*>.isVisible: Boolean
    get() = this is DialogVisibility.Open<*>

inline val DialogVisibility<*>.isClosed: Boolean
    get() = this === DialogVisibility.Closed

inline fun <T> DialogVisibility<T>.argsOrNull(): T? =
    (this as? DialogVisibility.Open<T>)?.args

inline val DialogVisibility<*>.hasArgs: Boolean
    get() = (this as? DialogVisibility.Open<*>)?.args != null

inline fun <T, R> DialogVisibility<T>.mapArgs(transform: (T) -> R): DialogVisibility<R> =
    when (this) {
        is DialogVisibility.Open ->
            if (args == null) DialogVisibility.Open(null)
            else DialogVisibility.Open(transform(args))
        DialogVisibility.Closed -> DialogVisibility.Closed
    }

fun <T> openDialog(args: T? = null): DialogVisibility<T> = DialogVisibility.Open(args)

fun <T> DialogVisibility<T>.close(): DialogVisibility<T> = DialogVisibility.Closed

inline fun <T> DialogVisibility<T>.updateArgs(
    transform: (T?) -> T?,
): DialogVisibility<T> = when (this) {
    is DialogVisibility.Open -> DialogVisibility.Open(transform(this.args))
    DialogVisibility.Closed -> this
}

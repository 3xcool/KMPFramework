package com.tekmoon.domain.util.data

// ─── Base ────────────────────────────────────────────────────────────────────

/**
 * Marker interface for all errors flowing through the framework.
 *
 * Open by design — feature modules and apps can declare their own error types:
 * ```kotlin
 * sealed interface MyAppError : DataError {
 *     data object SessionExpired : MyAppError
 *     data class BrokenContract(val detail: String) : MyAppError
 * }
 * ```
 */
interface DataError : Error

// ─── HTTP 4xx ────────────────────────────────────────────────────────────────

/** Client-side HTTP errors (4xx). */
sealed interface ClientError : DataError {
    /** 400 — malformed request. */
    data object BadRequest : ClientError
    /** 401 — missing or invalid credentials. */
    data object Unauthorized : ClientError
    /** 403 — authenticated but not allowed. */
    data object Forbidden : ClientError
    /** 404 — resource does not exist. */
    data object NotFound : ClientError
    /** 408 — server closed the connection before the client finished. */
    data object Timeout : ClientError
    /** 409 — optimistic-lock or duplicate conflict. */
    data object Conflict : ClientError
    /** 413 — request body too large. */
    data object PayloadTooLarge : ClientError
    /** 429 — rate-limit exceeded. */
    data object TooManyRequests : ClientError
}

// ─── HTTP 5xx ────────────────────────────────────────────────────────────────

/** Server-side HTTP errors (5xx). */
sealed interface ServerError : DataError {
    /** 500 — unhandled server exception. */
    data object InternalError : ServerError
    /** 503 — server temporarily unavailable or under maintenance. */
    data object ServiceUnavailable : ServerError
}

// ─── Connectivity ─────────────────────────────────────────────────────────────

/** Device has no active network connection or the host is unreachable. */
data object NoInternet : DataError

// ─── Parsing ─────────────────────────────────────────────────────────────────

/** Response received but could not be deserialized into the expected type. */
data object Serialization : DataError

// ─── Local storage ───────────────────────────────────────────────────────────

/** Errors originating from on-device persistence (database, disk, preferences). */
sealed interface LocalError : DataError {
    /** The storage medium is full and the write could not complete. */
    data object DiskFull : LocalError
    /** The requested local record does not exist. */
    data object NotFound : LocalError
    /** An unexpected local storage error. */
    data class Unknown(val cause: Throwable? = null) : LocalError
}

// ─── Catch-all ────────────────────────────────────────────────────────────────

/** Fallback for errors that don't fit any other category. */
data class UnknownError(val cause: Throwable? = null) : DataError

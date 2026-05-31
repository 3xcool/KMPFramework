package com.tekmoon.backgroundwork

/**
 * Lifecycle state of a scheduled task, observable via
 * [BackgroundScheduler.observe].
 */
public sealed interface BackgroundStatus {
    public object Enqueued : BackgroundStatus
    public object Running : BackgroundStatus
    public object Succeeded : BackgroundStatus
    public data class Failed(val cause: Throwable? = null) : BackgroundStatus
    public object Cancelled : BackgroundStatus
}

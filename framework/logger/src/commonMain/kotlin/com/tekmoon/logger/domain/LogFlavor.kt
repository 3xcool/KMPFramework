package com.tekmoon.logger.domain

/**
 * Created by Andre Filgueiras on 12/11/24.
 *
 */
enum class LogFlavor(val value: String) {
    DEFAULT("default"),
    PERFORMANCE("performance"),
    FULL_INFO("fullinfo"),
    THREAD_INFO("threadinfo"),
    TRACE_INFO("traceinfo"),
    DEVELOP("develop"),
    PRODUCTION("production"),
}
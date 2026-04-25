package com.tekmoon.logger.domain

import com.tekmoon.logger.data.CommonLogWriter

/**
 * Created by Andre Filgueiras on 01/06/23.
 */
interface LoggerConfig {
    val enabled: Boolean
    val logWriters: List<LogWriter>
}

open class DefaultLoggerConfig(
    override val enabled: Boolean = true,
    override val logWriters: List<LogWriter> = listOf(CommonLogWriter()),
): LoggerConfig {
}

open class TestLoggerConfig(
    override val enabled: Boolean = true,
    override val logWriters: List<LogWriter> = listOf(CommonLogWriter(tag = "ShowMe")),
): LoggerConfig {
}

package com.tekmoon.logger

import com.tekmoon.logger.domain.LogWriter
import com.tekmoon.logger.domain.LoggerConfig

/**
 * Created by Andre Filgueiras on 20/06/23.
 */
open class AndroidLogConfig(
    override val enabled: Boolean = true,
    override val logWriters: List<LogWriter> = listOf(AndroidLogcatWriter()),
): LoggerConfig
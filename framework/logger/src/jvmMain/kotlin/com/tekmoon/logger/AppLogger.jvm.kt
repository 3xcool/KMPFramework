package com.tekmoon.logger

import com.tekmoon.logger.domain.LogFlavor
import com.tekmoon.logger.domain.LoggerConfig

actual fun getLogger(logFlavor: LogFlavor, config: LoggerConfig?): ShowMeLoggerK =
    ShowMeLoggerK(config = config ?: DesktopLogConfig())
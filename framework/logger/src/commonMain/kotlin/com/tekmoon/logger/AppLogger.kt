package com.tekmoon.logger

import com.tekmoon.logger.domain.LogFlavor
import com.tekmoon.logger.domain.LoggerConfig

class AppLogger() {
    fun build(): ShowMeLoggerK = getLogger(LogFlavor.DEFAULT)
}

expect fun getLogger(logFlavor: LogFlavor = LogFlavor.DEFAULT, config: LoggerConfig? = null): ShowMeLoggerK

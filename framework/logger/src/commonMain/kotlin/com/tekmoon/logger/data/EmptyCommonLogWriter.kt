package com.tekmoon.logger.data

import com.tekmoon.logger.domain.LogWriter
import com.tekmoon.logger.domain.Severity

/**
 * Use this log for security reasons
 */
class EmptyCommonLogWriter(): LogWriter() {
    override fun processLog(message: String, severity: Severity): String {
        return ""
    }

    override fun getWriteConfigInfo(): String {
        return ""
    }
}
package com.tekmoon.logger.data

import com.tekmoon.logger.domain.LogWriter
import com.tekmoon.logger.domain.Severity

/**
 * Minimum log implementation, good for Tests
 */
class CommonLogWriter(val tag: String = "", private val minSeverity: Severity = Severity.Verbose): LogWriter() {
    override fun isLoggable(severity: Severity): Boolean {
        return severity.ordinal >= minSeverity.ordinal
    }
    override fun processLog(message: String, severity: Severity): String {
        println("$tag: $message")
        return message
    }

    override fun getWriteConfigInfo(): String {
        return "Common Writer"
    }
}
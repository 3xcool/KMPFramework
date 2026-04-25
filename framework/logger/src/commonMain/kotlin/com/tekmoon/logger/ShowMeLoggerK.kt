package com.tekmoon.logger

import com.tekmoon.logger.domain.LogFlavor
import com.tekmoon.logger.domain.LogWriter
import com.tekmoon.logger.domain.LoggerConfig
import com.tekmoon.logger.domain.Severity

/**
 * Created by Andre Filgueiras on 01/06/23.
 */
open class ShowMeLoggerK(
    open val config: LoggerConfig,
) {

    fun v(message: String, onLog: (String) -> Unit = {}) { log(message, Severity.Verbose, onLog) }
    fun d(message: String, onLog: (String) -> Unit = {}) { log(message, Severity.Debug, onLog) }
    fun i(message: String, onLog: (String) -> Unit = {}) { log(message, Severity.Info, onLog) }
    fun w(message: String, onLog: (String) -> Unit = {}) { log(message, Severity.Warn, onLog) }
    fun e(message: String, onLog: (String) -> Unit = {}) { log(message, Severity.Error, onLog) }
    fun a(message: String, onLog: (String) -> Unit = {}) { log(message, Severity.Assert, onLog) }

    private fun log(message: String, severity: Severity, onLog: (String) -> Unit = {}) {
        if (config.enabled) {
            config.logWriters.forEach {
                if (it.isLoggable(severity = severity)) {
                    it.processLog(message = message, severity = severity).also(onLog)
                }
            }
        }
    }

    fun initConfig() {
        if (!config.enabled) return

        val writers = config.logWriters

        val minSeverity = Severity.entries.firstOrNull { severity ->
            writers.any { it.isLoggable(severity) }
        } ?: Severity.Assert

        log(
            message = buildString {
                appendLine("Logger initialized")
                appendLine("• enabled = ${config.enabled}")
                appendLine("• minSeverity = $minSeverity")
                appendLine("• writers = ${writers.map { it::class.simpleName }}")
                appendLine("• writers config = ${writers.map { it.getWriteConfigInfo() }}")
            },
            severity = minSeverity
        )
    }

}
package com.tekmoon.logger

import android.util.Log
import com.tekmoon.logger.domain.LogAdditionalInfo
import com.tekmoon.logger.domain.LogWriter
import com.tekmoon.logger.domain.Severity
import kotlin.math.log

/**
 * Created by Andre Filgueiras on 04/06/23.
 */
open class AndroidLogcatWriter(
    private val tag: String = "Android-ShowMe",
    private val minSeverity: Severity = Severity.Verbose,
    private val logAdditionalInfo: LogAdditionalInfo? = AndroidLogAdditionalInfoImpl(),
    private val addAdditionalInfo: Boolean = false, // decrease performance
    private val prettify: Boolean = false // decrease performance
): LogWriter() {

    override fun isLoggable(severity: Severity): Boolean {
        return severity.ordinal >= minSeverity.ordinal
    }

    override fun getWriteConfigInfo(): String {
        val additionalInfo = logAdditionalInfo as? AndroidLogAdditionalInfoImpl
        return buildString {
            appendLine("\n    - minSeverity = $minSeverity")
            appendLine("    - prettify = $prettify")
            appendLine("    - threadInfo = ${additionalInfo?.addThreadInfo}")
            appendLine("    - traceInfo = ${additionalInfo?.addTraceInfo}")
        }
    }

    override fun processLog(message: String, severity: Severity): String {
        return if (addAdditionalInfo && severity.ordinal >= Severity.Debug.ordinal) {
            val output = logAdditionalInfo?.let{
                "${it.getAdditionalInfo()}$message"
            } ?: message

            printToLogcat(output, severity)
            output
        } else {
            printToLogcat(message, severity)
            message
        }
    }

    private fun getSeverityChar(severity: Severity): String {
        if (prettify.not()) return ""
        return when (severity) {
            Severity.Verbose -> "\uD83D\uDCAC "
            Severity.Debug -> "\uD83D\uDC1E "
            Severity.Info -> "✋ "
            Severity.Warn -> "⚠ "
            Severity.Error -> "❌❌❌ "
            Severity.Assert -> "⛔⛔⛔ "
        }
    }

    private fun printToLogcat(message: String, severity: Severity) {
        val charIndicator = getSeverityChar(severity)
        when (severity) {
            Severity.Verbose -> Log.v(tag, "$charIndicator$message")
            Severity.Debug -> Log.d(tag, "$charIndicator$message")
            Severity.Info -> Log.i(tag, "$charIndicator$message")
            Severity.Warn -> Log.w(tag, "$charIndicator$message")
            Severity.Error -> Log.e(tag, "$charIndicator$message")
            Severity.Assert -> Log.wtf(tag, "$charIndicator$message")
        }
    }

}
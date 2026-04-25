package com.tekmoon.logger

import com.tekmoon.logger.domain.LogFlavor
import com.tekmoon.logger.domain.LoggerConfig
import com.tekmoon.logger.domain.Severity

actual fun getLogger(logFlavor: LogFlavor, config: LoggerConfig?): ShowMeLoggerK {
    config?.let {
        return ShowMeLoggerK(config = config)
    }

    return when(logFlavor) {
        LogFlavor.DEFAULT ->
            androidLogger(
                flavor = logFlavor,
                minSeverity = Severity.Debug,
                prettify = true,
                additionalInfo = AndroidLogAdditionalInfoImpl(
                    addThreadInfo = true,
                    addTraceInfo = false
                )
            )

        LogFlavor.DEVELOP ->
            androidLogger(
                flavor = logFlavor,
                minSeverity = Severity.Verbose,
                prettify = true,
                additionalInfo = AndroidLogAdditionalInfoImpl(
                    addThreadInfo = true,
                    addTraceInfo = true
                )
            )

        LogFlavor.PRODUCTION ->
            androidLogger(
                flavor = logFlavor,
                minSeverity = Severity.Warn,
                prettify = false
            )

        LogFlavor.PERFORMANCE ->
            androidLogger(
                flavor = logFlavor,
                minSeverity = Severity.Warn,
                prettify = false
            )

        LogFlavor.FULL_INFO ->
            androidLogger(
                flavor = logFlavor,
                minSeverity = Severity.Debug,
                prettify = true,
                additionalInfo = AndroidLogAdditionalInfoImpl(
                    addThreadInfo = true,
                    addTraceInfo = true
                )
            )

        LogFlavor.THREAD_INFO ->
            androidLogger(
                flavor = logFlavor,
                minSeverity = Severity.Debug,
                prettify = false,
                additionalInfo = AndroidLogAdditionalInfoImpl(
                    addThreadInfo = true,
                    addTraceInfo = false
                )
            )

        LogFlavor.TRACE_INFO ->
            androidLogger(
                flavor = logFlavor,
                minSeverity = Severity.Debug,
                prettify = false,
                additionalInfo = AndroidLogAdditionalInfoImpl(
                    addThreadInfo = false,
                    addTraceInfo = true
                )
            )
    }
}


private fun androidLogger(
    flavor: LogFlavor,
    enabled: Boolean = true,
    minSeverity: Severity,
    prettify: Boolean,
    additionalInfo: AndroidLogAdditionalInfoImpl? = null
): ShowMeLoggerK {
    return ShowMeLoggerK(
        config = AndroidLogConfig(
            enabled = enabled,
            logWriters = listOf(
                AndroidLogcatWriter(
                    tag = "Android-ShowMe-${flavor.value}",
                    minSeverity = minSeverity,
                    prettify = prettify,
                    addAdditionalInfo = additionalInfo != null,
                    logAdditionalInfo = additionalInfo
                )
            )
        )
    )
}
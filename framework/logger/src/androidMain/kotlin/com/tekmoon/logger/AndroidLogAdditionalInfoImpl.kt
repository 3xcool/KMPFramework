package com.tekmoon.logger


import com.tekmoon.logger.domain.DefaultLoggerConfig
import com.tekmoon.logger.domain.LogAdditionalInfo
import com.tekmoon.logger.domain.LoggerConfig

/**
 * Created by Andre Filgueiras on 20/06/23.
 */
open class AndroidLogAdditionalInfoImpl(
    internal val addTraceInfo: Boolean = true,
    internal val addThreadInfo: Boolean = true,
    private val ignoreClasses: List<String> = emptyList()
): LogAdditionalInfo {

    private val ignoreClassList = ignoreClasses + listOf(
        AndroidLogAdditionalInfoImpl::class.java.name,
        LogAdditionalInfo::class.java.name,
        AndroidLogcatWriter::class.java.name,
        DefaultLoggerConfig::class.java.name,
        LoggerConfig::class.java.name,
        ShowMeLoggerK::class.java.name,
    )

    private fun getThreadInfoString(): String =
        Thread.currentThread().run { "$name ($id) | " }

    private val trace: String
        get() = Throwable().stackTrace
            .first { it.className !in ignoreClassList }
            .let(::createStackElement)

//    See: https://stackoverflow.com/questions/38689399/log-method-name-and-line-number-in-timber
    protected open fun createStackElement(element: StackTraceElement): String {
//        var tag = element.className.substringAfterLast('.')
//        val m = ANONYMOUS_CLASS.matcher(tag)
//        if (m.find()) {
//            tag = m.replaceAll("")
//        }
//        // Tag length limit was removed in API 26.
//        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
//            tag
//        } else {
//            tag.substring(0, MAX_TAG_LENGTH)
//        }

        return "(${element.fileName}:${element.lineNumber})#${element.methodName} | "
    }

    override fun getAdditionalInfo(): String {
        return buildString {
            if (addThreadInfo) append(getThreadInfoString())
            if (addTraceInfo) append(trace)
        }
    }
}
package com.tekmoon.logger.domain

abstract class LogWriter(
){
    open fun isLoggable(severity: Severity): Boolean = true

    abstract fun processLog(message: String, severity: Severity): String

    abstract fun getWriteConfigInfo(): String
}
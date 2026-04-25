package com.tekmoon.data

expect fun platform(): String

val apiKey get() = FrameworkConfig.apiKey
val url get() = FrameworkConfig.apiBaseUrl
val isPro get() = FrameworkConfig.isPro
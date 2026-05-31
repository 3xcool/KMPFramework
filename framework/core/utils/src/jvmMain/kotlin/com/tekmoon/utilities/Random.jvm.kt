package com.tekmoon.utilities

import java.util.UUID

actual fun randomUUID(): String = UUID.randomUUID().toString()

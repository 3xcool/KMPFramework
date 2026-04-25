package com.tekmoon.domain.util

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()
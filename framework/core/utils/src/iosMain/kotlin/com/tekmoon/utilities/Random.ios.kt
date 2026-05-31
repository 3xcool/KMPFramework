package com.tekmoon.utilities

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()

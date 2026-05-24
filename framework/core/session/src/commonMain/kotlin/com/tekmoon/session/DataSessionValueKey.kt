package com.tekmoon.session

import kotlinx.serialization.KSerializer

/**
 * Typed key for feature-level saved state inside a session.
 */
interface DataSessionValueKey<T : Any> {
    val key: String
    val serializer: KSerializer<T>
    val defaultValue: T
}

data class SimpleSessionValueKey<T : Any>(
    override val key: String,
    override val serializer: KSerializer<T>,
    override val defaultValue: T,
) : DataSessionValueKey<T>

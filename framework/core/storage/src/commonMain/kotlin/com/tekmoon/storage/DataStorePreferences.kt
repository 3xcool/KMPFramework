package com.tekmoon.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences as DsPrefs

internal class DataStorePreferences(
    private val dataStore: DataStore<DsPrefs>,
) : Preferences {

    override fun getString(key: String, default: String?): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey(key)] ?: default }

    override fun getInt(key: String, default: Int): Flow<Int> =
        dataStore.data.map { it[intPreferencesKey(key)] ?: default }

    override fun getLong(key: String, default: Long): Flow<Long> =
        dataStore.data.map { it[longPreferencesKey(key)] ?: default }

    override fun getBoolean(key: String, default: Boolean): Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(key)] ?: default }

    override fun getFloat(key: String, default: Float): Flow<Float> =
        dataStore.data.map { it[floatPreferencesKey(key)] ?: default }

    override fun getDouble(key: String, default: Double): Flow<Double> =
        dataStore.data.map { it[doublePreferencesKey(key)] ?: default }

    override fun getStringSet(key: String, default: Set<String>): Flow<Set<String>> =
        dataStore.data.map { it[stringSetPreferencesKey(key)] ?: default }

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun putInt(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    override suspend fun putLong(key: String, value: Long) {
        dataStore.edit { it[longPreferencesKey(key)] = value }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    override suspend fun putFloat(key: String, value: Float) {
        dataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    override suspend fun putDouble(key: String, value: Double) {
        dataStore.edit { it[doublePreferencesKey(key)] = value }
    }

    override suspend fun putStringSet(key: String, value: Set<String>) {
        dataStore.edit { it[stringSetPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        // DataStore Preferences keys are name-based; the typed wrapper only annotates retrieval.
        // Removing via any typed key with the right name clears the slot regardless of what
        // type was stored. Using stringPreferencesKey here is arbitrary.
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override suspend fun contains(key: String): Boolean {
        val snapshot = dataStore.data.first()
        return snapshot.asMap().keys.any { it.name == key }
    }

    override val keys: Flow<Set<String>> =
        dataStore.data.map { snapshot -> snapshot.asMap().keys.map { it.name }.toSet() }
}

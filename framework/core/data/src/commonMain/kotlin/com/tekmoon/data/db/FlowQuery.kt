package com.tekmoon.data.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Converts a SQLDelight [Query] into a [Flow] that emits a fresh [List] whenever
 * the underlying table is modified.
 *
 * Internally uses [app.cash.sqldelight.coroutines.asFlow] + [mapToList] from the
 * `coroutines-extensions` artifact.
 *
 * @param dispatcher The dispatcher on which query execution runs.
 *                   Defaults to [Dispatchers.Default]; swap in a test dispatcher
 *                   for unit tests.
 */
fun <T : Any> Query<T>.asFlowList(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Flow<List<T>> = asFlow().mapToList(dispatcher)

/**
 * Converts a SQLDelight [Query] that always returns exactly one row into a [Flow].
 *
 * Emits the latest value whenever the table changes. Throws if the query returns
 * no rows (use [asFlowOneOrNull] when the row might be absent).
 */
fun <T : Any> Query<T>.asFlowOne(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Flow<T> = asFlow().mapToOne(dispatcher)

/**
 * Converts a SQLDelight [Query] that may return zero or one rows into a [Flow].
 *
 * Emits `null` when the row is absent, or the latest value when it exists.
 */
fun <T : Any> Query<T>.asFlowOneOrNull(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Flow<T?> = asFlow().mapToOneOrNull(dispatcher)

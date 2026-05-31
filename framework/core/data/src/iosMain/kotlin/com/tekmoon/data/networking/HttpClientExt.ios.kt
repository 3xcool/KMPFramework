package com.tekmoon.data.networking

import com.tekmoon.domain.util.data.ClientError
import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.NoInternet
import com.tekmoon.domain.util.data.Result
import com.tekmoon.domain.util.data.Serialization
import com.tekmoon.domain.util.data.UnknownError
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import platform.Foundation.NSURLErrorCallIsActive
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorResourceUnavailable
import platform.Foundation.NSURLErrorTimedOut
import kotlin.coroutines.coroutineContext

actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError>
): Result<T, DataError> {
    return try {
        val response = execute()
        handleResponse(response)
    } catch (e: DarwinHttpRequestException) {
        handleDarwinException(e)
    } catch (_: UnresolvedAddressException) {
        Result.Failure(NoInternet)
    } catch (_: HttpRequestTimeoutException) {
        Result.Failure(ClientError.Timeout)
    } catch (_: SerializationException) {
        Result.Failure(Serialization)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        // Final safety net: any unexpected exception is mapped to UnknownError
        // while preserving the cause via UnknownError(e). Coroutine cancellation
        // is re-thrown via ensureActive() so it can't be silently swallowed here.
        coroutineContext.ensureActive()
        Result.Failure(UnknownError(e))
    }
}

private fun handleDarwinException(e: DarwinHttpRequestException): Result<Nothing, DataError> {
    val nsError = e.origin
    return if (nsError.domain == NSURLErrorDomain) {
        when (nsError.code) {
            NSURLErrorNotConnectedToInternet,
            NSURLErrorNetworkConnectionLost,
            NSURLErrorCannotFindHost,
            NSURLErrorDNSLookupFailed,
            NSURLErrorResourceUnavailable,
            NSURLErrorInternationalRoamingOff,
            NSURLErrorCallIsActive,
            NSURLErrorDataNotAllowed -> Result.Failure(NoInternet)
            NSURLErrorTimedOut -> Result.Failure(ClientError.Timeout)
            else -> Result.Failure(UnknownError())
        }
    } else Result.Failure(UnknownError())
}

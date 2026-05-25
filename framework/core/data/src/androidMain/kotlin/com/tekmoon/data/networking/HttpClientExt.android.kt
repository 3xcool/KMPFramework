package com.tekmoon.data.networking

import com.tekmoon.domain.util.data.ClientError
import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.NoInternet
import com.tekmoon.domain.util.data.Result
import com.tekmoon.domain.util.data.Serialization
import com.tekmoon.domain.util.data.UnknownError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError>
): Result<T, DataError> {
    return try {
        val response = execute()
        handleResponse(response)
    } catch (e: UnknownHostException) {
        Result.Failure(NoInternet)
    } catch (e: UnresolvedAddressException) {
        Result.Failure(NoInternet)
    } catch (e: ConnectException) {
        Result.Failure(NoInternet)
    } catch (e: SocketTimeoutException) {
        Result.Failure(ClientError.Timeout)
    } catch (e: HttpRequestTimeoutException) {
        Result.Failure(ClientError.Timeout)
    } catch (e: SerializationException) {
        Result.Failure(Serialization)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        Result.Failure(UnknownError(e))
    }
}

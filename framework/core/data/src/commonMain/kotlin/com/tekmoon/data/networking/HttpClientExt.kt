package com.tekmoon.data.networking

import com.tekmoon.domain.util.data.ClientError
import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.Result
import com.tekmoon.domain.util.data.Serialization
import com.tekmoon.domain.util.data.ServerError
import com.tekmoon.domain.util.data.UnknownError
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse

expect suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError>
): Result<T, DataError>

suspend inline fun <reified Request, reified Response: Any> HttpClient.post(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    body: Request,
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Result<Response, DataError> {
    return safeCall {
        post {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }
}

suspend inline fun <reified Response: Any> HttpClient.get(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Result<Response, DataError> {
    return safeCall {
        get {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }
}

suspend inline fun <reified Response: Any> HttpClient.delete(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Result<Response, DataError> {
    return safeCall {
        delete {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }
}

suspend inline fun <reified Request, reified Response: Any> HttpClient.put(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    body: Request,
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Result<Response, DataError> {
    return safeCall {
        put {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        } as HttpResponse
    }
}

suspend inline fun <reified T> safeCall(
    noinline execute: suspend () -> HttpResponse
): Result<T, DataError> {
    return platformSafeCall(
        execute = execute
    ) { response ->
        responseToResult(response)
    }
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (_: NoTransformationFoundException) {
                Result.Failure(Serialization)
            }
        }
        400 -> Result.Failure(ClientError.BadRequest)
        401 -> Result.Failure(ClientError.Unauthorized)
        403 -> Result.Failure(ClientError.Forbidden)
        404 -> Result.Failure(ClientError.NotFound)
        408 -> Result.Failure(ClientError.Timeout)
        409 -> Result.Failure(ClientError.Conflict)
        413 -> Result.Failure(ClientError.PayloadTooLarge)
        429 -> Result.Failure(ClientError.TooManyRequests)
        500 -> Result.Failure(ServerError.InternalError)
        503 -> Result.Failure(ServerError.ServiceUnavailable)
        else -> Result.Failure(UnknownError())
    }
}

fun constructRoute(route: String): String {
    return when {
        route.contains(UrlConstants.BASE_URL_HTTP) -> route
        route.startsWith("/") -> "${UrlConstants.BASE_URL_HTTP}$route"
        else -> "${UrlConstants.BASE_URL_HTTP}/$route"
    }
}

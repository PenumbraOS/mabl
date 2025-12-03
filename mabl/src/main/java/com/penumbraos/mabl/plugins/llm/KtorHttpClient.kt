package com.penumbraos.mabl.plugins.llm

import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.http.ktor.HttpClientPlugin
import dev.langchain4j.exception.HttpException
import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpMethod
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser
import io.ktor.client.call.body
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.util.toMap
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class KtorHttpClient : HttpClient {
    private val coroutineScope: CoroutineScope
    private val ktorClient: io.ktor.client.HttpClient

    constructor(coroutineScope: CoroutineScope, penumbraClient: PenumbraClient) {
        this.coroutineScope = coroutineScope
        this.ktorClient = io.ktor.client.HttpClient {
            // Otherwise ktor strips ContentType
            useDefaultTransformers = false
            install(HttpClientPlugin) {
                this.penumbraClient = penumbraClient
            }
            install(SSE)
        }
    }

    override fun execute(request: HttpRequest): SuccessfulHttpResponse {
        return runBlocking {
            val response = ktorClient.request(request.url()) {
                buildRequest(this, request)
            }

            buildResponse(response, true)
        }
    }

    override fun execute(
        request: HttpRequest,
        parser: ServerSentEventParser,
        listener: ServerSentEventListener
    ) {
        coroutineScope.launch {
            ktorClient.prepareRequest {
                buildRequest(this, request)
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    try {
                        listener.onError(HttpException(response.status.value, response.body()))
                    } catch (_: Exception) {
                    }

                    return@execute
                }

                try {
                    listener.onOpen(buildResponse(response, false))
                } catch (_: Exception) {
                    return@execute
                }

                try {
                    val stream = response.bodyAsChannel().toInputStream()
                    parser.parse(stream, listener)
                    listener.onClose()
                } catch (e: Exception) {
                    listener.onError(e)
                }
            }
        }
    }

    private fun buildRequest(builder: HttpRequestBuilder, langChainRequest: HttpRequest) {
        builder.url(langChainRequest.url())
        builder.method = when (langChainRequest.method()) {
            HttpMethod.GET -> io.ktor.http.HttpMethod.Get
            HttpMethod.POST -> io.ktor.http.HttpMethod.Post
            HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
        }
        for ((key, values) in langChainRequest.headers()) {
            builder.headers.appendAll(key, values)
        }
        val contentTypeString = langChainRequest.headers()["ContentType"]?.first() ?: ""

        val contentType = try {
            ContentType.parse(contentTypeString)
        } catch (_: Exception) {
            ContentType.Application.Json
        }

        builder.setBody(
            TextContent(
                langChainRequest.body(),
                contentType
            )
        )
    }

    private suspend fun buildResponse(
        response: HttpResponse,
        withBody: Boolean
    ): SuccessfulHttpResponse {
        val builder = SuccessfulHttpResponse.builder()
            .statusCode(response.status.value)
            .headers(response.headers.toMap())

        if (withBody) {
            builder.body(response.body())
        }

        return builder.build()
    }
}
package com.penumbraos.plugins.openai

import com.penumbraos.sdk.PenumbraClient
import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpClientBuilder
import kotlinx.coroutines.CoroutineScope
import java.time.Duration

class KtorHttpClientBuilder(coroutineScope: CoroutineScope, penumbraClient: PenumbraClient) :
    HttpClientBuilder {
    val client = KtorHttpClient(coroutineScope, penumbraClient)

    override fun connectTimeout(): Duration? {
        // TODO
        return Duration.ZERO
    }

    override fun connectTimeout(timeout: Duration?): HttpClientBuilder {
        // TODO
        return this
    }

    override fun readTimeout(): Duration? {
        // TODO
        return Duration.ZERO
    }

    override fun readTimeout(timeout: Duration?): HttpClientBuilder {
        // TODO
        return this
    }

    override fun build(): HttpClient {
        return client
    }
}
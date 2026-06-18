package com.pixies.recetario.data.remote

import com.pixies.recetario.domain.exception.QuotaExhaustedException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun buildSpoonacularClient(engine: HttpClientEngine, apiKey: String): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
    }.also { client -> installInterceptor(client, apiKey) }

private fun installInterceptor(client: HttpClient, apiKey: String) {
    client.plugin(HttpSend).intercept { request ->
        request.url.parameters.append("apiKey", apiKey)
        val call = execute(request)
        val quota = call.response.headers[QUOTA_HEADER]?.toIntOrNull() ?: Int.MAX_VALUE
        if (quota == 0) throw QuotaExhaustedException()
        call
    }
}

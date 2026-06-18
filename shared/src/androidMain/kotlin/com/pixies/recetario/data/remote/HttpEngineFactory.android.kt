package com.pixies.recetario.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun httpEngine(): HttpClientEngine = OkHttp.create()

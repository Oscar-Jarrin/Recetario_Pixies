package com.pixies.recetario.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual fun httpEngine(): HttpClientEngine = CIO.create()

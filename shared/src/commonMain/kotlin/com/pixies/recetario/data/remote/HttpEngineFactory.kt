package com.pixies.recetario.data.remote

import io.ktor.client.engine.HttpClientEngine

expect fun httpEngine(): HttpClientEngine

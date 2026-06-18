package com.pixies.recetario

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.pixies.recetario.di.AppModule

private val module = AppModule(apiKey = System.getProperty("SPOONACULAR_API_KEY") ?: "")

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RecetarioPixies",
        state = WindowState(placement = WindowPlacement.Maximized)
    ) {
        App(module)
    }
}

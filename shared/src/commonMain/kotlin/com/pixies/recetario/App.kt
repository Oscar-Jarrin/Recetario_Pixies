package com.pixies.recetario

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.pixies.recetario.di.DependencyInjector
import com.pixies.recetario.presentation.navigation.AppNavGraph

@Composable
fun App(module: DependencyInjector) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).build()
    }
    MaterialTheme { AppNavGraph(module) }
}

package com.pixies.recetario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pixies.recetario.di.DependencyInjector

class MainActivity : ComponentActivity() {
    private val module by lazy {
        DependencyInjector(apiKey = BuildConfig.SPOONACULAR_API_KEY, context = applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App(module) }
    }
}

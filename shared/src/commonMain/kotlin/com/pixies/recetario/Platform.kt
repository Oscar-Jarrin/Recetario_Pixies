package com.pixies.recetario

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
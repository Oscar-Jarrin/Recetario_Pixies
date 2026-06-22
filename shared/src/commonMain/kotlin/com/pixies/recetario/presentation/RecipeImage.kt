package com.pixies.recetario.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import recetariopixies.shared.generated.resources.Res
import recetariopixies.shared.generated.resources.default_recipe

@Composable
fun RecipeImage(imageUrl: String, contentDescription: String, modifier: Modifier = Modifier) {
    val fallback = painterResource(Res.drawable.default_recipe)
    AsyncImage(
        model = imageUrl.ifEmpty { null },
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        fallback = fallback,
        error = fallback,
        modifier = modifier
    )
}

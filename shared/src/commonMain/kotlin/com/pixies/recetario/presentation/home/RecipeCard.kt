package com.pixies.recetario.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.presentation.RecipeImage

internal const val CARD_PADDING_DP = 4
internal const val CARD_CONTENT_PADDING_DP = 8
internal const val IMAGE_HEIGHT_DP = 200
internal const val TITLE_MAX_LINES = 2
internal const val MINUTES_SUFFIX = " min"

@Composable
fun RecipeCard(recipe: RecipeOverview, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(CARD_PADDING_DP.dp).fillMaxWidth()
    ) {
        Column {
            RecipeImage(
                imageUrl = recipe.imageUrl,
                contentDescription = recipe.title,
                modifier = Modifier.fillMaxWidth().height(IMAGE_HEIGHT_DP.dp)
            )
            Column(modifier = Modifier.padding(CARD_CONTENT_PADDING_DP.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = TITLE_MAX_LINES
                )
                Text(
                    text = recipe.readyInMinutes.toString() + MINUTES_SUFFIX,
                    style = MaterialTheme.typography.bodySmall
                )
                if (recipe.dishType.isNotEmpty()) {
                    Text(text = recipe.dishType, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

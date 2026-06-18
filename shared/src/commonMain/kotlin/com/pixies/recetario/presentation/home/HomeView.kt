package com.pixies.recetario.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pixies.recetario.domain.model.RecipeOverview

private const val GRID_COLUMNS = 2
private const val GRID_PADDING_DP = 8
private const val CARD_PADDING_DP = 4
private const val CARD_CONTENT_PADDING_DP = 8
private const val IMAGE_HEIGHT_DP = 200
private const val RETRY_LABEL = "Retry"
private const val TITLE_MAX_LINES = 2
private const val MINUTES_SUFFIX = " min"
private const val SEARCH_H_PADDING_DP = 16
private const val SEARCH_V_PADDING_DP = 8
private const val SEARCH_SPACING_DP = 8
private const val SEARCH_PLACEHOLDER = "e.g. flour, sugar, egg"
private const val SEARCH_LABEL = "Search"

@Composable
fun HomeView(viewModel: HomeViewModel, onRecipeClick: (Int) -> Unit, onSearchClick: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        InlineSearchBar(onSearch = onSearchClick)
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (val s = state) {
                HomeState.Loading -> LoadingContent()
                is HomeState.Success -> RecipeGrid(s.recipes, onRecipeClick)
                is HomeState.Error -> ErrorContent(
                    message = s.exception.message ?: "Unknown error",
                    onRetry = viewModel::retry
                )
            }
        }
    }
}

@Composable
private fun InlineSearchBar(onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SEARCH_H_PADDING_DP.dp, vertical = SEARCH_V_PADDING_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(SEARCH_SPACING_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(SEARCH_PLACEHOLDER) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Button(
            onClick = { if (query.isNotBlank()) onSearch(query) },
        ) { Text(SEARCH_LABEL) }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RecipeGrid(recipes: List<RecipeOverview>, onRecipeClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize().padding(GRID_PADDING_DP.dp)
    ) {
        items(recipes) { recipe ->
            RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe.id) })
        }
    }
}

@Composable
private fun RecipeCard(recipe: RecipeOverview, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(CARD_PADDING_DP.dp).fillMaxWidth()
    ) {
        Column {
            AsyncImage(
                model = recipe.imageUrl.ifEmpty { null },
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
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

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry) { Text(RETRY_LABEL) }
    }
}

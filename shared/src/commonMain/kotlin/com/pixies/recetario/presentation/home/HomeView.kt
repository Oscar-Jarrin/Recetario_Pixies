package com.pixies.recetario.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.presentation.RETRY_LABEL

private const val GRID_COLUMNS = 2
private const val GRID_PADDING_DP = 8
private const val SEARCH_V_PADDING_DP = 8

@Composable
fun HomeView(viewModel: HomeViewModel, onRecipeClick: (RecipeOverview) -> Unit, onSearchClick: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Recetario Pixies",
            style = MaterialTheme.typography.displayLarge,
            fontFamily = FontFamily.Cursive,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SEARCH_V_PADDING_DP.dp),
            textAlign = TextAlign.Center
        )
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
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RecipeGrid(recipes: List<RecipeOverview>, onRecipeClick: (RecipeOverview) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize().padding(GRID_PADDING_DP.dp)
    ) {
        items(recipes) { recipe ->
            RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe) })
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
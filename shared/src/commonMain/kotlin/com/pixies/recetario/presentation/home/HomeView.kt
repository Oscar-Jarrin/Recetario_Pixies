package com.pixies.recetario.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.presentation.RETRY_LABEL
import com.pixies.recetario.presentation.SEARCH_LABEL
import com.pixies.recetario.presentation.SEARCH_PLACEHOLDER

private const val GRID_COLUMNS = 2
private const val GRID_PADDING_DP = 8
private const val SEARCH_H_PADDING_DP = 16
private const val SEARCH_V_PADDING_DP = 8
private const val SEARCH_SPACING_DP = 8

@Composable
fun HomeView(viewModel: HomeViewModel, onRecipeClick: (RecipeOverview) -> Unit, onSearchClick: (String) -> Unit) {
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
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(query) })
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
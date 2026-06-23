package com.pixies.recetario.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pixies.recetario.domain.model.IngredientSearchResult
import com.pixies.recetario.presentation.RecipeImage
import com.pixies.recetario.presentation.RETRY_LABEL
import com.pixies.recetario.presentation.SEARCH_LABEL
import com.pixies.recetario.presentation.SEARCH_PLACEHOLDER

private const val SCREEN_PADDING_DP = 16
private const val ITEM_SPACING_DP = 8
private const val CHIP_SPACING_DP = 4
private const val CARD_IMAGE_WIDTH_DP = 230
private const val CARD_IMAGE_HEIGHT_DP = 230
private const val IDLE_HINT = "Enter ingredients separated by commas"
private val COLOR_USED = Color(0xFF2E7D32)
private val COLOR_MISSING = Color(0xFFC62828)
private val COLOR_UNUSED = Color(0xFF757575)

@Composable
fun SearchView(viewModel: SearchViewModel, initialQuery: String = "", onRecipeClick: (IngredientSearchResult) -> Unit) {
    val query by viewModel.currentQuery.collectAsState()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && viewModel.currentQuery.value.isBlank()) {
            viewModel.updateQuery(initialQuery)
            viewModel.search(initialQuery)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING_DP.dp)) {
        SearchBar(
            query = query,
            onQueryChange = { viewModel.updateQuery(it) },
            onSearch = { viewModel.search(query) }
        )
        when (val s = state) {
            SearchState.Idle -> IdleContent()
            SearchState.Loading -> LoadingContent()
            is SearchState.Success -> ResultsList(s.results, onRecipeClick)
            is SearchState.Error -> SearchErrorContent(
                message = s.exception.message ?: "Unknown error",
                onRetry = { viewModel.search(query) }
            )
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = ITEM_SPACING_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(SEARCH_PLACEHOLDER) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Button(onClick = onSearch) { Text(SEARCH_LABEL) }
    }
}

@Composable
private fun IdleContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(IDLE_HINT, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ResultsList(results: List<IngredientSearchResult>, onRecipeClick: (IngredientSearchResult) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results) { result ->
            IngredientResultCard(result = result, onClick = { onRecipeClick(result) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientResultCard(result: IngredientSearchResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(CHIP_SPACING_DP.dp)
    ) {
        Row {
            Column(
                modifier = Modifier.weight(1f).padding(ITEM_SPACING_DP.dp),
                verticalArrangement = Arrangement.spacedBy(CHIP_SPACING_DP.dp)
            ) {
                Text(text = result.title, style = MaterialTheme.typography.titleSmall)
                IngredientChipGroup("Similar used Ingredients", result.usedIngredients, COLOR_USED)
                IngredientChipGroup("Requested but not used", result.unusedIngredients, COLOR_UNUSED)
                IngredientChipGroup("Not requested but used", result.missingIngredients, COLOR_MISSING)
            }
            RecipeImage(
                imageUrl = result.imageUrl,
                contentDescription = result.title,
                modifier = Modifier.width(CARD_IMAGE_WIDTH_DP.dp).height(CARD_IMAGE_HEIGHT_DP.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientChipGroup(label: String, ingredients: List<String>, labelColor: Color) {
    if (ingredients.isEmpty()) return
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING_DP.dp)) {
            ingredients.forEach { name ->
                SuggestionChip(onClick = {}, label = { Text(name) })
            }
        }
    }
}

@Composable
private fun SearchErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry) { Text(RETRY_LABEL) }
    }
}

package com.pixies.recetario.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixies.recetario.presentation.RETRY_LABEL

private const val SCREEN_PADDING_DP = 16
private const val STEP_PADDING_DP = 8
private const val GROUP_PADDING_DP = 12

@Composable
fun RecipeDetailView(viewModel: RecipeDetailViewModel, recipeId: Int) {
    LaunchedEffect(recipeId) { viewModel.load(recipeId) }
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            DetailState.Loading -> CircularProgressIndicator()
            is DetailState.Success -> InstructionsList(s)
            is DetailState.Error -> ErrorContent(
                message = s.exception.message ?: "Unknown error",
                onRetry = { viewModel.retry(recipeId) }
            )
        }
    }
}

@Composable
private fun InstructionsList(state: DetailState.Success) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING_DP.dp)) {
        state.instructions.instructions.forEach { (groupName, steps) ->
            if (groupName.isNotEmpty()) {
                item {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = GROUP_PADDING_DP.dp, bottom = STEP_PADDING_DP.dp)
                    )
                }
            }
            items(steps.size) { index ->
                InstructionGroupSection(stepNumber = index + 1, stepText = steps[index])
            }
        }
    }
}

@Composable
private fun InstructionGroupSection(stepNumber: Int, stepText: String) {
    Text(
        text = "$stepNumber. $stepText",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = STEP_PADDING_DP.dp)
    )
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry) { Text(RETRY_LABEL) }
    }
}

package com.pixies.recetario.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pixies.recetario.presentation.RecipeImage
import com.pixies.recetario.presentation.RETRY_LABEL

private const val SCREEN_PADDING_DP = 16
private const val STEP_PADDING_DP = 8
private const val GROUP_PADDING_DP = 12
private const val HEADER_IMAGE_SIZE_DP = 100
private const val HEADER_IMAGE_CORNER_DP = 12
private const val HEADER_SPACING_DP = 12

@Composable
fun RecipeDetailView(
    viewModel: RecipeDetailViewModel,
    recipeId: Int,
    recipeTitle: String,
    recipeImageUrl: String,
    onBack: () -> Unit
) {
    LaunchedEffect(recipeId) { viewModel.load(recipeId) }
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onBack,
            modifier = Modifier.padding(SCREEN_PADDING_DP.dp)
        ) {
            Text("Atras")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = SCREEN_PADDING_DP.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                DetailState.Loading -> CircularProgressIndicator()
                is DetailState.Success -> InstructionsList(s, recipeTitle, recipeImageUrl)
                is DetailState.Error -> ErrorContent(
                    message = s.exception.message ?: "Unknown error",
                    onRetry = { viewModel.retry(recipeId) }
                )
            }
        }
    }
}

@Composable
private fun InstructionsList(state: DetailState.Success, recipeTitle: String, recipeImageUrl: String) {
    val checkedSteps = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { RecipeHeader(imageUrl = recipeImageUrl, title = recipeTitle) }
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
                val key = "$groupName:$index"
                InstructionGroupSection(
                    stepNumber = index + 1,
                    stepText = steps[index],
                    checked = checkedSteps[key] == true,
                    onCheckedChange = { checkedSteps[key] = it }
                )
            }
        }
    }
}

@Composable
private fun RecipeHeader(imageUrl: String, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GROUP_PADDING_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(HEADER_SPACING_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RecipeImage(
            imageUrl = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(HEADER_IMAGE_SIZE_DP.dp)
                .clip(RoundedCornerShape(HEADER_IMAGE_CORNER_DP.dp))
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun InstructionGroupSection(
    stepNumber: Int,
    stepText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = STEP_PADDING_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = "$stepNumber. $stepText",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            modifier = Modifier.weight(1f)
        )
    }
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

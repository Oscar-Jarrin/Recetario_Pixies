package com.pixies.recetario.presentation.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixies.recetario.domain.model.IngredientSearchResult
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.presentation.RETRY_LABEL
import com.pixies.recetario.presentation.search.SearchView
import com.pixies.recetario.presentation.search.SearchViewModel

private val DAY_LABELS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private const val ROW_PADDING_DP = 8
private const val SECTION_PADDING_DP = 16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailView(
    viewModel: PlannerViewModel,
    searchViewModel: SearchViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.detailState.collectAsState()
    var pickingForDay by remember { mutableStateOf<Int?>(null) }

    when (val s = state) {
        PlanDetailState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is PlanDetailState.Error -> DetailError(
            message = s.exception.message ?: "Error",
            onRetry = onBack
        )
        is PlanDetailState.Success -> {
            DayGrid(
                plan = s.plan,
                onSlotClick = { dayIndex -> pickingForDay = dayIndex },
                onRemoveSlot = viewModel::removeRecipe
            )
            pickingForDay?.let { dayIndex ->
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { pickingForDay = null },
                    sheetState = sheetState
                ) {
                    SearchView(
                        viewModel = searchViewModel,
                        onRecipeClick = { result ->
                            viewModel.assignRecipe(dayIndex, result.toRecipeOverview())
                            pickingForDay = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayGrid(
    plan: WeeklyPlan,
    onSlotClick: (Int) -> Unit,
    onRemoveSlot: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = plan.planName,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(SECTION_PADDING_DP.dp)
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize().padding(ROW_PADDING_DP.dp)) {
            items(DAY_LABELS.indices.toList()) { dayIndex ->
                DaySlotRow(
                    label = DAY_LABELS[dayIndex],
                    recipe = plan.daySlots[dayIndex],
                    onAddClick = { onSlotClick(dayIndex) },
                    onRemoveClick = { onRemoveSlot(dayIndex) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DaySlotRow(
    label: String,
    recipe: RecipeOverview?,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ROW_PADDING_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = ROW_PADDING_DP.dp)
        )
        if (recipe != null) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRemoveClick) { Text("Quitar") }
        } else {
            TextButton(onClick = onAddClick, modifier = Modifier.weight(1f)) {
                Text("+ Agregar receta")
            }
        }
    }
}

@Composable
private fun DetailError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry) { Text(RETRY_LABEL) }
    }
}

private fun IngredientSearchResult.toRecipeOverview() = RecipeOverview(
    id = id,
    title = title,
    imageUrl = imageUrl,
    readyInMinutes = 0,
    dishType = ""
)

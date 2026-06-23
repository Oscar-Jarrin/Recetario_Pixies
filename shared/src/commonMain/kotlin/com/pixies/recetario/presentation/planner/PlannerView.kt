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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.presentation.RETRY_LABEL
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

private const val FAB_PADDING_DP = 16
private const val CARD_PADDING_DP = 8
private const val CARD_CONTENT_PADDING_DP = 12
private const val PLAN_LIST_PADDING_DP = 8

@Composable
fun PlannerView(viewModel: PlannerViewModel, onPlanClick: (WeeklyPlan) -> Unit) {
    val state by viewModel.plannerState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            PlannerState.Loading -> PlannerLoading()
            is PlannerState.Success -> PlanList(s.plans, onPlanClick, viewModel::deletePlan)
            is PlannerState.Error -> PlannerError(
                message = s.exception.message ?: "Error loading plans",
                onRetry = viewModel::loadAll
            )
        }
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(FAB_PADDING_DP.dp)
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showCreateDialog) {
        CreatePlanDialog(
            onConfirm = { name -> viewModel.createPlan(name); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun PlanList(
    plans: List<WeeklyPlan>,
    onPlanClick: (WeeklyPlan) -> Unit,
    onDeletePlan: (Long) -> Unit
) {
    if (plans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No plans yet. Tap + to create one.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(PLAN_LIST_PADDING_DP.dp)) {
        items(plans, key = { it.id }) { plan ->
            PlanRow(plan = plan, onClick = { onPlanClick(plan) }, onDelete = { onDeletePlan(plan.id) })
        }
    }
}

@Composable
private fun PlanRow(plan: WeeklyPlan, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(CARD_PADDING_DP.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CARD_CONTENT_PADDING_DP.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = plan.planName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${plan.daySlots.size}/7 days planned",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDelete) { Text("Borrar") }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CreatePlanDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New plan") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Plan name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                        keyboardController?.hide()
                    }
                }),
                modifier = Modifier.onKeyEvent {
                    if (it.key == Key.Enter && name.isNotBlank()) {
                        onConfirm(name.trim())
                        keyboardController?.hide()
                        true
                    } else {
                        false
                    }
                }
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PlannerLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlannerError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry) { Text(RETRY_LABEL) }
    }
}
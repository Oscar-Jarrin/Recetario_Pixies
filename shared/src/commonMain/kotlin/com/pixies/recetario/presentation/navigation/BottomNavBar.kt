package com.pixies.recetario.presentation.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class TabItem(val screen: Screen, val label: String)

private val TABS = listOf(
    TabItem(Screen.Home, "Inicio"),
    TabItem(Screen.Search, "Buscar"),
    TabItem(Screen.Planner, "Planeador")
)

@Composable
fun BottomNavBar(currentRoute: String?, onTabSelected: (Screen) -> Unit) {
    NavigationBar {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.screen.route,
                onClick = { onTabSelected(tab.screen) },
                icon = { androidx.compose.foundation.layout.Box(Modifier.size(0.dp)) },
                label = { Text(tab.label) }
            )
        }
    }
}

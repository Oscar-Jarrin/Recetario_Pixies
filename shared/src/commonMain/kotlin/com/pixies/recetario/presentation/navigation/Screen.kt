package com.pixies.recetario.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Planner : Screen("planner")

    companion object {
        val tabScreens: List<Screen> = listOf(Home, Search, Planner)
    }

    data class RecipeDetail(val id: Int) : Screen("recipe/$id") {
        companion object {
            const val ROUTE = "recipe/{id}"
            const val ARG_ID = "id"
        }
    }

    data class PlannerDetail(val planId: Long) : Screen("planner/$planId") {
        companion object {
            const val ROUTE = "planner/{planId}"
            const val ARG_PLAN_ID = "planId"
        }
    }
}

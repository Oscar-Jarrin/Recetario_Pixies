package com.pixies.recetario.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixies.recetario.di.AppModule
import com.pixies.recetario.presentation.home.HomeView
import com.pixies.recetario.presentation.home.HomeViewModel
import com.pixies.recetario.presentation.search.SearchView
import com.pixies.recetario.presentation.search.SearchViewModel

@Composable
fun AppNavGraph(module: AppModule, navController: NavHostController = rememberNavController()) {
    var pendingSearchQuery by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel { HomeViewModel(module.getRandomRecipesUseCase) }
            HomeView(
                viewModel = viewModel,
                onRecipeClick = { id -> navController.navigate(Screen.RecipeDetail(id).route) },
                onSearchClick = { query ->
                    pendingSearchQuery = query
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        composable(Screen.Search.route) {
            val viewModel: SearchViewModel = viewModel { SearchViewModel(module.searchRecipesByIngredientsUseCase) }
            SearchView(
                viewModel = viewModel,
                initialQuery = pendingSearchQuery,
                onRecipeClick = { id -> navController.navigate(Screen.RecipeDetail(id).route) }
            )
        }
        composable(Screen.RecipeDetail.ROUTE) { }
        composable(Screen.Planner.route) { }
        composable(Screen.PlannerDetail.ROUTE) { }
    }
}

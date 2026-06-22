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
import com.pixies.recetario.presentation.detail.RecipeDetailView
import com.pixies.recetario.presentation.detail.RecipeDetailViewModel
import com.pixies.recetario.presentation.home.HomeView
import com.pixies.recetario.presentation.home.HomeViewModel
import com.pixies.recetario.presentation.search.SearchView
import com.pixies.recetario.presentation.search.SearchViewModel

@Composable
fun AppNavGraph(module: AppModule, navController: NavHostController = rememberNavController()) {
    var pendingSearchQuery by remember { mutableStateOf("") }
    var pendingRecipeId by remember { mutableStateOf(0) }
    var pendingRecipeTitle by remember { mutableStateOf("") }
    var pendingRecipeImageUrl by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel { HomeViewModel(module.getRandomRecipesUseCase) }
            HomeView(
                viewModel = viewModel,
                onRecipeClick = { recipe ->
                    pendingRecipeId = recipe.id
                    pendingRecipeTitle = recipe.title
                    pendingRecipeImageUrl = recipe.imageUrl
                    navController.navigate(Screen.RecipeDetail(recipe.id).route)
                },
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
                onRecipeClick = { result ->
                    pendingRecipeId = result.id
                    pendingRecipeTitle = result.title
                    pendingRecipeImageUrl = result.imageUrl
                    navController.navigate(Screen.RecipeDetail(result.id).route)
                }
            )
        }
        composable(Screen.RecipeDetail.ROUTE) {
            val id = pendingRecipeId.takeIf { it != 0 } ?: return@composable
            val viewModel: RecipeDetailViewModel =
                viewModel { RecipeDetailViewModel(module.getRecipeInstructionsUseCase) }
            RecipeDetailView(
                viewModel = viewModel,
                recipeId = id,
                recipeTitle = pendingRecipeTitle,
                recipeImageUrl = pendingRecipeImageUrl,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Planner.route) { }
        composable(Screen.PlannerDetail.ROUTE) { }
    }
}
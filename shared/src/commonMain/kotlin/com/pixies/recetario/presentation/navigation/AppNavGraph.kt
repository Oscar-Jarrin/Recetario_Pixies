package com.pixies.recetario.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixies.recetario.di.AppModule
import com.pixies.recetario.presentation.home.HomeView
import com.pixies.recetario.presentation.home.HomeViewModel

@Composable
fun AppNavGraph(module: AppModule, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel { HomeViewModel(module.getRandomRecipesUseCase) }
            HomeView(
                viewModel = viewModel,
                onRecipeClick = { id -> navController.navigate(Screen.RecipeDetail(id).route) }
            )
        }
        composable(Screen.Search.route) { }
        composable(Screen.RecipeDetail.ROUTE) { }
        composable(Screen.Planner.route) { }
        composable(Screen.PlannerDetail.ROUTE) { }
    }
}

package com.pixies.recetario.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixies.recetario.di.DependencyInjector
import com.pixies.recetario.presentation.detail.RecipeDetailView
import com.pixies.recetario.presentation.detail.RecipeDetailViewModel
import com.pixies.recetario.presentation.home.HomeView
import com.pixies.recetario.presentation.home.HomeViewModel
import com.pixies.recetario.presentation.planner.PlanDetailView
import com.pixies.recetario.presentation.planner.PlannerView
import com.pixies.recetario.presentation.planner.PlannerViewModel
import com.pixies.recetario.presentation.search.SearchView
import com.pixies.recetario.presentation.search.SearchViewModel

@Composable
fun AppNavGraph(module: DependencyInjector, navController: NavHostController = rememberNavController()) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val tabRoutes = setOf(Screen.Home.route, Screen.Search.route, Screen.Planner.route)
    val showBottomBar = currentRoute in tabRoutes

    var pendingSearchQuery by remember { mutableStateOf("") }
    var pendingRecipeId by remember { mutableStateOf(0) }
    var pendingRecipeTitle by remember { mutableStateOf("") }
    var pendingRecipeImageUrl by remember { mutableStateOf("") }

    val plannerViewModel: PlannerViewModel = viewModel {
        PlannerViewModel(
            getAllWeeklyPlans = module.getAllWeeklyPlansUseCase,
            saveWeeklyPlan = module.saveWeeklyPlanUseCase,
            deleteWeeklyPlan = module.deleteWeeklyPlanUseCase
        )
    }
    val plannerSearchViewModel: SearchViewModel = viewModel(key = "plannerSearch") {
        SearchViewModel(module.searchRecipesByIngredientsUseCase)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(currentRoute = currentRoute) { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
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
                val viewModel: SearchViewModel =
                    viewModel { SearchViewModel(module.searchRecipesByIngredientsUseCase) }
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
            composable(Screen.Planner.route) {
                PlannerView(
                    viewModel = plannerViewModel,
                    onPlanClick = { plan ->
                        plannerViewModel.selectPlan(plan)
                        navController.navigate("planner/${plan.id}")
                    }
                )
            }
            composable(Screen.PlannerDetail.ROUTE) {
                PlanDetailView(
                    viewModel = plannerViewModel,
                    searchViewModel = plannerSearchViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

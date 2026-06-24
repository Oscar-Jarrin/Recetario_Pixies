package com.pixies.recetario.di

import com.pixies.recetario.data.RecipeRepositoryImpl
import com.pixies.recetario.data.WeeklyPlanRepositoryImpl
import com.pixies.recetario.data.local.getDatabaseBuilder
import com.pixies.recetario.data.remote.KtorSpoonacularApiService
import com.pixies.recetario.data.remote.buildSpoonacularClient
import com.pixies.recetario.data.remote.httpEngine
import com.pixies.recetario.domain.repository.RecipeRepository
import com.pixies.recetario.domain.repository.WeeklyPlanRepository
import com.pixies.recetario.domain.usecase.DeleteWeeklyPlanUseCase
import com.pixies.recetario.domain.usecase.GetAllWeeklyPlansUseCase
import com.pixies.recetario.domain.usecase.GetRandomRecipesUseCase
import com.pixies.recetario.domain.usecase.GetRecipeInstructionsUseCase
import com.pixies.recetario.domain.usecase.SaveWeeklyPlanUseCase
import com.pixies.recetario.domain.usecase.SearchRecipesByIngredientsUseCase
import io.ktor.client.HttpClient

class AppModule(val apiKey: String, val context: Any? = null) {

    val httpClient: HttpClient = buildSpoonacularClient(httpEngine(), apiKey)

    private val database = getDatabaseBuilder(context).build()

    private val apiService = KtorSpoonacularApiService(httpClient)

    private val overviewDao = database.recipeOverviewDao()
    private val ingredientDao = database.recipeIngredientDao()
    private val stepDao = database.recipeInstructionStepDao()
    private val weeklyPlanDao = database.weeklyPlanDao()
    private val weeklyPlanSlotDao = database.weeklyPlanSlotDao()

    private val recipeRepository: RecipeRepository = RecipeRepositoryImpl(
        api = apiService,
        overviewDao = overviewDao,
        ingredientDao = ingredientDao,
        stepDao = stepDao
    )

    private val weeklyPlanRepository: WeeklyPlanRepository = WeeklyPlanRepositoryImpl(
        planDao = weeklyPlanDao,
        slotDao = weeklyPlanSlotDao,
        overviewDao = overviewDao
    )

    val getRandomRecipesUseCase = GetRandomRecipesUseCase(recipeRepository)
    val searchRecipesByIngredientsUseCase = SearchRecipesByIngredientsUseCase(recipeRepository)
    val getRecipeInstructionsUseCase = GetRecipeInstructionsUseCase(recipeRepository)
    val getAllWeeklyPlansUseCase = GetAllWeeklyPlansUseCase(weeklyPlanRepository)
    val saveWeeklyPlanUseCase = SaveWeeklyPlanUseCase(weeklyPlanRepository)
    val deleteWeeklyPlanUseCase = DeleteWeeklyPlanUseCase(weeklyPlanRepository)
}

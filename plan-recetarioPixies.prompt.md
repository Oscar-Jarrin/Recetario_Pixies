# Recetario_Pixies: Domain & Data Layer Specification

**Status:** Planning phase (ready for implementation)  
**Last Updated:** 2026-06-12  
**Architecture:** MVVM + Clean Architecture  
**Tech Stack:** Kotlin + Compose Desktop + SQLDelight + Ktor  

---

## 1. Environment Configuration

### File: `ENV_VARS` (project root)
```
SPOONACULAR_API_KEY=your_api_key_here
```
**Action:** Add to `.gitignore`

### File: `src/main/kotlin/core/EnvConfig.kt`
```kotlin
object EnvConfig {
    val SPOONACULAR_API_KEY: String = 
        System.getenv("SPOONACULAR_API_KEY") 
            ?: error("SPOONACULAR_API_KEY not found in environment")
}
```

---

## 2. Domain Layer

### 2.1 Domain Models

**File:** `src/main/kotlin/domain/model/Ingredient.kt`
```kotlin
data class Ingredient(
    val id: Long?,
    val name: String,
    val normalized: String
)
```

**File:** `src/main/kotlin/domain/model/Recipe.kt`
```kotlin
data class Recipe(
    val id: Long,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val popularityScore: Double?,
    val ingredients: List<Ingredient>,
    val instructionsSummary: String?,
    val createdAt: Long?
)
```

**File:** `src/main/kotlin/domain/model/RecipeInstructions.kt`
```kotlin
data class RecipeInstructions(
    val recipeId: Long,
    val steps: List<String>
)
```

**File:** `src/main/kotlin/domain/model/WeeklyPlanEntry.kt`
```kotlin
data class WeeklyPlanEntry(
    val dayOfWeek: Int,          // 1-7 (Monday-Sunday)
    val mealSlot: String,        // "breakfast", "lunch", "dinner"
    val recipeId: Long
)
```

**File:** `src/main/kotlin/domain/model/WeeklyPlan.kt`
```kotlin
data class WeeklyPlan(
    val weekStartEpochDay: Long,
    val entries: List<WeeklyPlanEntry>
)
```

### 2.2 Result Wrappers (Type-safe)

**File:** `src/main/kotlin/domain/result/ApiResult.kt`
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : ApiResult<Nothing>()
    data class QuotaExceeded(val remainingReset: Long = 0L) : ApiResult<Nothing>()
    data class StaleData<T>(val data: T, val message: String) : ApiResult<T>()
}
```

### 2.3 Repository Interface

**File:** `src/main/kotlin/domain/repository/RecipeRepository.kt`
```kotlin
interface RecipeRepository {
    // Search by ingredients
    fun getRecipesByIngredients(
        ingredients: List<String>,
        limit: Int = 20
    ): Flow<ApiResult<List<Recipe>>>
    
    suspend fun getRecipesByIngredientsOnce(
        ingredients: List<String>,
        limit: Int = 20
    ): ApiResult<List<Recipe>>
    
    // Random recipes
    fun getRandomRecipes(limit: Int = 10): Flow<ApiResult<List<Recipe>>>
    
    suspend fun getRandomRecipesOnce(limit: Int = 10): ApiResult<List<Recipe>>
    
    // Recipe details
    fun getRecipeInstructions(recipeId: Long): Flow<ApiResult<RecipeInstructions>>
    
    // Weekly plan
    suspend fun saveWeeklyPlan(plan: WeeklyPlan): ApiResult<Unit>
    
    fun getWeeklyPlan(weekStartEpochDay: Long): Flow<ApiResult<WeeklyPlan?>>
    
    // Cache management
    suspend fun clearExpiredCache(): ApiResult<Unit>
    
    suspend fun getApiQuotaStatus(): ApiResult<QuotaStatus>
    
    suspend fun manualSync(): ApiResult<SyncResult>
}

data class QuotaStatus(
    val requestsToday: Int,
    val remainingAllowance: Int,
    val quotaResetTime: Long,
    val canMakeRequests: Boolean
)

data class SyncResult(
    val recipesAdded: Int,
    val ingredientsAdded: Int,
    val requestsMade: Int
)
```

### 2.4 Use Cases

**File:** `src/main/kotlin/domain/usecase/GetRecipesByIngredientUsecase.kt`
```kotlin
class GetRecipesByIngredientUsecase(
    private val repository: RecipeRepository
) {
    operator fun invoke(
        ingredients: List<String>,
        limit: Int = 20
    ): Flow<ApiResult<List<Recipe>>> =
        repository.getRecipesByIngredients(ingredients, limit)
}
```

**File:** `src/main/kotlin/domain/usecase/GetRandomRecipesUsecase.kt`
```kotlin
class GetRandomRecipesUsecase(
    private val repository: RecipeRepository
) {
    operator fun invoke(limit: Int = 10): Flow<ApiResult<List<Recipe>>> =
        repository.getRandomRecipes(limit)
}
```

**File:** `src/main/kotlin/domain/usecase/GetRecipeInstructionsUsecase.kt`
```kotlin
class GetRecipeInstructionsUsecase(
    private val repository: RecipeRepository
) {
    operator fun invoke(recipeId: Long): Flow<ApiResult<RecipeInstructions>> =
        repository.getRecipeInstructions(recipeId)
}
```

**File:** `src/main/kotlin/domain/usecase/GetWeeklyPlanUsecase.kt`
```kotlin
class GetWeeklyPlanUsecase(
    private val repository: RecipeRepository
) {
    operator fun invoke(weekStartEpochDay: Long): Flow<ApiResult<WeeklyPlan?>> =
        repository.getWeeklyPlan(weekStartEpochDay)
}
```

**File:** `src/main/kotlin/domain/usecase/SaveWeeklyPlanUsecase.kt`
```kotlin
class SaveWeeklyPlanUsecase(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(plan: WeeklyPlan): ApiResult<Unit> =
        repository.saveWeeklyPlan(plan)
}
```

---

## 3. Data Layer

### 3.1 Remote DTOs

**File:** `src/main/kotlin/data/remote/dto/RecipeDto.kt`
```kotlin
@Serializable
data class RecipeDto(
    val id: Long,
    val title: String,
    val summary: String? = null,
    val image: String? = null,
    val sourceUrl: String? = null,
    val spoonacularScore: Double? = null,
    val usedIngredients: List<IngredientDto> = emptyList(),
    val analyzedInstructions: List<AnalyzedInstructionDto> = emptyList()
)
```

**File:** `src/main/kotlin/data/remote/dto/IngredientDto.kt`
```kotlin
@Serializable
data class IngredientDto(
    val id: Long,
    val name: String,
    val amount: Float? = null,
    val unit: String? = null
)
```

**File:** `src/main/kotlin/data/remote/dto/AnalyzedInstructionDto.kt`
```kotlin
@Serializable
data class AnalyzedInstructionDto(
    val steps: List<StepDto> = emptyList()
)

@Serializable
data class StepDto(
    val number: Int,
    val step: String
)
```

### 3.2 Mappers

**File:** `src/main/kotlin/data/mappers/RecipeMapper.kt`
```kotlin
object RecipeMapper {
    fun dtoToDomain(dto: RecipeDto): Recipe = Recipe(
        id = dto.id,
        title = dto.title,
        summary = dto.summary,
        imageUrl = dto.image,
        sourceUrl = dto.sourceUrl,
        popularityScore = dto.spoonacularScore,
        ingredients = dto.usedIngredients.map { 
            Ingredient(id = it.id, name = it.name, normalized = it.name.normalize()) 
        },
        instructionsSummary = dto.analyzedInstructions.firstOrNull()?.steps?.firstOrNull()?.step,
        createdAt = System.currentTimeMillis()
    )
    
    fun domainToDatabase(recipe: Recipe, expiresAt: Long, isPreloaded: Boolean): RecipeEntity =
        RecipeEntity(
            id = recipe.id,
            title = recipe.title,
            summary = recipe.summary,
            imageUrl = recipe.imageUrl,
            sourceUrl = recipe.sourceUrl,
            popularityScore = recipe.popularityScore,
            lastFetchedAt = System.currentTimeMillis(),
            expiresAt = expiresAt,
            isPreloaded = isPreloaded
        )
}
```

**File:** `src/main/kotlin/data/mappers/InstructionsMapper.kt`
```kotlin
object InstructionsMapper {
    fun dtoToDomain(recipeId: Long, dto: AnalyzedInstructionDto): RecipeInstructions =
        RecipeInstructions(
            recipeId = recipeId,
            steps = dto.steps.map { it.step }
        )
}
```

**File:** `src/main/kotlin/data/core/TextNormalizer.kt`
```kotlin
object TextNormalizer {
    fun normalize(text: String): String =
        text.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n")
            .trim()
}

fun String.normalize(): String = TextNormalizer.normalize(this)
```

### 3.3 SQLDelight Schema & Tables

**File:** `src/main/sqldelight/recipes.sq`

```sql
CREATE TABLE recipes (
  id INTEGER PRIMARY KEY,
  title TEXT NOT NULL,
  summary TEXT,
  image_url TEXT,
  source_url TEXT,
  popularity_score REAL,
  last_fetched_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  is_preloaded INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE ingredients (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  normalized TEXT NOT NULL UNIQUE
);

CREATE TABLE recipe_ingredients (
  recipe_id INTEGER NOT NULL,
  ingredient_id INTEGER NOT NULL,
  is_main INTEGER NOT NULL DEFAULT 0,
  amount REAL,
  unit TEXT,
  PRIMARY KEY (recipe_id, ingredient_id),
  FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
  FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

CREATE TABLE instructions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  recipe_id INTEGER NOT NULL,
  step_number INTEGER NOT NULL,
  text TEXT NOT NULL,
  FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
);

CREATE TABLE weekly_plan (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  week_start_epoch_day INTEGER NOT NULL,
  day_of_week INTEGER NOT NULL,
  meal_slot TEXT NOT NULL,
  recipe_id INTEGER NOT NULL,
  UNIQUE (week_start_epoch_day, day_of_week, meal_slot),
  FOREIGN KEY (recipe_id) REFERENCES recipes(id)
);

CREATE TABLE search_cache (
  query_hash TEXT PRIMARY KEY,
  requested_ingredients TEXT NOT NULL,
  result_recipe_ids TEXT NOT NULL,
  fetched_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);

CREATE INDEX idx_ingredients_normalized ON ingredients(normalized);
CREATE INDEX idx_recipe_ingredients_ingredient_id ON recipe_ingredients(ingredient_id);
CREATE INDEX idx_instructions_recipe_id ON instructions(recipe_id);
CREATE INDEX idx_weekly_plan_week_start ON weekly_plan(week_start_epoch_day);
CREATE INDEX idx_recipes_expires_at ON recipes(expires_at);
CREATE INDEX idx_search_cache_expires_at ON search_cache(expires_at);

-- Named queries for DAOs

getRecipeById:
SELECT * FROM recipes WHERE id = ?;

getRecipesByIngredientNormalized:
SELECT DISTINCT r.* FROM recipes r
INNER JOIN recipe_ingredients ri ON r.id = ri.recipe_id
INNER JOIN ingredients i ON ri.ingredient_id = i.id
WHERE i.normalized = ?
AND r.expires_at > ?
LIMIT ?;

getRecipesByIngredientSet:
SELECT * FROM recipes WHERE id IN (?)
AND expires_at > ?
LIMIT ?;

getRandomRecipes:
SELECT * FROM recipes
WHERE expires_at > ?
ORDER BY RANDOM()
LIMIT ?;

getRecipesByPopularity:
SELECT * FROM recipes
WHERE expires_at > ?
ORDER BY popularity_score DESC
LIMIT ?;

upsertRecipe:
INSERT OR REPLACE INTO recipes (id, title, summary, image_url, source_url, popularity_score, last_fetched_at, expires_at, is_preloaded)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

insertRecipeIngredient:
INSERT INTO recipe_ingredients (recipe_id, ingredient_id, is_main, amount, unit)
VALUES (?, ?, ?, ?, ?);

getOrInsertIngredient:
INSERT OR IGNORE INTO ingredients (name, normalized) VALUES (?, ?);

getIngredientByNormalized:
SELECT * FROM ingredients WHERE normalized = ?;

getInstructionsByRecipeId:
SELECT * FROM instructions WHERE recipe_id = ? ORDER BY step_number;

insertInstruction:
INSERT INTO instructions (recipe_id, step_number, text) VALUES (?, ?, ?);

insertWeeklyPlanEntries:
INSERT INTO weekly_plan (week_start_epoch_day, day_of_week, meal_slot, recipe_id)
VALUES (?, ?, ?, ?);

getWeeklyPlanByWeekStart:
SELECT * FROM weekly_plan WHERE week_start_epoch_day = ?;

deleteWeeklyPlanByWeekStart:
DELETE FROM weekly_plan WHERE week_start_epoch_day = ?;

getSearchCacheByHash:
SELECT * FROM search_cache WHERE query_hash = ?;

insertSearchCache:
INSERT INTO search_cache (query_hash, requested_ingredients, result_recipe_ids, fetched_at, expires_at)
VALUES (?, ?, ?, ?, ?);

getExpiredSearchCaches:
SELECT * FROM search_cache WHERE expires_at < ?;

deleteExpiredSearchCaches:
DELETE FROM search_cache WHERE expires_at < ?;

deleteOrphanedRecipes:
DELETE FROM recipes 
WHERE is_preloaded = 0
AND expires_at < ?
AND id NOT IN (SELECT recipe_id FROM search_cache)
AND id NOT IN (SELECT recipe_id FROM weekly_plan);

getRecipesCount:
SELECT COUNT(*) FROM recipes;

getIngredientsCount:
SELECT COUNT(*) FROM ingredients;
```

### 3.4 Local Data Classes (Entity Mappings)

**File:** `src/main/kotlin/data/local/entity/RecipeEntity.kt`
```kotlin
data class RecipeEntity(
    val id: Long,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val popularityScore: Double?,
    val lastFetchedAt: Long,
    val expiresAt: Long,
    val isPreloaded: Boolean
)
```

**File:** `src/main/kotlin/data/local/entity/IngredientEntity.kt`
```kotlin
data class IngredientEntity(
    val id: Long,
    val name: String,
    val normalized: String
)
```

**File:** `src/main/kotlin/data/local/entity/RecipeWithIngredients.kt`
```kotlin
data class RecipeWithIngredients(
    val recipe: RecipeEntity,
    val ingredients: List<Ingredient>
)
```

### 3.5 Remote API Client

**File:** `src/main/kotlin/data/remote/SpoonacularApi.kt`
```kotlin
class SpoonacularApi(
    private val httpClient: HttpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    },
    private val apiKey: String = EnvConfig.SPOONACULAR_API_KEY
) {
    companion object {
        private const val BASE_URL = "https://api.spoonacular.com"
        private const val FIND_BY_INGREDIENTS_ENDPOINT = "$BASE_URL/recipes/findByIngredients"
        private const val RANDOM_RECIPES_ENDPOINT = "$BASE_URL/recipes/random"
        private const val RECIPE_INFORMATION_ENDPOINT = "$BASE_URL/recipes"
    }
    
    suspend fun findRecipesByIngredients(
        ingredients: List<String>,
        limit: Int = 20
    ): ApiResult<List<RecipeDto>> = withContext(Dispatchers.IO) {
        try {
            val response: List<RecipeDto> = httpClient.get {
                url(FIND_BY_INGREDIENTS_ENDPOINT)
                parameter("ingredients", ingredients.joinToString(","))
                parameter("number", limit)
                parameter("ranking", 2)
                parameter("apiKey", apiKey)
            }
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error("Failed to find recipes by ingredients", e)
        }
    }
    
    suspend fun getRandomRecipes(limit: Int = 10): ApiResult<RandomRecipesDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response: RandomRecipesDto = httpClient.get {
                    url(RANDOM_RECIPES_ENDPOINT)
                    parameter("number", limit)
                    parameter("apiKey", apiKey)
                }
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error("Failed to get random recipes", e)
            }
        }
    
    suspend fun getRecipeInformation(recipeId: Long): ApiResult<RecipeDto> = 
        withContext(Dispatchers.IO) {
            try {
                val response: RecipeDto = httpClient.get {
                    url("$RECIPE_INFORMATION_ENDPOINT/$recipeId")
                    parameter("apiKey", apiKey)
                }
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error("Failed to get recipe information", e)
            }
        }
}

@Serializable
data class RandomRecipesDto(val recipes: List<RecipeDto> = emptyList())
```

### 3.6 Quota Manager

**File:** `src/main/kotlin/data/core/ApiQuotaMonitor.kt`
```kotlin
class ApiQuotaMonitor(
    private val dailyLimit: Int = 50
) {
    private var requestsToday = 0
    private var lastResetDate = getCurrentDate()
    
    @Synchronized
    fun canMakeRequest(count: Int = 1): Boolean {
        resetIfNewDay()
        return (requestsToday + count) <= dailyLimit
    }
    
    @Synchronized
    fun recordRequest(count: Int = 1) {
        resetIfNewDay()
        requestsToday += count
    }
    
    @Synchronized
    fun getStatus(): QuotaStatus {
        resetIfNewDay()
        return QuotaStatus(
            requestsToday = requestsToday,
            remainingAllowance = maxOf(0, dailyLimit - requestsToday),
            quotaResetTime = getNextResetTime(),
            canMakeRequests = canMakeRequest()
        )
    }
    
    private fun resetIfNewDay() {
        val today = getCurrentDate()
        if (today != lastResetDate) {
            requestsToday = 0
            lastResetDate = today
        }
    }
    
    private fun getCurrentDate(): Long = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
    
    private fun getNextResetTime(): Long {
        val now = System.currentTimeMillis()
        val secondsUntilMidnight = (24 * 60 * 60 * 1000 - (now % (24 * 60 * 60 * 1000))) / 1000
        return now + (secondsUntilMidnight * 1000)
    }
}
```

### 3.7 Cache Manager

**File:** `src/main/kotlin/data/core/CacheManager.kt`
```kotlin
class CacheManager {
    companion object {
        const val TTL_MS = 20L * 24 * 60 * 60 * 1000  // 20 days
    }
    
    fun computeQueryHash(ingredients: List<String>): String {
        val normalized = ingredients
            .map { it.normalize() }
            .sorted()
            .joinToString(",")
        return normalized.hashCode().toString()
    }
    
    fun isExpired(expiresAt: Long): Boolean = expiresAt < System.currentTimeMillis()
    
    fun getExpiresAtTime(): Long = System.currentTimeMillis() + TTL_MS
}
```

### 3.8 Background Populator

**File:** `src/main/kotlin/data/background/Populator.kt`
```kotlin
class Populator(
    private val repository: RecipeRepository,
    private val quotaMonitor: ApiQuotaMonitor,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        private val SEED_INGREDIENTS = listOf(
            "chicken", "meat", "tomato", "potato", "rice", "egg"
        )
        private const val PRELOAD_SIZE = 5
        private const val THROTTLE_DELAY_MS = 3600000L  // 1 hour
        private const val MAX_RECIPES_PER_REQUEST = 100
    }
    
    private var isRunning = false
    private var preloadCompleted = false
    
    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            performPreload()
            startBackgroundPopulation()
        }
    }
    
    fun stop() {
        isRunning = false
    }
    
    private suspend fun performPreload() {
        if (preloadCompleted) return
        
        SEED_INGREDIENTS.take(PRELOAD_SIZE).forEach { ingredient ->
            if (!isRunning || !quotaMonitor.canMakeRequest()) return@forEach
            
            val result = repository.getRecipesByIngredientsOnce(
                listOf(ingredient),
                MAX_RECIPES_PER_REQUEST
            )
            
            if (result is ApiResult.Success) {
                quotaMonitor.recordRequest()
            }
            delay(1000)  // 1 second between requests
        }
        preloadCompleted = true
    }
    
    private suspend fun startBackgroundPopulation() {
        while (isRunning) {
            delay(THROTTLE_DELAY_MS)
            if (quotaMonitor.canMakeRequest()) {
                val ingredient = SEED_INGREDIENTS.random()
                repository.getRecipesByIngredientsOnce(
                    listOf(ingredient),
                    MAX_RECIPES_PER_REQUEST
                )
                quotaMonitor.recordRequest()
            }
        }
    }
}
```

### 3.9 Repository Implementation

**File:** `src/main/kotlin/data/repository/RepositoryImpl.kt`
```kotlin
class RepositoryImpl(
    private val database: Queries,  // SQLDelight generated
    private val api: SpoonacularApi,
    private val quotaMonitor: ApiQuotaMonitor,
    private val cacheManager: CacheManager
) : RecipeRepository {
    
    override fun getRecipesByIngredients(
        ingredients: List<String>,
        limit: Int
    ): Flow<ApiResult<List<Recipe>>> = flow {
        val queryHash = cacheManager.computeQueryHash(ingredients)
        
        // Check cache
        val cachedResult = database.getSearchCacheByHash(queryHash).executeAsOneOrNull()
        if (cachedResult != null && !cacheManager.isExpired(cachedResult.expires_at)) {
            val ids = cachedResult.result_recipe_ids.split(",").mapNotNull { it.toLongOrNull() }
            val recipes = ids.mapNotNull { id ->
                database.getRecipeById(id).executeAsOneOrNull()?.toDomain()
            }
            emit(ApiResult.Success(recipes))
            return@flow
        }
        
        // Remote call
        if (quotaMonitor.canMakeRequest()) {
            val apiResult = api.findRecipesByIngredients(ingredients, limit)
            when (apiResult) {
                is ApiResult.Success -> {
                    val recipes = apiResult.data
                    val expiresAt = cacheManager.getExpiresAtTime()
                    
                    recipes.forEach { dto ->
                        val recipeEntity = RecipeMapper.domainToDatabase(
                            RecipeMapper.dtoToDomain(dto),
                            expiresAt,
                            false
                        )
                        database.transaction {
                            database.upsertRecipe(...)
                            dto.usedIngredients.forEach { ingDto ->
                                database.getOrInsertIngredient(ingDto.name, ingDto.name.normalize())
                                val ingEntity = database.getIngredientByNormalized(ingDto.name.normalize())
                                    .executeAsOne()
                                database.insertRecipeIngredient(
                                    dto.id, ingEntity.id.toLong(), 1, ingDto.amount, ingDto.unit
                                )
                            }
                        }
                    }
                    
                    // Update cache
                    val resultIds = recipes.map { it.id }.joinToString(",")
                    database.insertSearchCache(queryHash, ingredients.joinToString(","), resultIds, System.currentTimeMillis(), expiresAt)
                    
                    quotaMonitor.recordRequest()
                    emit(ApiResult.Success(recipes.map { RecipeMapper.dtoToDomain(it) }))
                }
                is ApiResult.Error -> emit(apiResult)
                is ApiResult.QuotaExceeded -> {
                    // Try local data
                    val localRecipes = database.getRecipesByPopularity(System.currentTimeMillis(), limit.toLong())
                        .executeAsList().map { it.toDomain() }
                    if (localRecipes.isNotEmpty()) {
                        emit(ApiResult.StaleData(localRecipes, "Using cached data - quota exceeded"))
                    } else {
                        emit(apiResult)
                    }
                }
                else -> emit(ApiResult.Error("Unknown error"))
            }
        } else {
            emit(ApiResult.QuotaExceeded())
        }
    }
    
    override suspend fun getRecipesByIngredientsOnce(
        ingredients: List<String>,
        limit: Int
    ): ApiResult<List<Recipe>> = getRecipesByIngredients(ingredients, limit).first()
    
    override fun getRandomRecipes(limit: Int): Flow<ApiResult<List<Recipe>>> = flow {
        if (quotaMonitor.canMakeRequest()) {
            val apiResult = api.getRandomRecipes(limit)
            when (apiResult) {
                is ApiResult.Success -> {
                    val recipes = apiResult.data.recipes
                    val expiresAt = cacheManager.getExpiresAtTime()
                    recipes.forEach { dto ->
                        database.upsertRecipe(...)
                    }
                    quotaMonitor.recordRequest()
                    emit(ApiResult.Success(recipes.map { RecipeMapper.dtoToDomain(it) }))
                }
                else -> emit(apiResult)
            }
        } else {
            val localRecipes = database.getRandomRecipes(System.currentTimeMillis(), limit.toLong())
                .executeAsList().map { it.toDomain() }
            if (localRecipes.isNotEmpty()) {
                emit(ApiResult.StaleData(localRecipes, "Using cached random recipes"))
            } else {
                emit(ApiResult.QuotaExceeded())
            }
        }
    }
    
    override suspend fun getRandomRecipesOnce(limit: Int): ApiResult<List<Recipe>> = 
        getRandomRecipes(limit).first()
    
    override fun getRecipeInstructions(recipeId: Long): Flow<ApiResult<RecipeInstructions>> = flow {
        val local = database.getInstructionsByRecipeId(recipeId).executeAsList()
        if (local.isNotEmpty()) {
            emit(ApiResult.Success(RecipeInstructions(recipeId, local.map { it.text })))
        } else if (quotaMonitor.canMakeRequest()) {
            val apiResult = api.getRecipeInformation(recipeId)
            when (apiResult) {
                is ApiResult.Success -> {
                    val instructions = apiResult.data.analyzedInstructions.first()
                    instructions.steps.forEachIndexed { index, step ->
                        database.insertInstruction(recipeId, (index + 1).toLong(), step.step)
                    }
                    quotaMonitor.recordRequest()
                    emit(ApiResult.Success(RecipeInstructions(recipeId, instructions.steps.map { it.step })))
                }
                else -> emit(apiResult)
            }
        } else {
            emit(ApiResult.QuotaExceeded())
        }
    }
    
    override suspend fun saveWeeklyPlan(plan: WeeklyPlan): ApiResult<Unit> = try {
        database.deleteWeeklyPlanByWeekStart(plan.weekStartEpochDay)
        plan.entries.forEach { entry ->
            database.insertWeeklyPlanEntries(
                plan.weekStartEpochDay,
                entry.dayOfWeek.toLong(),
                entry.mealSlot,
                entry.recipeId
            )
        }
        ApiResult.Success(Unit)
    } catch (e: Exception) {
        ApiResult.Error("Failed to save weekly plan", e)
    }
    
    override fun getWeeklyPlan(weekStartEpochDay: Long): Flow<ApiResult<WeeklyPlan?>> = flow {
        try {
            val entries = database.getWeeklyPlanByWeekStart(weekStartEpochDay)
                .executeAsList().map { 
                    WeeklyPlanEntry(
                        dayOfWeek = it.day_of_week.toInt(),
                        mealSlot = it.meal_slot,
                        recipeId = it.recipe_id
                    )
                }
            val plan = if (entries.isNotEmpty()) WeeklyPlan(weekStartEpochDay, entries) else null
            emit(ApiResult.Success(plan))
        } catch (e: Exception) {
            emit(ApiResult.Error("Failed to get weekly plan", e))
        }
    }
    
    override suspend fun clearExpiredCache(): ApiResult<Unit> = try {
        database.deleteExpiredSearchCaches(System.currentTimeMillis())
        database.deleteOrphanedRecipes(System.currentTimeMillis())
        ApiResult.Success(Unit)
    } catch (e: Exception) {
        ApiResult.Error("Failed to clear expired cache", e)
    }
    
    override suspend fun getApiQuotaStatus(): ApiResult<QuotaStatus> = 
        ApiResult.Success(quotaMonitor.getStatus())
    
    override suspend fun manualSync(): ApiResult<SyncResult> = try {
        val recipesCountBefore = database.getRecipesCount().executeAsOne()
        val ingredientsCountBefore = database.getIngredientsCount().executeAsOne()
        
        val populator = Populator(this, quotaMonitor)
        populator.start()
        delay(5000)  // Give populator time to run
        populator.stop()
        
        val recipesCountAfter = database.getRecipesCount().executeAsOne()
        val ingredientsCountAfter = database.getIngredientsCount().executeAsOne()
        
        ApiResult.Success(SyncResult(
            recipesAdded = (recipesCountAfter - recipesCountBefore).toInt(),
            ingredientsAdded = (ingredientsCountAfter - ingredientsCountBefore).toInt(),
            requestsMade = 1
        ))
    } catch (e: Exception) {
        ApiResult.Error("Manual sync failed", e)
    }
}
```

---

## 4. Implementation Checklist

### Phase 1: Foundation & Domain
- [ ] Create `ENV_VARS` at project root and add to `.gitignore`
- [ ] Implement `core/EnvConfig.kt`
- [ ] Create domain models: `Recipe.kt`, `Ingredient.kt`, `RecipeInstructions.kt`, `WeeklyPlanEntry.kt`, `WeeklyPlan.kt`
- [ ] Create `domain/result/ApiResult.kt` sealed class
- [ ] Create `domain/repository/RecipeRepository.kt` interface
- [ ] Create all 5 use case classes in `domain/usecase/*`

### Phase 2: Data Layer - Core Infrastructure
- [ ] Create `data/core/TextNormalizer.kt`
- [ ] Create `data/core/ApiQuotaMonitor.kt`
- [ ] Create `data/core/CacheManager.kt`
- [ ] Create remote DTOs: `RecipeDto.kt`, `IngredientDto.kt`, `AnalyzedInstructionDto.kt`
- [ ] Create mappers: `RecipeMapper.kt`, `InstructionsMapper.kt`
- [ ] Create local entity classes: `RecipeEntity.kt`, `IngredientEntity.kt`, `RecipeWithIngredients.kt`

### Phase 3: Database & Persistence
- [ ] Create SQLDelight schema file: `src/main/sqldelight/recipes.sq`
- [ ] Run SQLDelight code generation to create DAO interfaces
- [ ] Verify generated `Queries` interface matches all named queries

### Phase 4: Remote API & Repository
- [ ] Create `data/remote/SpoonacularApi.kt` with Ktor client
- [ ] Implement `data/repository/RepositoryImpl.kt`
- [ ] Wire API client and database into RepositoryImpl

### Phase 5: Background Population & UI Integration
- [ ] Create `data/background/Populator.kt`
- [ ] Implement preload logic (SEED_INGREDIENTS: chicken, meat, tomato, potato, rice, egg)
- [ ] Implement background population scheduler (throttle, quota protection)

### Phase 6: Testing & Validation
- [ ] Create unit tests for `TextNormalizer.kt`
- [ ] Create unit tests for `CacheManager.kt`
- [ ] Create mapper tests (`RecipeDtoToEntity`, `EntityToDomain`)
- [ ] Create DAO integration tests (use SQLDelight in-memory driver)
- [ ] Create repository integration tests (mock API + real DB)
- [ ] Create use case unit tests (mock repository)
- [ ] Create background populator tests

### Phase 7: Presentation Layer Integration (next phase)
- [ ] Create `SearchViewModel.kt`
- [ ] Create `PlanViewModel.kt`
- [ ] Create Compose UI: `SearchRecipesView.kt`
- [ ] Create Compose UI: `WeeklyPlanView.kt`
- [ ] Add sync control UI component showing quota status
- [ ] Wire ViewModels to use cases

---

## 5. Constants & Configuration

### TTL Configuration
```kotlin
const val CACHE_TTL_MS = 20L * 24 * 60 * 60 * 1000  // 20 days
```

### API Rate Limits
```kotlin
const val DAILY_QUERY_LIMIT = 50
const val PRELOAD_SIZE = 5
const val MAX_RECIPES_PER_REQUEST = 100
const val BACKGROUND_THROTTLE_DELAY_MS = 3600000L  // 1 hour
```

### Seed Ingredients (English)
```kotlin
val SEED_INGREDIENTS = listOf(
    "chicken",
    "meat",
    "tomato",
    "potato",
    "rice",
    "egg"
)
```

---

## 6. UI Components for Quota Management

### Quota Status Display (Compose)
```kotlin
@Composable
fun QuotaStatusIndicator(quotaStatus: QuotaStatus) {
    Column {
        Text("API Requests Today: ${quotaStatus.requestsToday} / 50")
        LinearProgressIndicator(
            progress = (quotaStatus.requestsToday / 50f).coerceIn(0f, 1f)
        )
        if (quotaStatus.requestsToday >= 45) {
            Text("Warning: Approaching daily quota", color = Color.Red)
        }
        if (!quotaStatus.canMakeRequests) {
            Text("Daily quota exhausted. Resets in: ${formatTimeRemaining(quotaStatus.quotaResetTime)}")
        }
    }
}

@Composable
fun ManualSyncButton(onSync: suspend () -> Unit) {
    Button(onClick = {
        coroutineScope.launch {
            val result = onSync()
            // Handle result
        }
    }) {
        Text("Sync Recipes")
    }
}
```

---

## 7. Kotlin Gradle Dependencies (sample)

```kotlin
dependencies {
    // Kotlin & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    
    // Compose
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material:material:1.5.0")
    
    // Ktor Client
    implementation("io.ktor:ktor-client-core:2.3.1")
    implementation("io.ktor:ktor-client-okhttp:2.3.1")
    implementation("io.ktor:ktor-client-serialization:2.3.1")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    
    // SQLDelight
    implementation("com.squareup.sqldelight:runtime:2.0.1")
    
    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
}
```

---

## 8. Next Phase

Once Phases 1–7 are complete:
1. **Presentation Layer:** Implement ViewModels (`SearchViewModel`, `PlanViewModel`) and Compose UI components
2. **Main Application:** Wire DI, start Populator on app launch, display quota status in UI
3. **Testing:** E2E tests with mock API and pre-populated DB
4. **Deployment:** Package as Compose for Desktop executable

---

**Document Version:** 1.0  
**Last Updated:** 2026-06-12  
**Assigned To:** Development Team  
**Status:** Ready for Implementation ✅


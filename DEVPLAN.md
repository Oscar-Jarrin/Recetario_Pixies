# RecetarioPixies â€” Granular Development Plan
**Architecture**: Clean MVVM Â· KMP Â· TDD-First Â· Desktop-Priority  
**API**: Spoonacular (single provider) Â· Offline-First Cache Strategy  
**Date**: 2026-06-17

---

## Design Decisions (locked before implementation)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Navigation | `compose-navigation` library, `NavHost` in `App.kt`, typed routes in `commonMain` | Idiomatic KMP, keeps routing in shared code |
| `IngredientSearchResult` enrichment | JOIN with `RecipeOverviewEntity` by id; default `0` / `""` if recipe was never cached | `/findByIngredients` returns no `readyInMinutes` or `dishType` |
| `RecipeInstructions.instructions` | `Map<String, List<String>>` â€” key = group name (`""` for ungrouped steps) | API returns multiple named groups; UI renders group headers |
| Weekly plans | Unlimited user-named plans; `WeeklyPlanEntity` + `WeeklyPlanSlotEntity` (7 slots per plan) | "planType" â†’ user-defined plan name string |
| Offline ingredient search | `RecipeIngredientEntity` join table populated at cache time; SQL categorisation in DAO | Scalable, testable, avoids in-memory set ops on large caches |
| Ktor engine | `ktor-client-cio` (jvmMain) Â· `ktor-client-okhttp` (androidMain) | Standard KMP Ktor split |
| Quota signal | Custom `HttpSend` interceptor reads `X-API-Quota-Left` header; throws `QuotaExhaustedException` when `0` | Keeps RepositoryImpl decoupled from HTTP mechanics |

---

## API â†’ Domain Field Mappings

### GET /recipes/random â†’ `RecipeOverview`
| API JSON field                | Domain field                                  | Transform |
|-------------------------------|-----------------------------------------------|-----------|
| `id`                          | `id: Int`                                     | direct |
| `title`                       | `title: String`                               | direct |
| `image`                       | `imageUrl: String`                            | rename |
| `readyInMinutes`              | `readyInMinutes: Int`                         | direct |
| `dishTypes[i].name`           | `dishType: List<String>`                      | first element; `""` if array is empty |
| `extendedIngredients[i].name` | stored separately in `RecipeIngredientEntity` | extract all names for offline search |

### GET /recipes/findByIngredients â†’ `IngredientSearchResult`
| API JSON field | Domain field | Transform |
|----------------|--------------|-----------|
| `id` | `id: Int` | direct |
| `title` | `title: String` | direct |
| `image` | `imageUrl: String` | rename |
| *(not in response)* | `readyInMinutes: Int` | JOIN with `RecipeOverviewEntity.readyInMinutes` by id |
| *(not in response)* | `dishType: String` | JOIN with `RecipeOverviewEntity.dishType` by id |
| `missedIngredients[i].name` | `missingIngredients: List<String>` | extract names |
| `usedIngredients[i].name` | `usedIngredients: List<String>` | extract names |
| `unusedIngredients[i].name` | `unusedIngredients: List<String>` | extract names |

### GET /recipes/{id}/analyzedInstructions â†’ `RecipeInstructions`
| API JSON field | Domain field | Transform |
|----------------|--------------|-----------|
| `[i].name` | Map key (`""` when name is blank) | per group |
| `[i].steps[j].step` | Map value list | collect in order; order by `steps[j].number` |

---

## Room Schema

```
RecipeOverviewEntity
  id: Int (PK)
  title: String
  imageUrl: String
  readyInMinutes: Int
  dishType: String

RecipeIngredientEntity
  rowId: Long (PK, autoGenerate)
  recipeId: Int (FK â†’ RecipeOverviewEntity.id)
  ingredientName: String

RecipeInstructionStepEntity
  rowId: Long (PK, autoGenerate)
  recipeId: Int
  groupName: String        â† "" for ungrouped
  stepOrder: Int           â† steps[j].number
  stepText: String

WeeklyPlanEntity
  id: Long (PK, autoGenerate)
  planName: String (UNIQUE)

WeeklyPlanSlotEntity
  id: Long (PK, autoGenerate)
  planId: Long (FK â†’ WeeklyPlanEntity.id)
  dayIndex: Int            â† 0=Monday â€¦ 6=Sunday
  recipeId: Int (FK â†’ RecipeOverviewEntity.id)
```

---

## Navigation Routes

```
sealed class Screen(val route: String) {
    object Home        : Screen("home")
    object Search      : Screen("search")
    object Planner     : Screen("planner")
    data class RecipeDetail(val id: Int)     : Screen("recipe/{id}")
    data class PlannerDetail(val planId: Long): Screen("planner/{planId}")
}
```
`NavHost` defined in `shared/src/commonMain/â€¦/presentation/navigation/AppNavGraph.kt`.

---

## Custom Exceptions (shared/domain layer)

```kotlin
class QuotaExhaustedException : Exception("Spoonacular API quota exhausted")
class NetworkException(cause: Throwable) : Exception(cause)
class RecipeNotFoundException(id: Int) : Exception("Recipe $id not found in cache")
```

---

## Constants (companion objects / top-level const val)

```kotlin
// SpoonacularApiConstants.kt
const val QUOTA_HEADER         = "X-API-Quota-Left"
const val RANDOM_RECIPES_COUNT = 10
const val BASE_URL             = "https://api.spoonacular.com"

// WeeklyPlanConstants.kt
const val DAYS_IN_WEEK = 7
const val MAX_PLAN_WEEKS = 14  // 14 days of meal structures in Room
```

---

## TDD Step Template (applies to every vertical slice)

For each phase the 6 steps below are mandatory and must be executed in order.  
**Tests must be written and compile before any production code for that step exists.**  
Run `./gradlew :shared:commonTest` to verify before checking off any step.

---

---

# PHASE 0 â€” Foundational Architecture Platform

> No tests yet â€” this phase wires the skeleton that all later tests depend on.  
> Every sub-task must be done before Phase 1 begins.

---

### 0.1 Gradle & Dependency Setup âœ…

**Files edited:**
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`

> **Note**: KSP uses version `2.3.9` (new KSP2 unified versioning, decoupled from Kotlin version). `defaultRequest { parameter() }` is unavailable in Ktor 3.x inside that block â€” API key injection moved into the `HttpSend` interceptor instead.

- [x] **Add versions** to `gradle/libs.versions.toml`:
  ```toml
  [versions]
  ktor           = "3.1.3"
  room           = "2.7.1"
  ksp            = "2.4.0-2.0.2"
  sqlite-bundled = "2.5.1"
  mockk          = "1.14.2"
  kotlinx-serialization = "1.8.1"
  kotlinx-coroutines-test = "1.11.0"
  navigation-compose = "2.9.0-beta01"
  ```

- [x] **Add library aliases** to `[libraries]` block in `gradle/libs.versions.toml`:
  ```toml
  # Ktor
  ktor-client-core        = { module = "io.ktor:ktor-client-core",               version.ref = "ktor" }
  ktor-client-cio         = { module = "io.ktor:ktor-client-cio",                version.ref = "ktor" }
  ktor-client-okhttp      = { module = "io.ktor:ktor-client-okhttp",             version.ref = "ktor" }
  ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
  ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
  ktor-client-logging     = { module = "io.ktor:ktor-client-logging",            version.ref = "ktor" }

  # Room KMP
  room-runtime            = { module = "androidx.room:room-runtime",             version.ref = "room" }
  room-compiler           = { module = "androidx.room:room-compiler",            version.ref = "room" }
  sqlite-bundled          = { module = "androidx.sqlite:sqlite-bundled",         version.ref = "sqlite-bundled" }

  # Serialization
  kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

  # Coroutines test
  kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines-test" }

  # MockK
  mockk                   = { module = "io.mockk:mockk",                        version.ref = "mockk" }

  # Navigation
  navigation-compose      = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
  ```

- [x] **Add plugin aliases** to `[plugins]` block:
  ```toml
  ksp                = { id = "com.google.devtools.ksp",              version.ref = "ksp" }
  kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
  room               = { id = "androidx.room",                         version.ref = "room" }
  ```

- [x] **Update `shared/build.gradle.kts`** â€” add plugins, KSP room schema directory, and dependencies:
  ```kotlin
  plugins {
      // existing plugins â€¦
      alias(libs.plugins.ksp)
      alias(libs.plugins.room)
      alias(libs.plugins.kotlinx.serialization)
  }

  room {
      schemaDirectory("$projectDir/schemas")
  }

  // In kotlin { ... } add KSP configurations:
  // ksp(libs.room.compiler)  â† add per target

  // commonMain.dependencies additions:
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.json)
  implementation(libs.ktor.client.logging)
  implementation(libs.room.runtime)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.navigation.compose)

  // androidMain.dependencies:
  implementation(libs.ktor.client.okhttp)

  // jvmMain.dependencies:
  implementation(libs.ktor.client.cio)

  // commonTest.dependencies additions:
  implementation(libs.kotlin.test)
  implementation(libs.mockk)
  implementation(libs.kotlinx.coroutines.test)

  // KSP targets (add inside kotlin { } block):
  // add(kspCommonMainMetadata, libs.room.compiler)
  // add("kspAndroid", libs.room.compiler)
  // add("kspJvm", libs.room.compiler)
  ```

---

### 0.2 Ktor HTTP Client with Quota Header Plugin âœ…

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/SpoonacularApiConstants.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/QuotaExhaustedException.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/NetworkException.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/HttpEngineFactory.kt` (expect)
- `shared/src/androidMain/kotlin/com/pixies/recetario/data/remote/HttpEngineFactory.android.kt` (actual)
- `shared/src/jvmMain/kotlin/com/pixies/recetario/data/remote/HttpEngineFactory.jvm.kt` (actual)
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/SpoonacularHttpClient.kt`

- [ ] Create `SpoonacularApiConstants.kt`:
  ```kotlin
  package com.pixies.recetario.data.remote

  const val BASE_URL             = "https://api.spoonacular.com"
  const val QUOTA_HEADER         = "X-API-Quota-Left"
  const val RANDOM_RECIPES_COUNT = 10
  ```

- [ ] Create `QuotaExhaustedException.kt` and `NetworkException.kt` in
  `shared/src/commonMain/kotlin/com/pixies/recetario/domain/exception/`.

- [ ] Create `expect fun httpEngine()` in `HttpEngineFactory.kt`; implement:
  - `androidMain` actual â†’ `OkHttp.create()`
  - `jvmMain` actual â†’ `CIO.create()`

- [ ] Create `SpoonacularHttpClient.kt` â€” factory function returning an `HttpClient`:
  ```kotlin
  fun buildSpoonacularClient(engine: HttpClientEngine, apiKey: String): HttpClient =
      HttpClient(engine) {
          install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
          install(Logging) { level = LogLevel.HEADERS }
          defaultRequest { url(BASE_URL); parameter("apiKey", apiKey) }
      }.also { client ->
          client.plugin(HttpSend).intercept { request ->
              val call = execute(request)
              val quota = call.response.headers[QUOTA_HEADER]?.toIntOrNull() ?: Int.MAX_VALUE
              if (quota == 0) throw QuotaExhaustedException()
              call
          }
      }
  ```
  Keep the `also` block â‰¤ 10 lines; extract `intercept` body to a named private fun if it grows.

---

### 0.3 Room Database Skeleton âœ… (partial â€” @Database annotation deferred to Phase 1 step 2)

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/AppDatabase.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/DatabaseBuilder.kt` (expect)
- `shared/src/androidMain/kotlin/com/pixies/recetario/data/local/DatabaseBuilder.android.kt` (actual)
- `shared/src/jvmMain/kotlin/com/pixies/recetario/data/local/DatabaseBuilder.jvm.kt` (actual)

- [ ] Declare `@Database` abstract class `AppDatabase` in `commonMain` with an empty entities list (entities added per phase).

- [ ] Create `expect fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<AppDatabase>`:
  - `androidMain` actual: uses `Room.databaseBuilder(context as Context, â€¦)`
  - `jvmMain` actual: uses `Room.databaseBuilder(â€¦).setDriver(BundledSQLiteDriver())`

- [ ] Verify project compiles: `./gradlew :shared:compileKotlinJvm`

---

### 0.4 Test Coroutine Helpers âœ…

**File created:**
- `shared/src/commonTest/kotlin/com/pixies/recetario/TestDispatchers.kt`

- [ ] Create `TestDispatcherRule` helper:
  ```kotlin
  class TestDispatcherRule(
      val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
  ) {
      fun before() = Dispatchers.setMain(dispatcher)
      fun after() = Dispatchers.resetMain()
  }
  ```
  Used as a setup/teardown pair inside every ViewModel test class.

---

### 0.5 Navigation Graph Skeleton âœ…

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/navigation/Screen.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/navigation/AppNavGraph.kt`

- [ ] Define `sealed class Screen` with routes: `Home`, `Search`, `RecipeDetail(id)`, `Planner`, `PlannerDetail(planId)`.
- [ ] Create `AppNavGraph` composable with `NavHost`; all screen destinations are `composable { TODO() }` stubs until each phase implements them.
- [ ] Wire `AppNavGraph` into `App.kt` replacing the current placeholder content.

---

### 0.6 Pure Kotlin DI Factory Skeleton âœ… (database property deferred to Phase 1)

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/di/AppModule.kt`
- `desktopApp/src/jvmMain/kotlin/com/pixies/recetario/main.kt` (edit â€” wire `AppModule`)
- `androidApp/src/androidMain/kotlin/com/pixies/recetario/MainActivity.kt` (edit â€” wire `AppModule`)

- [ ] Create `AppModule` as a plain Kotlin class (not object) that takes `apiKey: String` and platform context:
  ```kotlin
  class AppModule(apiKey: String, context: Any?) {
      private val httpClient    = buildSpoonacularClient(httpEngine(), apiKey)
      private val database      = getDatabaseBuilder(context).build()
      // DAOs and repositories added per phase
  }
  ```
- [ ] Desktop `main.kt` instantiates `AppModule` and passes it to `App(module)`.
- [ ] Android `MainActivity` instantiates `AppModule` and passes it to `App(module)`.

---

---

# PHASE 1 â€” Vertical Slice: Random Home Discovery

**Feature**: A lazy grid of 10 random recipes showing title, image, ready time, and dish type.  
Clicking a recipe navigates to the detail screen. Includes Loading skeleton + Error state with retry.  
**Endpoint**: `GET /recipes/random?number=10`  
**Mock file**: `spoonacular_responses/GetRandomRecipes response`

---

### Step 1 â€” Write Domain UseCase Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/domain/GetRandomRecipesUseCaseTest.kt`

- [x] Mock `RecipeRepository` with MockK.
- [x] **Happy path**: `coEvery { repo.getRandomRecipes(RANDOM_RECIPES_COUNT) } returns listOf(fakeOverview)` â†’ use case returns same list.
- [ ] **Sad path â€” network failure**: `coEvery { repo.getRandomRecipes(â€¦) } throws NetworkException(â€¦)` â†’ use case propagates exception.
- [ ] **Sad path â€” quota exhausted**: `coEvery { repo.getRandomRecipes(â€¦) } throws QuotaExhaustedException()` â†’ use case propagates exception.
- [ ] **Edge case â€” empty list**: repository returns `emptyList()` â†’ use case returns `emptyList()` (not an error).
- [ ] Run: `./gradlew :shared:commonTest --tests "*GetRandomRecipesUseCaseTest"` â€” expect compilation errors (production code not yet written). Record failures; do **not** fix by writing prod code yet.

---

### Step 2 â€” Implement Domain Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/model/RecipeOverview.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/exception/AppExceptions.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/repository/RecipeRepository.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/usecase/GetRandomRecipesUseCase.kt`

- [x] Create immutable domain model:
  ```kotlin
  data class RecipeOverview(
      val id: Int,
      val title: String,
      val imageUrl: String,
      val readyInMinutes: Int,
      val dishType: String
  )
  ```

- [x] Create exceptions in `AppExceptions.kt`:
  ```kotlin
  class QuotaExhaustedException : Exception("API quota exhausted")
  class NetworkException(cause: Throwable) : Exception(cause)
  class RecipeNotFoundException(id: Int) : Exception("Recipe $id not in cache")
  ```

- [x] Create `RecipeRepository` interface (suspend functions, `commonMain`):
  ```kotlin
  interface RecipeRepository {
      suspend fun getRandomRecipes(count: Int): List<RecipeOverview>
  }
  ```

- [x] Create `GetRandomRecipesUseCase`:
  ```kotlin
  class GetRandomRecipesUseCase(private val repository: RecipeRepository) {
      suspend operator fun invoke(): List<RecipeOverview> =
          repository.getRandomRecipes(RANDOM_RECIPES_COUNT)
  }
  ```

- [ ] Run: `./gradlew :shared:commonTest --tests "*GetRandomRecipesUseCaseTest"` â€” all tests must pass. âœ“

---

### Step 3 â€” Write Data Layer Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/data/RecipeRepositoryImplTest.kt`

- [x] Mock `SpoonacularApiService` (Ktor wrapper interface) and `RecipeOverviewDao` with MockK.

- [x] **Happy path**: `coEvery { api.getRandomRecipes(10) } returns listOf(fakeDto)` â†’
  - `dao.insertAll(â€¦)` is called exactly once (verify with `coVerify`)
  - Return value equals mapped `RecipeOverview` list.

- [ ] **Network failure path**: `coEvery { api.getRandomRecipes(10) } throws IOException(â€¦)` â†’
  - `dao.insertAll` is **never** called
  - `dao.getAllRecipes()` is called once as fallback
  - Return value equals what DAO returns.

- [x] **Quota exhausted path**: `coEvery { api.getRandomRecipes(10) } throws QuotaExhaustedException()` â†’
  - Same fallback behaviour as network failure path.

- [x] **Cold cache (empty Room)**: network fails + `dao.getAllRecipes()` returns `emptyList()` â†’
  - Use case returns `emptyList()` (not an exception).

- [ ] Run tests â€” expect failures. Do not fix yet.

---

### Step 4 â€” Implement Data Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/dto/RandomRecipesResponseDto.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/dto/RecipeOverviewDto.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/dto/ExtendedIngredientDto.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/SpoonacularApiService.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/entity/RecipeOverviewEntity.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/entity/RecipeIngredientEntity.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/dao/RecipeOverviewDao.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/dao/RecipeIngredientDao.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/mapper/RecipeMapper.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/RecipeRepositoryImpl.kt`

**Files edited:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/AppDatabase.kt` (add entities + DAOs)

- [x] Create DTOs with `@Serializable`:
  ```kotlin
  @Serializable data class RandomRecipesResponseDto(val recipes: List<RecipeOverviewDto>)
  @Serializable data class RecipeOverviewDto(
      val id: Int, val title: String, val image: String,
      val readyInMinutes: Int, val dishTypes: List<String>,
      val extendedIngredients: List<ExtendedIngredientDto> = emptyList()
  )
  @Serializable data class ExtendedIngredientDto(val name: String)
  ```

- [x] Create `SpoonacularApiService` interface with Ktor implementation:
  ```kotlin
  interface SpoonacularApiService {
      suspend fun getRandomRecipes(count: Int): List<RecipeOverviewDto>
  }
  ```
  Implementation uses `HttpClient.get("/recipes/random") { parameter("number", count) }`.

- [x] Create Room entities and DAOs:
  - `RecipeOverviewEntity` â€” fields match schema table above; `@PrimaryKey val id: Int`
  - `RecipeIngredientEntity` â€” `rowId: Long, recipeId: Int, ingredientName: String`
  - `RecipeOverviewDao`: `insertAll`, `getAllRecipes`, `getRecipeById`
  - `RecipeIngredientDao`: `insertAll(ingredients: List<RecipeIngredientEntity>)`, `getIngredientsForRecipe(recipeId: Int)`

- [x] Add both entities to `@Database` annotation in `AppDatabase.kt`.

- [ ] Create `RecipeMapper.kt` â€” pure mapping functions (max 15 lines each):
  ```kotlin
  fun RecipeOverviewDto.toDomain(): RecipeOverview = RecipeOverview(
      id = id, title = title, imageUrl = image,
      readyInMinutes = readyInMinutes, dishType = dishTypes.firstOrNull() ?: ""
  )
  fun RecipeOverviewDto.toEntity(): RecipeOverviewEntity = â€¦
  fun RecipeOverviewEntity.toDomain(): RecipeOverview = â€¦
  fun RecipeOverviewDto.toIngredientEntities(): List<RecipeIngredientEntity> =
      extendedIngredients.map { RecipeIngredientEntity(recipeId = id, ingredientName = it.name) }
  ```

- [ ] Create `RecipeRepositoryImpl` â€” implement `RecipeRepository` interface:
  ```kotlin
  class RecipeRepositoryImpl(
      private val api: SpoonacularApiService,
      private val overviewDao: RecipeOverviewDao,
      private val ingredientDao: RecipeIngredientDao
  ) : RecipeRepository {
      override suspend fun getRandomRecipes(count: Int): List<RecipeOverview> =
          runCatching { fetchAndCache(count) }
              .getOrElse { fallbackToCache() }

      private suspend fun fetchAndCache(count: Int): List<RecipeOverview> {
          val dtos = api.getRandomRecipes(count)
          overviewDao.insertAll(dtos.map { it.toEntity() })
          ingredientDao.insertAll(dtos.flatMap { it.toIngredientEntities() })
          return dtos.map { it.toDomain() }
      }

      private suspend fun fallbackToCache(): List<RecipeOverview> =
          overviewDao.getAllRecipes().map { it.toDomain() }
  }
  ```

- [x] Wire `RecipeRepositoryImpl` into `AppModule`.
- [ ] Run: `./gradlew :shared:commonTest --tests "*RecipeRepositoryImplTest"` â€” all tests must pass. âœ“

---

### Step 5 â€” Write Presentation Layer Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/presentation/HomeViewModelTest.kt`

- [x] Mock `GetRandomRecipesUseCase` with MockK.

- [ ] **Loading â†’ Success**: Initial state is `HomeState.Loading`. After `viewModel.load()` resolves with data â†’ state transitions to `HomeState.Success(recipes)`. Assert list content.

- [ ] **Loading â†’ Error**: `coEvery { useCase() } throws NetworkException(â€¦)` â†’ state transitions to `HomeState.Error(exception)`.

- [x] **Retry on error**: After `Error` state, call `viewModel.retry()` â†’ state goes back to `Loading` then to `Success` or `Error`.

- [ ] **Empty result**: use case returns `emptyList()` â†’ `HomeState.Success(emptyList())` (not Error).

- [ ] Use `TestDispatcherRule` for coroutine cleanup. Run tests â€” expect failures.

---

### Step 6 â€” Implement Presentation Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/home/HomeState.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/home/HomeViewModel.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/home/HomeView.kt`

**File edited:** `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/navigation/AppNavGraph.kt`

- [x] Create sealed interface:
  ```kotlin
  sealed interface HomeState {
      object Loading : HomeState
      data class Success(val recipes: List<RecipeOverview>) : HomeState
      data class Error(val exception: Exception) : HomeState
  }
  ```

- [x] Create `HomeViewModel`:
  ```kotlin
  class HomeViewModel(private val getRandomRecipes: GetRandomRecipesUseCase) : ViewModel() {
      private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
      val state: StateFlow<HomeState> = _state.asStateFlow()

      init { load() }

      fun retry() { load() }

      private fun load() {
          viewModelScope.launch {
              _state.value = HomeState.Loading
              _state.value = runCatching { HomeState.Success(getRandomRecipes()) }
                  .getOrElse { HomeState.Error(it as Exception) }
          }
      }
  }
  ```

- [x] Create `HomeView.kt` Composable:
  - Receives `HomeViewModel` and `onRecipeClick: (Int) -> Unit` lambda.
  - `when (state)` exhaustive: `Loading` â†’ `CircularProgressIndicator`; `Success` â†’ `LazyVerticalGrid` of recipe cards; `Error` â†’ error message + "Retry" `Button` that calls `viewModel.retry()`.
  - Recipe card shows: image (AsyncImage or coil), title, `readyInMinutes` min badge, `dishType` chip. Max 30 lines; extract `RecipeCard` composable if needed.

- [x] Wire `HomeView` into `AppNavGraph` for `Screen.Home` destination.
- [ ] Run: `./gradlew :shared:commonTest --tests "*HomeViewModelTest"` â€” all must pass. âœ“
- [ ] Run desktop: `./gradlew :desktopApp:run` â€” verify Home grid loads and shows recipes.

---

---

# PHASE 2 â€” Vertical Slice: Ingredient Filter Search

**Feature**: Search bar on Home opens an ingredient input view. Returns recipes categorised into used / unused / missing ingredient groups. Offline: filters Room cache with same categorisation.  
**Endpoint**: `GET /recipes/findByIngredients?ingredients=â€¦&number=10&ranking=1`  
**Mock file**: `spoonacular_responses/GerRecipesByIngredients response`

---

### Step 1 â€” Write Domain UseCase Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/domain/SearchRecipesByIngredientsUseCaseTest.kt`

- [ ] Mock `RecipeRepository`.

- [ ] **Happy path**: `coEvery { repo.searchByIngredients(listOf("apple","flour")) } returns listOf(fakeResult)` â†’ use case returns same list.

- [ ] **Empty ingredients list**: `invoke(emptyList())` â†’ returns `emptyList()` immediately without calling repository.

- [ ] **Network failure**: `throws NetworkException` â†’ propagated.

- [ ] **Quota exhausted**: `throws QuotaExhaustedException` â†’ propagated.

- [ ] Run â€” expect failures. Do not fix yet.

---

### Step 2 â€” Implement Domain Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/model/IngredientSearchResult.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/usecase/SearchRecipesByIngredientsUseCase.kt`

**File edited:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/repository/RecipeRepository.kt`

- [x] Create immutable domain model:
  ```kotlin
  data class IngredientSearchResult(
      val id: Int,
      val title: String,
      val imageUrl: String,
      val readyInMinutes: Int,
      val dishType: String,
      val missingIngredients: List<String>,
      val usedIngredients: List<String>,
      val unusedIngredients: List<String>
  )
  ```

- [ ] Add to `RecipeRepository` interface:
  ```kotlin
  suspend fun searchByIngredients(ingredients: List<String>): List<IngredientSearchResult>
  ```

- [ ] Create `SearchRecipesByIngredientsUseCase`:
  ```kotlin
  class SearchRecipesByIngredientsUseCase(private val repository: RecipeRepository) {
      suspend operator fun invoke(ingredients: List<String>): List<IngredientSearchResult> {
          if (ingredients.isEmpty()) return emptyList()
          return repository.searchByIngredients(ingredients)
      }
  }
  ```

- [ ] Run: `./gradlew :shared:commonTest --tests "*SearchRecipesByIngredientsUseCaseTest"` â€” all pass. âœ“

---

### Step 3 â€” Write Data Layer Test

**File edited:**
`shared/src/commonTest/kotlin/com/pixies/recetario/data/RecipeRepositoryImplTest.kt` (add new test class or nested class)

- [ ] **Happy path**: API returns `IngredientSearchResponseDto` list â†’ for each result, enrich from `overviewDao.getRecipeById(id)` â†’ returns assembled `IngredientSearchResult` list.

- [ ] **Enrichment fallback**: `overviewDao.getRecipeById(id)` returns null (recipe not cached) â†’ `readyInMinutes = 0`, `dishType = ""`.

- [ ] **Network failure path**: `api.findByIngredients(â€¦)` throws â†’ `ingredientDao.getOfflineSearchResults(inputs)` called â†’ returns locally assembled list.

- [ ] **Quota exhausted path**: same fallback behaviour as network failure.

- [ ] **Offline categorisation logic** (test DAO directly with fake data):
  - Given `RecipeIngredientEntity` rows for recipe 42: `["butter", "egg", "flour"]`
  - User inputs: `["flour", "sugar"]`
  - Expected: `usedIngredients = ["flour"]`, `missingIngredients = ["sugar"]`, `unusedIngredients = ["butter", "egg"]`

- [ ] Run â€” expect failures. Do not fix yet.

---

### Step 4 â€” Implement Data Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/dto/IngredientSearchResponseDto.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/mapper/IngredientSearchMapper.kt`

**Files edited:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/SpoonacularApiService.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/dao/RecipeIngredientDao.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/RecipeRepositoryImpl.kt`

- [ ] Create DTOs:
  ```kotlin
  @Serializable data class IngredientSearchResponseDto(
      val id: Int, val title: String, val image: String,
      val missedIngredients: List<IngredientItemDto>,
      val usedIngredients:   List<IngredientItemDto>,
      val unusedIngredients: List<IngredientItemDto>
  )
  @Serializable data class IngredientItemDto(val name: String)
  ```

- [ ] Add `findByIngredients(ingredients: String, count: Int): List<IngredientSearchResponseDto>` to `SpoonacularApiService`.
  Implementation: `GET /recipes/findByIngredients` with `ingredients`, `number = count`, `ranking = 1`.

- [ ] Add to `RecipeIngredientDao`:
  ```kotlin
  // Returns all (recipeId, ingredientName) pairs for offline categorisation
  @Query("SELECT * FROM recipe_ingredients WHERE recipeId IN (:ids)")
  suspend fun getIngredientsForRecipes(ids: List<Int>): List<RecipeIngredientEntity>

  @Query("SELECT DISTINCT recipeId FROM recipe_ingredients WHERE ingredientName IN (:names)")
  suspend fun getRecipeIdsMatchingIngredients(names: List<String>): List<Int>
  ```

- [ ] Create `IngredientSearchMapper.kt` â€” pure functions:
  - `IngredientSearchResponseDto.toDomain(overview: RecipeOverviewEntity?): IngredientSearchResult`
  - `categoriseOffline(recipeId: Int, cachedIngredients: List<String>, userIngredients: List<String>): Triple<List<String>, List<String>, List<String>>`
    (returns `used, missing, unused`)

- [ ] Extend `RecipeRepositoryImpl.searchByIngredients`:
  ```kotlin
  override suspend fun searchByIngredients(ingredients: List<String>): List<IngredientSearchResult> =
      runCatching { fetchIngredientResults(ingredients) }
          .getOrElse { offlineIngredientSearch(ingredients) }

  private suspend fun fetchIngredientResults(ingredients: List<String>): List<IngredientSearchResult> {
      val dtos = api.findByIngredients(ingredients.joinToString(","), RANDOM_RECIPES_COUNT)
      return dtos.map { dto ->
          dto.toDomain(overviewDao.getRecipeById(dto.id))
      }
  }

  private suspend fun offlineIngredientSearch(ingredients: List<String>): List<IngredientSearchResult> {
      val recipeIds   = ingredientDao.getRecipeIdsMatchingIngredients(ingredients)
      val overviews   = recipeIds.mapNotNull { overviewDao.getRecipeById(it) }
      val allCached   = ingredientDao.getIngredientsForRecipes(recipeIds)
      return overviews.map { entity -> buildOfflineResult(entity, allCached, ingredients) }
  }
  ```
  Each helper stays â‰¤ 15 lines.

- [ ] Run: `./gradlew :shared:commonTest --tests "*RecipeRepositoryImplTest"` â€” all pass. âœ“

---

### Step 5 â€” Write Presentation Layer Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/presentation/SearchViewModelTest.kt`

- [ ] **Loading â†’ Success**: `coEvery { useCase(listOf("apple")) } returns listOf(fakeResult)` â†’ state = `SearchState.Success(results)`.

- [ ] **Loading â†’ Error**: use case throws â†’ `SearchState.Error(exception)`.

- [ ] **Empty query**: `viewModel.search("")` â†’ state stays `SearchState.Idle` (no network call).

- [ ] **State reset**: After a search result, `viewModel.clearSearch()` â†’ state = `SearchState.Idle`.

- [ ] Run â€” expect failures. Do not fix yet.

---

### Step 6 â€” Implement Presentation Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/search/SearchState.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/search/SearchViewModel.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/search/SearchView.kt`

**File edited:** `AppNavGraph.kt`

- [x] Create sealed interface:
  ```kotlin
  sealed interface SearchState {
      object Idle    : SearchState
      object Loading : SearchState
      data class Success(val results: List<IngredientSearchResult>) : SearchState
      data class Error(val exception: Exception) : SearchState
  }
  ```

- [ ] Create `SearchViewModel` with `search(query: String)` and `clearSearch()` methods; state as `StateFlow<SearchState>`.

- [ ] Create `SearchView.kt`:
  - `OutlinedTextField` for ingredient CSV input.
  - "Search" button triggers `viewModel.search(query)`.
  - `when (state)`: `Idle` â†’ empty placeholder; `Loading` â†’ spinner; `Success` â†’ `LazyColumn` of `IngredientResultCard`; `Error` â†’ error + retry.
  - `IngredientResultCard` shows three chip-groups: Used (green), Unused (grey), Missing (red).

- [ ] Wire into `AppNavGraph` for `Screen.Search`. Add FAB or search icon on `HomeView` that navigates to `Screen.Search`.

- [ ] Run: `./gradlew :shared:commonTest --tests "*SearchViewModelTest"` â€” all pass. âœ“
- [ ] Run desktop: `./gradlew :desktopApp:run` â€” verify search flow end-to-end.

---

---

# PHASE 3 â€” Vertical Slice: Detailed Instructions View

**Feature**: Opened via recipe card click. Displays ordered steps grouped by section name. Loading spinner during fetch; error state with Retry button.  
**Endpoint**: `GET /recipes/{id}/analyzedInstructions`  
**Mock file**: `spoonacular_responses/GetAnalyzedRecipeInstructioins response`  
**Domain change**: `RecipeInstructions.instructions: Map<String, List<String>>`

---

### Step 1 â€” Write Domain UseCase Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/domain/GetRecipeInstructionsUseCaseTest.kt`

- [ ] **Happy path**: `coEvery { repo.getRecipeInstructions(42) } returns fakeInstructions` â†’ use case returns same.

- [ ] **Sad path â€” network failure**: `throws NetworkException` â†’ propagated.

- [ ] **Sad path â€” quota exhausted**: `throws QuotaExhaustedException` â†’ propagated.

- [ ] **Recipe not cached offline**: `throws RecipeNotFoundException(42)` â†’ propagated.

- [ ] Run â€” expect failures. Do not fix yet.

---

### Step 2 â€” Implement Domain Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/model/RecipeInstructions.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/usecase/GetRecipeInstructionsUseCase.kt`

**File edited:** `RecipeRepository.kt`

- [ ] Create domain model:
  ```kotlin
  data class RecipeInstructions(
      val id: Int,
      val instructions: Map<String, List<String>>  // key = group name; "" = ungrouped
  )
  ```

- [ ] Add to `RecipeRepository`:
  ```kotlin
  suspend fun getRecipeInstructions(id: Int): RecipeInstructions
  ```

- [ ] Create `GetRecipeInstructionsUseCase`:
  ```kotlin
  class GetRecipeInstructionsUseCase(private val repository: RecipeRepository) {
      suspend operator fun invoke(id: Int): RecipeInstructions =
          repository.getRecipeInstructions(id)
  }
  ```

- [ ] Run useCase tests â€” all pass. âœ“

---

### Step 3 â€” Write Data Layer Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/data/RecipeInstructionsRepositoryImplTest.kt`

- [ ] **Happy path**: API returns two groups (`""` + `"Bourbon Molasses Butter"`) â†’
  - `stepDao.deleteStepsForRecipe(id)` called once.
  - `stepDao.insertAll(â€¦)` called with correct `groupName` and `stepOrder` values.
  - Return value: `Map<String, List<String>>` with both groups.

- [ ] **Network failure path**: API throws â†’ `stepDao.getStepsForRecipe(id)` called â†’ reconstructs map from rows â†’ returned.

- [ ] **Quota exhausted path**: same as network failure.

- [ ] **Empty cache on failure**: step DAO returns empty list â†’ `RecipeNotFoundException(id)` thrown.

- [ ] **Map reconstruction test**: given a list of `RecipeInstructionStepEntity` rows with mixed `groupName` and `stepOrder` values â†’ `buildInstructionsMap(rows)` returns correctly ordered map.

- [ ] Run â€” expect failures.

---

### Step 4 â€” Implement Data Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/remote/dto/AnalyzedInstructionsResponseDto.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/entity/RecipeInstructionStepEntity.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/dao/RecipeInstructionStepDao.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/mapper/InstructionsMapper.kt`

**Files edited:** `AppDatabase.kt`, `SpoonacularApiService.kt`, `RecipeRepositoryImpl.kt`

- [ ] Create DTOs:
  ```kotlin
  @Serializable data class InstructionGroupDto(val name: String, val steps: List<StepDto>)
  @Serializable data class StepDto(val number: Int, val step: String)
  ```

- [ ] Create Room entity:
  ```kotlin
  @Entity(tableName = "recipe_instruction_steps")
  data class RecipeInstructionStepEntity(
      @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
      val recipeId: Int,
      val groupName: String,
      val stepOrder: Int,
      val stepText: String
  )
  ```

- [ ] Create `RecipeInstructionStepDao`:
  ```kotlin
  @Dao interface RecipeInstructionStepDao {
      @Insert(onConflict = REPLACE) suspend fun insertAll(steps: List<RecipeInstructionStepEntity>)
      @Query("SELECT * FROM recipe_instruction_steps WHERE recipeId = :id ORDER BY rowId ASC")
      suspend fun getStepsForRecipe(id: Int): List<RecipeInstructionStepEntity>
      @Query("DELETE FROM recipe_instruction_steps WHERE recipeId = :id")
      suspend fun deleteStepsForRecipe(id: Int)
  }
  ```

- [ ] Add entity to `AppDatabase`; expose DAO.

- [ ] Create `InstructionsMapper.kt`:
  ```kotlin
  fun List<InstructionGroupDto>.toEntities(recipeId: Int): List<RecipeInstructionStepEntity> =
      flatMap { group ->
          group.steps.map { step ->
              RecipeInstructionStepEntity(
                  recipeId = recipeId,
                  groupName = group.name,
                  stepOrder = step.number,
                  stepText = step.step
              )
          }
      }

  fun List<RecipeInstructionStepEntity>.toInstructionsMap(): Map<String, List<String>> =
      groupBy { it.groupName }
          .mapValues { (_, steps) -> steps.sortedBy { it.stepOrder }.map { it.stepText } }
  ```

- [ ] Extend `RecipeRepositoryImpl.getRecipeInstructions`:
  ```kotlin
  override suspend fun getRecipeInstructions(id: Int): RecipeInstructions =
      runCatching { fetchAndCacheInstructions(id) }
          .getOrElse { fallbackInstructions(id) }

  private suspend fun fetchAndCacheInstructions(id: Int): RecipeInstructions {
      val groups = api.getAnalyzedInstructions(id)
      stepDao.deleteStepsForRecipe(id)
      stepDao.insertAll(groups.toEntities(id))
      return RecipeInstructions(id, groups.toInstructionsMap())
  }

  private suspend fun fallbackInstructions(id: Int): RecipeInstructions {
      val steps = stepDao.getStepsForRecipe(id)
      if (steps.isEmpty()) throw RecipeNotFoundException(id)
      return RecipeInstructions(id, steps.toInstructionsMap())
  }
  ```

- [ ] Run: `./gradlew :shared:commonTest --tests "*RecipeInstructionsRepositoryImplTest"` â€” all pass. âœ“

---

### Step 5 â€” Write Presentation Layer Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/presentation/RecipeDetailViewModelTest.kt`

- [ ] **Loading â†’ Success**: use case returns `RecipeInstructions` with 2 groups â†’ `DetailState.Success(instructions)`.

- [ ] **Loading â†’ Error**: use case throws â†’ `DetailState.Error(exception)`.

- [ ] **Retry**: `viewModel.retry(id)` transitions `Error â†’ Loading â†’ Success`.

- [ ] **Not cached**: `RecipeNotFoundException` â†’ `DetailState.Error` with specific message.

- [ ] Run â€” expect failures.

---

### Step 6 â€” Implement Presentation Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/detail/DetailState.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/detail/RecipeDetailViewModel.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/detail/RecipeDetailView.kt`

**File edited:** `AppNavGraph.kt`

- [x] Create sealed interface:
  ```kotlin
  sealed interface DetailState {
      object Loading : DetailState
      data class Success(val instructions: RecipeInstructions) : DetailState
      data class Error(val exception: Exception) : DetailState
  }
  ```

- [ ] Create `RecipeDetailViewModel(useCase: GetRecipeInstructionsUseCase)` with `load(id: Int)` and `retry(id: Int)`.

- [ ] Create `RecipeDetailView.kt`:
  - Receives `viewModel` and `recipeId: Int`.
  - `LaunchedEffect(recipeId) { viewModel.load(recipeId) }`.
  - `when (state)`: `Loading` â†’ `CircularProgressIndicator`; `Error` â†’ error message + "Retry" button; `Success` â†’ `LazyColumn`.
  - For `Success`: iterate `instructions.instructions.entries`; for each group: if key is non-blank, render a section `Text(key)` header; then an indexed `Text` for each step. Extract `InstructionGroupSection` composable.

- [ ] Wire into `AppNavGraph` for `Screen.RecipeDetail(id)`. Pass `id` from `HomeView`'s `onRecipeClick`.

- [ ] Run: `./gradlew :shared:commonTest --tests "*RecipeDetailViewModelTest"` â€” all pass. âœ“
- [ ] Run desktop: `./gradlew :desktopApp:run` â€” click a recipe card â†’ detail screen shows grouped steps.

---

---

# PHASE 4 â€” Vertical Slice: Local Weekly Meal Planner

**Feature**: 7-day grid. Users create named plans, assign cached recipes to day slots. Multiple plans stored. Fully offline.  
**No network call** â€” planner reads only from Room.

---

### Step 1 â€” Write Domain UseCase Tests

**Files created:**
- `shared/src/commonTest/kotlin/com/pixies/recetario/domain/GetAllWeeklyPlansUseCaseTest.kt`
- `shared/src/commonTest/kotlin/com/pixies/recetario/domain/GetWeeklyPlanUseCaseTest.kt`
- `shared/src/commonTest/kotlin/com/pixies/recetario/domain/SaveWeeklyPlanUseCaseTest.kt`
- `shared/src/commonTest/kotlin/com/pixies/recetario/domain/DeleteWeeklyPlanUseCaseTest.kt`

- [ ] **GetAllWeeklyPlansUseCase** â€” happy path: returns list of all plans; empty list returned as `emptyList()` not exception.

- [ ] **GetWeeklyPlanUseCase** â€” happy path: returns plan by name; plan not found â†’ `RecipeNotFoundException`-style exception.

- [ ] **SaveWeeklyPlanUseCase** â€” happy path: repository `savePlan` called with correct model; verify no exception on upsert.

- [ ] **DeleteWeeklyPlanUseCase** â€” happy path: repository `deletePlan(id)` called; verify.

- [ ] Run â€” expect failures.

---

### Step 2 â€” Implement Domain Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/model/WeeklyPlan.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/repository/WeeklyPlanRepository.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/usecase/GetAllWeeklyPlansUseCase.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/usecase/GetWeeklyPlanUseCase.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/usecase/SaveWeeklyPlanUseCase.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/domain/usecase/DeleteWeeklyPlanUseCase.kt`

- [ ] Create domain model:
  ```kotlin
  data class WeeklyPlan(
      val id: Long = 0,
      val planName: String,
      val daySlots: Map<Int, RecipeOverview>   // key: 0=Mon â€¦ 6=Sun
  )
  ```

- [ ] Create `WeeklyPlanRepository` interface:
  ```kotlin
  interface WeeklyPlanRepository {
      suspend fun getAllPlans(): List<WeeklyPlan>
      suspend fun getPlanByName(name: String): WeeklyPlan
      suspend fun savePlan(plan: WeeklyPlan)
      suspend fun deletePlan(id: Long)
  }
  ```

- [ ] Create each use case as a single-method class delegating to repository. No business logic beyond delegation.

- [ ] Run: `./gradlew :shared:commonTest --tests "*WeeklyPlanUseCase*"` â€” all pass. âœ“

---

### Step 3 â€” Write Data Layer Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/data/WeeklyPlanRepositoryImplTest.kt`

- [ ] Mock `WeeklyPlanDao`, `WeeklyPlanSlotDao`, `RecipeOverviewDao`.

- [ ] **savePlan**: correct `WeeklyPlanEntity` and `WeeklyPlanSlotEntity` rows inserted; old slots for plan deleted before insert.

- [ ] **getPlanByName**: DAO returns entity + slots â†’ joined with `RecipeOverviewDao.getRecipeById` for each slot â†’ correct `WeeklyPlan` assembled.

- [ ] **getPlanByName â€” unknown name**: DAO returns null â†’ throws named exception.

- [ ] **getAllPlans**: returns list of assembled `WeeklyPlan`; empty list returns `emptyList()`.

- [ ] **deletePlan**: `planDao.deleteById(id)` and `slotDao.deleteSlotsForPlan(id)` both called.

- [ ] Run â€” expect failures.

---

### Step 4 â€” Implement Data Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/entity/WeeklyPlanEntity.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/entity/WeeklyPlanSlotEntity.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/dao/WeeklyPlanDao.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/local/dao/WeeklyPlanSlotDao.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/mapper/WeeklyPlanMapper.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/data/WeeklyPlanRepositoryImpl.kt`

**File edited:** `AppDatabase.kt`

- [ ] Create entities:
  ```kotlin
  @Entity(tableName = "weekly_plans", indices = [Index(value = ["planName"], unique = true)])
  data class WeeklyPlanEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val planName: String)

  @Entity(tableName = "weekly_plan_slots")
  data class WeeklyPlanSlotEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val planId: Long,
      val dayIndex: Int,
      val recipeId: Int
  )
  ```

- [ ] Create DAOs with `@Insert(onConflict = REPLACE)`, `@Query` for get/delete operations.

- [ ] Create `WeeklyPlanMapper.kt` â€” pure mapping functions; max 15 lines each.

- [ ] Create `WeeklyPlanRepositoryImpl` implementing `WeeklyPlanRepository`. Each function max 20 lines; extract helpers as needed.

- [ ] Wire `WeeklyPlanRepositoryImpl` into `AppModule` alongside the recipe dependencies.

- [ ] Run: `./gradlew :shared:commonTest --tests "*WeeklyPlanRepositoryImplTest"` â€” all pass. âœ“

---

### Step 5 â€” Write Presentation Layer Test

**File created:**
`shared/src/commonTest/kotlin/com/pixies/recetario/presentation/PlannerViewModelTest.kt`

- [ ] Mock all 4 planner use cases.

- [ ] **Initial load**: `getAllPlans()` returns 2 plans â†’ `PlannerState.Success(plans)`.

- [ ] **Select plan**: `viewModel.selectPlan(plan)` â†’ `selectedPlan` state updates; `getPlanByName` called.

- [ ] **Assign recipe to day**: `viewModel.assignRecipe(dayIndex = 2, recipe)` â†’ `savePlan` called with updated `daySlots`; state refreshed.

- [ ] **Delete plan**: `viewModel.deletePlan(planId)` â†’ `deletePlan` use case called; plan list refreshed.

- [ ] **Create new plan**: `viewModel.createPlan(name = "Semana Saludable")` â†’ `savePlan` called with empty slots; success state.

- [ ] **Error on load**: `getAllPlans` throws â†’ `PlannerState.Error`.

- [ ] Run â€” expect failures.

---

### Step 6 â€” Implement Presentation Layer

**Files created:**
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/planner/PlannerState.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/planner/PlannerViewModel.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/planner/PlannerView.kt`
- `shared/src/commonMain/kotlin/com/pixies/recetario/presentation/planner/PlanDetailView.kt`

**File edited:** `AppNavGraph.kt`

- [ ] Create sealed interfaces:
  ```kotlin
  sealed interface PlannerState {
      object Loading : PlannerState
      data class Success(val plans: List<WeeklyPlan>) : PlannerState
      data class Error(val exception: Exception) : PlannerState
  }

  sealed interface PlanDetailState {
      object Loading : PlanDetailState
      data class Success(val plan: WeeklyPlan, val availableRecipes: List<RecipeOverview>) : PlanDetailState
      data class Error(val exception: Exception) : PlanDetailState
  }
  ```

- [ ] Create `PlannerViewModel` with methods: `loadAll()`, `createPlan(name)`, `deletePlan(id)`, `selectPlan(plan)`.

- [ ] Create `PlannerView.kt`:
  - Lists all plans with name and summary (e.g. "3/7 days filled").
  - FAB to create new plan (dialog for name input).
  - Swipe-to-delete or delete icon per plan row.
  - Tap navigates to `Screen.PlannerDetail(planId)`.

- [ ] Create `PlanDetailView.kt`:
  - 7-row grid: `Mon â€¦ Sun` | recipe card (or empty slot placeholder).
  - Tapping an empty slot opens a recipe picker (reuses `RecipeCard` from HomeView).
  - Tapping a filled slot shows options: "Remove" or "Replace".

- [ ] Wire both views into `AppNavGraph`.

- [ ] Run: `./gradlew :shared:commonTest --tests "*PlannerViewModelTest"` â€” all pass. âœ“
- [ ] Run desktop: `./gradlew :desktopApp:run` â€” create a plan, assign recipes to days, verify persistence across restart.

---

---

# Compliance Checklist (verify before marking any step complete)

- [ ] All new production files live under `shared/src/commonMain/` (exceptions: platform engine actuals in `androidMain`/`jvmMain`)
- [ ] All new test files live under `shared/src/commonTest/`
- [ ] Every function is â‰¤ 30 lines; helpers extracted if exceeded
- [ ] No magic numbers or raw strings outside `const val` declarations
- [ ] No unused imports in any edited file
- [ ] All `when` expressions on `*State` types are exhaustive (no `else` branch)
- [ ] `./gradlew :shared:commonTest` is green before checking off any step
- [ ] `./gradlew build` is green before closing any phase

---

# Execution Order Summary

```
Phase 0  â†’  Phase 1  â†’  Phase 2  â†’  Phase 3  â†’  Phase 4
  0.1           1.1           2.1           3.1           4.1
  0.2           1.2           2.2           3.2           4.2
  0.3           1.3           2.3           3.3           4.3
  0.4           1.4           2.4           3.4           4.4
  0.5           1.5           2.5           3.5           4.5
  0.6           1.6           2.6           3.6           4.6
```

Each step within a phase must be completed in numeric order.  
Do not start a new phase until all steps of the previous phase are checked off and tests are green.


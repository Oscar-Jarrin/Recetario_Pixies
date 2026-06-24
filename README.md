# RecetarioPixies â€” Developer Onboarding Guide

A Kotlin Multiplatform recipe discovery and meal-planning app targeting **Android** and **Desktop (JVM)**.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Data Flow: Spoonacular API â†’ Compose UI](#data-flow-spoonacular-api--compose-ui)
4. [Screen Inventory](#screen-inventory)
5. [Local Database Schema](#local-database-schema)
6. [Running the App](#running-the-app)
7. [Running Tests](#running-tests)
8. [Adding a Feature â€” TDD Order](#adding-a-feature--tdd-order)
9. [Codebase Navigation Cheat Sheet](#codebase-navigation-cheat-sheet)
10. [Known Gotchas](#known-gotchas)

---

## Architecture Overview

RecetarioPixies follows **Clean MVVM** with three strict layers:

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  PRESENTATION (Compose UI + ViewModel)            â”‚
â”‚  shared/.../presentation/                         â”‚
â”‚  Â· Compose @Composable screens                   â”‚
â”‚  Â· ViewModels expose StateFlow<ScreenState>       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                     â”‚ UseCases only
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  DOMAIN (pure Kotlin)                             â”‚
â”‚  shared/.../domain/                               â”‚
â”‚  Â· UseCases (one per operation)                  â”‚
â”‚  Â· Repository interfaces                         â”‚
â”‚  Â· Domain models + custom exceptions             â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                     â”‚ implements interfaces
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  DATA                                             â”‚
â”‚  shared/.../data/                                 â”‚
â”‚  Â· RepositoryImpl (offline-first logic)          â”‚
â”‚  Â· Room entities + DAOs (local cache)            â”‚
â”‚  Â· Ktor HTTP client (Spoonacular API)            â”‚
â”‚  Â· Mappers (DTO â†” Entity â†” Domain)              â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

**Key invariants:**

- The domain layer imports **zero** Android, Room, or Ktor symbols.
- ViewModels call UseCases. They never call DAOs directly.
- All data flows `Remote â†’ Room â†’ ViewModel â†’ UI`. Room is always in the path.
- Network failure silently falls back to cached Room data. `RecipeNotFoundException` / `PlanNotFoundException` are thrown only when the cache is also empty.

**Dependency Injection**: Manual, no framework. `DependencyInjector` (`di/DependencyInjector.kt`) wires the entire graph at bootstrap and is passed into `App()` by the platform entry point.

---

## Project Structure

```
Recetario_Pixies/
â”œâ”€â”€ androidApp/                 Android entry point (MainActivity â†’ App())
â”œâ”€â”€ desktopApp/                 Desktop entry point (main.kt â†’ App())
â”œâ”€â”€ shared/
â”‚   â””â”€â”€ src/
â”‚       â”œâ”€â”€ commonMain/kotlin/com/pixies/recetario/
â”‚       â”‚   â”œâ”€â”€ App.kt                   Root Composable; sets up Coil ImageLoader
â”‚       â”‚   â”œâ”€â”€ di/
â”‚       â”‚   â”‚   â””â”€â”€ DependencyInjector.kt         Manual DI: wires DB, HTTP, repos, use cases
â”‚       â”‚   â”œâ”€â”€ data/
â”‚       â”‚   â”‚   â”œâ”€â”€ RecipeRepositoryImpl.kt
â”‚       â”‚   â”‚   â”œâ”€â”€ WeeklyPlanRepositoryImpl.kt
â”‚       â”‚   â”‚   â”œâ”€â”€ local/
â”‚       â”‚   â”‚   â”‚   â”œâ”€â”€ AppDatabase.kt   Room @Database declaration
â”‚       â”‚   â”‚   â”‚   â”œâ”€â”€ DatabaseBuilder.kt (expect)
â”‚       â”‚   â”‚   â”‚   â”œâ”€â”€ LocalConstants.kt
â”‚       â”‚   â”‚   â”‚   â”œâ”€â”€ dao/             5 DAO interfaces
â”‚       â”‚   â”‚   â”‚   â””â”€â”€ entity/          5 Room entity data classes
â”‚       â”‚   â”‚   â”œâ”€â”€ mapper/              Pure extension functions for DTOâ†”Entityâ†”Domain
â”‚       â”‚   â”‚   â””â”€â”€ remote/
â”‚       â”‚   â”‚       â”œâ”€â”€ SpoonacularApiService.kt     (interface)
â”‚       â”‚   â”‚       â”œâ”€â”€ KtorSpoonacularApiService.kt (implementation)
â”‚       â”‚   â”‚       â”œâ”€â”€ SpoonacularHttpClient.kt     (Ktor client builder + quota interceptor)
â”‚       â”‚   â”‚       â”œâ”€â”€ SpoonacularApiConstants.kt
â”‚       â”‚   â”‚       â”œâ”€â”€ HttpEngineFactory.kt (expect)
â”‚       â”‚   â”‚       â””â”€â”€ dto/             Kotlinx.serialization DTOs
â”‚       â”‚   â”œâ”€â”€ domain/
â”‚       â”‚   â”‚   â”œâ”€â”€ exception/           4 typed exceptions
â”‚       â”‚   â”‚   â”œâ”€â”€ model/               4 domain data classes
â”‚       â”‚   â”‚   â”œâ”€â”€ repository/          2 repository interfaces
â”‚       â”‚   â”‚   â””â”€â”€ usecase/             7 UseCase classes
â”‚       â”‚   â””â”€â”€ presentation/
â”‚       â”‚       â”œâ”€â”€ RecipeImage.kt       Shared AsyncImage wrapper (Coil3)
â”‚       â”‚       â”œâ”€â”€ ViewConstants.kt     Shared UI string constants
â”‚       â”‚       â”œâ”€â”€ detail/              RecipeDetailView + RecipeDetailViewModel + DetailState
â”‚       â”‚       â”œâ”€â”€ home/                HomeView + HomeViewModel + HomeState + RecipeCard
â”‚       â”‚       â”œâ”€â”€ navigation/          AppNavGraph + BottomNavBar + Screen
â”‚       â”‚       â”œâ”€â”€ planner/             PlannerView + PlanDetailView + PlannerViewModel + States
â”‚       â”‚       â””â”€â”€ search/              SearchView + SearchViewModel + SearchState
â”‚       â”œâ”€â”€ commonTest/                  All unit tests (MockK)
â”‚       â”œâ”€â”€ androidMain/                 Android platform actuals (OkHttp engine, Room builder)
â”‚       â””â”€â”€ jvmMain/                     Desktop platform actuals (CIO engine, Room + BundledSQLite)
â””â”€â”€ gradle/libs.versions.toml           Centralised version catalog
```

---

## Data Flow: Spoonacular API â†’ Compose UI

The following walkthrough traces the Home screen loading its recipe grid.

### Step 1 â€” Platform entry point bootstraps `DependencyInjector`

```kotlin
// desktopApp/src/.../main.kt (Desktop)
fun main() {
    application {
        Window(...) {
            App(module = DependencyInjector(apiKey = "YOUR_KEY"))
        }
    }
}
```

`DependencyInjector` constructs the entire dependency graph: Ktor `HttpClient` â†’ `KtorSpoonacularApiService` â†’ Room `AppDatabase` â†’ DAOs â†’ `RecipeRepositoryImpl` â†’ `GetRandomRecipesUseCase`.

### Step 2 â€” `AppNavGraph` creates `HomeViewModel`

```kotlin
// AppNavGraph.kt
val viewModel: HomeViewModel = viewModel {
    HomeViewModel(module.getRandomRecipesUseCase)
}
```

`viewModel { }` (Compose lifecycle-aware) ensures the instance survives recompositions.

### Step 3 â€” `HomeViewModel.init` launches the coroutine

```kotlin
init { load() }

private fun load() {
    viewModelScope.launch {
        _state.value = HomeState.Loading
        _state.value = runCatching { HomeState.Success(getRandomRecipes()) }
            .getOrElse { HomeState.Error(it as Exception) }
    }
}
```

### Step 4 â€” `GetRandomRecipesUseCase` delegates to the repository

```kotlin
suspend operator fun invoke(): List<RecipeOverview> =
    repository.getRandomRecipes(RANDOM_RECIPES_COUNT)   // count = 10
```

### Step 5 â€” `RecipeRepositoryImpl` tries the network first

```kotlin
override suspend fun getRandomRecipes(count: Int): List<RecipeOverview> =
    runCatching { fetchAndCache(count) }.getOrElse { fallbackToCache() }

private suspend fun fetchAndCache(count: Int): List<RecipeOverview> {
    val dtos = api.getRandomRecipes(count)          // Ktor â†’ Spoonacular /recipes/random
    overviewDao.insertAll(dtos.map { it.toEntity() })   // cache to Room
    ingredientDao.insertAll(dtos.flatMap { it.toIngredientEntities() })
    return dtos.map { it.toDomain() }               // return domain models
}

private suspend fun fallbackToCache(): List<RecipeOverview> =
    overviewDao.getAllRecipes().map { it.toDomain() }
```

The Ktor client appends `?apiKey=...` automatically via an `HttpSend` interceptor. If `X-API-Quota-Left: 0` is returned in headers, `QuotaExhaustedException` is thrown and the offline fallback activates.

### Step 6 â€” Domain models flow back to the ViewModel state

```kotlin
_state.value = HomeState.Success(listOf(RecipeOverview(id=..., title=..., imageUrl=...), ...))
```

### Step 7 â€” `HomeView` reacts to the state

```kotlin
val state by viewModel.state.collectAsState()

when (val s = state) {
    HomeState.Loading    -> CircularProgressIndicator()
    is HomeState.Success -> LazyVerticalGrid { items(s.recipes) { RecipeCard(it) } }
    is HomeState.Error   -> ErrorContent(s.exception.message, onRetry = viewModel::retry)
}
```

### Step 8 â€” `RecipeCard` renders the image via Coil3

```kotlin
// RecipeCard â†’ RecipeImage â†’ AsyncImage
AsyncImage(
    model = recipe.imageUrl,          // e.g. https://img.spoonacular.com/recipes/716429-556x370.jpg
    contentScale = ContentScale.Crop,
    fallback = painterResource(Res.drawable.default_recipe),
    error = painterResource(Res.drawable.default_recipe)
)
```

Coil fetches the image via Ktor, decodes it on a background thread, caches in memory and on disk, and hands the bitmap back to Compose for rendering.

---

## Screen Inventory

| Screen | Route | ViewModel | What it does |
|---|---|---|---|
| `HomeView` | `home` | `HomeViewModel` | 2-column grid of recipe cards fetched randomly from Spoonacular |
| `SearchView` | `search` | `SearchViewModel` | Comma-separated ingredient search; results show used/missing/unused chips |
| `RecipeDetailView` | `recipe/{id}` | `RecipeDetailViewModel` | Step-by-step cooking instructions with checkable steps |
| `PlannerView` | `planner` | `PlannerViewModel` | List of saved weekly meal plans; create/delete plans |
| `PlanDetailView` | `planner/{planId}` | `PlannerViewModel` (shared) | 7-day grid; tap a day to open a bottom sheet embedding `SearchView` for recipe assignment |

Navigation uses Jetpack Navigation Compose. A `BottomNavBar` shows on the three top-level tabs (Home, Search, Planner).

---

## Local Database Schema

Database name: `recetario_pixies.db` Â· Current version: **3**

```sql
-- Recipe cache (populated from Spoonacular /recipes/random)
CREATE TABLE recipe_overviews (
    id              INTEGER PRIMARY KEY,
    title           TEXT NOT NULL,
    imageUrl        TEXT NOT NULL,
    readyInMinutes  INTEGER NOT NULL,
    dishType        TEXT NOT NULL
);

-- Flat ingredient list per recipe (enables offline ingredient search)
CREATE TABLE recipe_ingredients (
    rowId          INTEGER PRIMARY KEY AUTOINCREMENT,
    recipeId       INTEGER NOT NULL,
    ingredientName TEXT NOT NULL
);

-- Cooking steps per recipe, grouped by optional section name
CREATE TABLE recipe_instruction_steps (
    rowId     INTEGER PRIMARY KEY AUTOINCREMENT,
    recipeId  INTEGER NOT NULL,
    groupName TEXT NOT NULL,
    stepOrder INTEGER NOT NULL,
    stepText  TEXT NOT NULL
);

-- User-created meal plan headers
CREATE TABLE weekly_plans (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    planName TEXT NOT NULL UNIQUE
);
CREATE UNIQUE INDEX idx_weekly_plans_name ON weekly_plans (planName);

-- Junction: which recipe is assigned to which day of which plan
CREATE TABLE weekly_plan_slots (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    planId    INTEGER NOT NULL,   -- references weekly_plans.id
    dayIndex  INTEGER NOT NULL,   -- 0=Mon â€¦ 6=Sun
    recipeId  INTEGER NOT NULL    -- references recipe_overviews.id
);
```

> **Migration warning**: `fallbackToDestructiveMigration(true)` is active on both platforms. A schema version bump wipes all data. Before shipping, implement explicit `Migration` objects for any additive changes.

---

## Running the App

### Prerequisites

- JDK 17+
- Android SDK (if building the Android target)
- A [Spoonacular API key](https://spoonacular.com/food-api) (free tier available)

### Desktop (primary dev target)

```bash
# Set your API key in desktopApp/src/main/kotlin/main.kt
./gradlew :desktopApp:run

# With hot reload
./gradlew :desktopApp:hotRun --auto
```

### Android

```bash
./gradlew :androidApp:installDebug
```

### Full build

```bash
./gradlew build
```

### Clean

```bash
./gradlew clean
```

---

## Running Tests

```bash
# All shared unit tests (runs on JVM â€” no emulator needed)
./gradlew :shared:commonTest

# Single test class
./gradlew :shared:commonTest --tests "*HomeViewModelTest"

# With verbose output
./gradlew :shared:commonTest --info
```

All tests in `shared/src/commonTest/` use MockK and `kotlinx-coroutines-test`. They run on the JVM and require no Android emulator or device.

---

## Adding a Feature â€” TDD Order

Follow this strict sequence to stay consistent with the project's architectural conventions.

### 1. Write tests first in `commonTest/`

```
shared/src/commonTest/kotlin/com/pixies/recetario/
â”œâ”€â”€ presentation/  â† ViewModel tests (mock the UseCase)
â”œâ”€â”€ domain/        â† UseCase tests (mock the Repository interface)
â””â”€â”€ data/          â† RepositoryImpl tests (mock the DAOs and API service)
```

### 2. Define domain models (if new data is needed)

Add a `data class` in `shared/.../domain/model/`. No Room, no Ktor imports allowed here.

### 3. Add the repository method to the interface

Edit the appropriate interface in `domain/repository/`. This is the only cross-layer contract.

### 4. Implement the UseCase

One class per file in `domain/usecase/`. Use `operator fun invoke()`. Keep it under 10 lines â€” any logic heavier than delegation belongs in the repository.

### 5. Implement the repository method

Add the method to `RecipeRepositoryImpl` or `WeeklyPlanRepositoryImpl`. Follow the pattern:

```kotlin
override suspend fun newMethod(): ReturnType =
    runCatching { fetchFromApi() }.getOrElse { fallbackToCache() }
```

### 6. Add mapper functions if new DTOs or entities are introduced

Add pure extension functions to the relevant mapper file in `data/mapper/`. No suspend, no I/O inside mappers.

### 7. Implement the ViewModel

- Expose `StateFlow<ScreenState>` where `ScreenState` is a `sealed interface` with `Loading`, `Success`, `Error` branches.
- Call only UseCases from `viewModelScope.launch { }`.

### 8. Build the Compose screen

- Accept the ViewModel as a parameter.
- Use `collectAsState()` to observe state.
- Use `when (val s = state)` with exhaustive branches.
- Extract sub-composables for Loading, Success content, and Error content.
- Register the screen route in `Screen.kt` and `AppNavGraph.kt`.
- Wire the ViewModel factory in `DependencyInjector.kt`.

### 9. Verify all tests pass

```bash
./gradlew :shared:commonTest
```

---

## Codebase Navigation Cheat Sheet

| "I need to findâ€¦" | Go to |
|---|---|
| How the database is set up | `data/local/AppDatabase.kt` + platform actuals in `androidMain/` and `jvmMain/` |
| A DAO for recipe data | `data/local/dao/RecipeOverviewDao.kt` or `RecipeIngredientDao.kt` |
| The offline ingredient search logic | `data/mapper/IngredientSearchMapper.kt` â†’ `categoriseOffline()` |
| Spoonacular API quota handling | `data/remote/SpoonacularHttpClient.kt` â†’ `installInterceptor()` |
| How images are loaded | `presentation/RecipeImage.kt` â†’ `AsyncImage`; Coil init in `App.kt` |
| Navigation routes | `presentation/navigation/Screen.kt` |
| How ViewModels are wired | `presentation/navigation/AppNavGraph.kt` |
| All dependencies and versions | `gradle/libs.versions.toml` |
| The full DI graph | `di/DependencyInjector.kt` |
| Custom exceptions | `domain/exception/AppExceptions.kt` |

---

## Known Gotchas

| Issue | Detail |
|---|---|
| **Destructive migration active** | Any Room schema version bump wipes all user data including meal plans. Implement `Migration` objects before any public release. |
| **Navigation args via `remember`** | `pendingRecipeId`, `pendingRecipeTitle`, `pendingRecipeImageUrl` in `AppNavGraph` are `remember { }` state, not real NavArgs. They reset on process death. Use `navArgument` for production robustness. |
| **Coil disk cache unbounded on Desktop** | The default `ImageLoader` has no disk cache size limit. Configure `DiskCache.Builder().maxSizeBytes(...)` in `App.kt`. |
| **No Room FK constraints** | `WeeklyPlanSlotEntity.planId` and `.recipeId` lack `@ForeignKey` declarations. Orphaned rows are possible if a delete is interrupted mid-operation. |

---

> *Original Kotlin Multiplatform project scaffold â€” targeting Android, Desktop (JVM).*

This is a Kotlin Multiplatform project targeting Android, Desktop (JVM).

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code thatâ€™s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Appleâ€™s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- Desktop tests: `./gradlew :shared:jvmTest`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)â€¦
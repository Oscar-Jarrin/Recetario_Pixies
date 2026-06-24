# RecetarioPixies — Developer Onboarding Guide

A Kotlin Multiplatform recipe discovery and meal-planning app targeting **Android** and **Desktop (JVM)**.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Data Flow: Spoonacular API → Compose UI](#data-flow-spoonacular-api--compose-ui)
4. [Screen Inventory](#screen-inventory)
5. [Local Database Schema](#local-database-schema)
6. [Running the App](#running-the-app)
7. [Running Tests](#running-tests)
8. [Adding a Feature — TDD Order](#adding-a-feature--tdd-order)
9. [Codebase Navigation Cheat Sheet](#codebase-navigation-cheat-sheet)
10. [Known Gotchas](#known-gotchas)

---

## Architecture Overview

RecetarioPixies follows **Clean MVVM** with three strict layers:

```
┌──────────────────────────────────────────────────┐
│  PRESENTATION (Compose UI + ViewModel)            │
│  shared/.../presentation/                         │
│  · Compose @Composable screens                   │
│  · ViewModels expose StateFlow<ScreenState>       │
└────────────────────┬─────────────────────────────┘
                     │ UseCases only
┌────────────────────▼─────────────────────────────┐
│  DOMAIN (pure Kotlin)                             │
│  shared/.../domain/                               │
│  · UseCases (one per operation)                  │
│  · Repository interfaces                         │
│  · Domain models + custom exceptions             │
└────────────────────┬─────────────────────────────┘
                     │ implements interfaces
┌────────────────────▼─────────────────────────────┐
│  DATA                                             │
│  shared/.../data/                                 │
│  · RepositoryImpl (offline-first logic)          │
│  · Room entities + DAOs (local cache)            │
│  · Ktor HTTP client (Spoonacular API)            │
│  · Mappers (DTO ↔ Entity ↔ Domain)              │
└──────────────────────────────────────────────────┘
```

**Key invariants:**

- The domain layer imports **zero** Android, Room, or Ktor symbols.
- ViewModels call UseCases. They never call DAOs directly.
- All data flows `Remote → Room → ViewModel → UI`. Room is always in the path.
- Network failure silently falls back to cached Room data. `RecipeNotFoundException` / `PlanNotFoundException` are thrown only when the cache is also empty.

**Dependency Injection**: Manual, no framework. `AppModule` (`di/AppModule.kt`) wires the entire graph at bootstrap and is passed into `App()` by the platform entry point.

---

## Project Structure

```
Recetario_Pixies/
├── androidApp/                 Android entry point (MainActivity → App())
├── desktopApp/                 Desktop entry point (main.kt → App())
├── shared/
│   └── src/
│       ├── commonMain/kotlin/com/pixies/recetario/
│       │   ├── App.kt                   Root Composable; sets up Coil ImageLoader
│       │   ├── di/
│       │   │   └── AppModule.kt         Manual DI: wires DB, HTTP, repos, use cases
│       │   ├── data/
│       │   │   ├── RecipeRepositoryImpl.kt
│       │   │   ├── WeeklyPlanRepositoryImpl.kt
│       │   │   ├── local/
│       │   │   │   ├── AppDatabase.kt   Room @Database declaration
│       │   │   │   ├── DatabaseBuilder.kt (expect)
│       │   │   │   ├── LocalConstants.kt
│       │   │   │   ├── dao/             5 DAO interfaces
│       │   │   │   └── entity/          5 Room entity data classes
│       │   │   ├── mapper/              Pure extension functions for DTO↔Entity↔Domain
│       │   │   └── remote/
│       │   │       ├── SpoonacularApiService.kt     (interface)
│       │   │       ├── KtorSpoonacularApiService.kt (implementation)
│       │   │       ├── SpoonacularHttpClient.kt     (Ktor client builder + quota interceptor)
│       │   │       ├── SpoonacularApiConstants.kt
│       │   │       ├── HttpEngineFactory.kt (expect)
│       │   │       └── dto/             Kotlinx.serialization DTOs
│       │   ├── domain/
│       │   │   ├── exception/           4 typed exceptions
│       │   │   ├── model/               4 domain data classes
│       │   │   ├── repository/          2 repository interfaces
│       │   │   └── usecase/             7 UseCase classes
│       │   └── presentation/
│       │       ├── RecipeImage.kt       Shared AsyncImage wrapper (Coil3)
│       │       ├── ViewConstants.kt     Shared UI string constants
│       │       ├── detail/              RecipeDetailView + RecipeDetailViewModel + DetailState
│       │       ├── home/                HomeView + HomeViewModel + HomeState + RecipeCard
│       │       ├── navigation/          AppNavGraph + BottomNavBar + Screen
│       │       ├── planner/             PlannerView + PlanDetailView + PlannerViewModel + States
│       │       └── search/              SearchView + SearchViewModel + SearchState
│       ├── commonTest/                  All unit tests (MockK)
│       ├── androidMain/                 Android platform actuals (OkHttp engine, Room builder)
│       └── jvmMain/                     Desktop platform actuals (CIO engine, Room + BundledSQLite)
└── gradle/libs.versions.toml           Centralised version catalog
```

---

## Data Flow: Spoonacular API → Compose UI

The following walkthrough traces the Home screen loading its recipe grid.

### Step 1 — Platform entry point bootstraps `AppModule`

```kotlin
// desktopApp/src/.../main.kt (Desktop)
fun main() {
    application {
        Window(...) {
            App(module = AppModule(apiKey = "YOUR_KEY"))
        }
    }
}
```

`AppModule` constructs the entire dependency graph: Ktor `HttpClient` → `KtorSpoonacularApiService` → Room `AppDatabase` → DAOs → `RecipeRepositoryImpl` → `GetRandomRecipesUseCase`.

### Step 2 — `AppNavGraph` creates `HomeViewModel`

```kotlin
// AppNavGraph.kt
val viewModel: HomeViewModel = viewModel {
    HomeViewModel(module.getRandomRecipesUseCase)
}
```

`viewModel { }` (Compose lifecycle-aware) ensures the instance survives recompositions.

### Step 3 — `HomeViewModel.init` launches the coroutine

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

### Step 4 — `GetRandomRecipesUseCase` delegates to the repository

```kotlin
suspend operator fun invoke(): List<RecipeOverview> =
    repository.getRandomRecipes(RANDOM_RECIPES_COUNT)   // count = 10
```

### Step 5 — `RecipeRepositoryImpl` tries the network first

```kotlin
override suspend fun getRandomRecipes(count: Int): List<RecipeOverview> =
    runCatching { fetchAndCache(count) }.getOrElse { fallbackToCache() }

private suspend fun fetchAndCache(count: Int): List<RecipeOverview> {
    val dtos = api.getRandomRecipes(count)          // Ktor → Spoonacular /recipes/random
    overviewDao.insertAll(dtos.map { it.toEntity() })   // cache to Room
    ingredientDao.insertAll(dtos.flatMap { it.toIngredientEntities() })
    return dtos.map { it.toDomain() }               // return domain models
}

private suspend fun fallbackToCache(): List<RecipeOverview> =
    overviewDao.getAllRecipes().map { it.toDomain() }
```

The Ktor client appends `?apiKey=...` automatically via an `HttpSend` interceptor. If `X-API-Quota-Left: 0` is returned in headers, `QuotaExhaustedException` is thrown and the offline fallback activates.

### Step 6 — Domain models flow back to the ViewModel state

```kotlin
_state.value = HomeState.Success(listOf(RecipeOverview(id=..., title=..., imageUrl=...), ...))
```

### Step 7 — `HomeView` reacts to the state

```kotlin
val state by viewModel.state.collectAsState()

when (val s = state) {
    HomeState.Loading    -> CircularProgressIndicator()
    is HomeState.Success -> LazyVerticalGrid { items(s.recipes) { RecipeCard(it) } }
    is HomeState.Error   -> ErrorContent(s.exception.message, onRetry = viewModel::retry)
}
```

### Step 8 — `RecipeCard` renders the image via Coil3

```kotlin
// RecipeCard → RecipeImage → AsyncImage
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

Database name: `recetario_pixies.db` · Current version: **3**

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
    dayIndex  INTEGER NOT NULL,   -- 0=Mon … 6=Sun
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
# All shared unit tests (runs on JVM — no emulator needed)
./gradlew :shared:commonTest

# Single test class
./gradlew :shared:commonTest --tests "*HomeViewModelTest"

# With verbose output
./gradlew :shared:commonTest --info
```

All tests in `shared/src/commonTest/` use MockK and `kotlinx-coroutines-test`. They run on the JVM and require no Android emulator or device.

---

## Adding a Feature — TDD Order

Follow this strict sequence to stay consistent with the project's architectural conventions.

### 1. Write tests first in `commonTest/`

```
shared/src/commonTest/kotlin/com/pixies/recetario/
├── presentation/  ← ViewModel tests (mock the UseCase)
├── domain/        ← UseCase tests (mock the Repository interface)
└── data/          ← RepositoryImpl tests (mock the DAOs and API service)
```

### 2. Define domain models (if new data is needed)

Add a `data class` in `shared/.../domain/model/`. No Room, no Ktor imports allowed here.

### 3. Add the repository method to the interface

Edit the appropriate interface in `domain/repository/`. This is the only cross-layer contract.

### 4. Implement the UseCase

One class per file in `domain/usecase/`. Use `operator fun invoke()`. Keep it under 10 lines — any logic heavier than delegation belongs in the repository.

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
- Wire the ViewModel factory in `AppModule.kt`.

### 9. Verify all tests pass

```bash
./gradlew :shared:commonTest
```

---

## Codebase Navigation Cheat Sheet

| "I need to find…" | Go to |
|---|---|
| How the database is set up | `data/local/AppDatabase.kt` + platform actuals in `androidMain/` and `jvmMain/` |
| A DAO for recipe data | `data/local/dao/RecipeOverviewDao.kt` or `RecipeIngredientDao.kt` |
| The offline ingredient search logic | `data/mapper/IngredientSearchMapper.kt` → `categoriseOffline()` |
| Spoonacular API quota handling | `data/remote/SpoonacularHttpClient.kt` → `installInterceptor()` |
| How images are loaded | `presentation/RecipeImage.kt` → `AsyncImage`; Coil init in `App.kt` |
| Navigation routes | `presentation/navigation/Screen.kt` |
| How ViewModels are wired | `presentation/navigation/AppNavGraph.kt` |
| All dependencies and versions | `gradle/libs.versions.toml` |
| The full DI graph | `di/AppModule.kt` |
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

> *Original Kotlin Multiplatform project scaffold — targeting Android, Desktop (JVM).*

This is a Kotlin Multiplatform project targeting Android, Desktop (JVM).

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
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

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
# CLAUDE.md

**RecetarioPixies**: Kotlin Multiplatform (KMP) recipe app targeting Android & Desktop. Clean MVVM architecture, TDD-first development, Desktop-priority multiplatform strategy.

## Architecture: Clean MVVM

**Layer Structure** (see diagram.png):
- **Presentation**: View (Compose UI) → ViewModel (state management, Kotlin pure)
- **Domain**: UseCase (business logic), Repository interface (abstraction)
- **Data**: RepositoryImpl, Room (local persistence), Ktor HTTP client (Spoonacular API)

**Offline-First Cache Strategy** (mandatory):
- Repository fetches from remote source first, caches result to Room on success
- On network failure or no connectivity, Repository falls back to Room cached data
- All data flows: Remote → Room → ViewModel → UI (never skip Room)
- Stale data is acceptable when offline
- Network errors must be surfaced to ViewModel for proper UI state handling

**Constraints**:
- All code in `shared/src/commonMain/` unless platform-specific bindings are unavoidable (then `androidMain/` or `jvmMain/`)
- Use `expect/actual` for platform abstractions
- No service locators; pure Kotlin DI at app entry points
- One UseCase per business operation (no god-objects)
- Repository is the only data access point; no direct Room calls outside it

## Tech Stack

| Layer | Tech |
|-------|------|
| Testing | kotlin-test, MockK (all test doubles + verification) |
| Persistence | Room (local data only) |
| HTTP | Ktor (Spoonacular API client) |
| DI | Pure Kotlin (manual factory wiring) |
| State Mgmt | ViewModel + Compose State |

## TDD: Test-First, Mandatory Coverage

**Write tests FIRST** in `shared/src/commonTest/`:
1. **ViewModels**: All. Mock repositories, verify state emissions & side effects.
2. **UseCases**: All. Mock repositories, verify business logic, edge cases.
3. **Repository**: RepositoryImpl & LocalDataSource. Mock Room DAOs, verify data flow.
4. **Room**: All DAOs. Integration tests using in-memory database.

**Scope**: Happy path + relevant edge cases (null handling, empty lists, API errors).

**No test code in production** (`commonMain`). Always use MockK for test doubles.

## Build & Test Commands

```bash
./gradlew build                              # Build all
./gradlew :desktopApp:run                   # Run Desktop (primary dev target)
./gradlew :desktopApp:hotRun --auto         # Hot reload
./gradlew :shared:commonTest                # Run all shared unit tests
./gradlew :shared:commonTest --tests "*NameTest"  # Run single test class
./gradlew :androidApp:installDebug          # Install Android APK
./gradlew clean                              # Clean build artifacts
```

## Directory Map

```
shared/src/
├── commonMain/kotlin/
│   ├── presentation/  (Views, ViewModels)
│   ├── domain/        (UseCases, Repository interface)
│   └── data/          (RepositoryImpl, Room, Ktor client)
├── commonTest/        (Unit tests for all layers; Kotlin-test + MockK)
├── androidMain/       (Only if platform-specific UI bindings needed)
└── jvmMain/          (Desktop-only; minimal—prefer commonMain)
```

## Key Rules

### Architecture & Design
1. **Multiplatform First**: Write in `commonMain`. Desktop development is the primary focus.
2. **Immutable Data Models**: Use Kotlin `data class` in domain & data layers.
3. **No Static Singletons**: DI wiring at app bootstrap, pass dependencies explicitly.
4. **Repository Pattern**: All data access through Repository; no DAO calls from UseCase.
5. **ViewModel Scope**: Manages UI state only; never direct Room/API calls.
6. **Compose Resources**: In `commonMain/composeResources/`; auto-generated accessors (`Res.kt`).

### UI State Pattern (mandatory)
7. **Sealed State Interface**: Every screen must use `sealed interface ScreenState` or `sealed class ScreenState` with exactly three branches:
   - `Loading` — UI shows progress indicator
   - `Success(data)` — UI displays content
   - `Error(exception)` — UI shows error message with retry option
8. **Zero Unhandled States**: All state branches must be explicitly handled in UI composables (Kotlin exhaustive when). Empty lists are `Success(emptyList())`, not missing.
9. **State Emissions**: ViewModel exposes state via `StateFlow<ScreenState>`, not scattered LiveData or reactive chains.

### Clean Code Constraints (zero tolerance)
10. **Function Size Limit**: Max 30 lines per function. If function exceeds 30 lines, extract helper functions.
11. **Dead Code Purge**: Zero tolerance for unused imports, commented-out code blocks, or unreachable branches. Delete immediately.
12. **No Magic Numbers/Strings**: All numeric literals and string constants must be named constants in companion objects or top-level `const val`. UI strings must use resource localization or defined constants, never hardcoded.

## Gradle Conventions

- Type-safe accessors enabled: use `projects.shared`, `projects.androidApp`, etc.
- Compose compiler plugin in root `build.gradle.kts` (alias pattern)
- Dependency versions centralized in `libs.versions.toml`

## Adding a Feature (TDD Order)

1. Write `*Test.kt` in `commonTest/` (ViewModel, UseCase, Repository tests)
2. Implement domain layer (UseCase, Repository interface)
3. Implement data layer (RepositoryImpl, Room, Ktor)
4. Implement presentation (ViewModel, Compose Views)
5. Verify tests pass

## CI/CD

Not yet configured. Build & tests must pass locally before merge to `master`.



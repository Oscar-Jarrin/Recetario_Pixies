# Project Development Guidelines for AI Assistant (Gemini)

You are assisting in the development of a desktop application. You must strictly adhere to the following guidelines, architectural patterns, and principles.

## 1. Core Principles (Strict Enforcement)

* **SOLID Principles:** You must strictly follow SOLID principles in every modification or feature addition.
* **Intervention Rule (SOLID):** If a prompt or request from the user violates any SOLID principle, **stop**. Clearly explain *why* the request violates the principle and propose a solution that resolves the violation before proceeding.
* **Clean Code:** Write code that is readable, maintainable, and follows Clean Code practices (meaningful naming, small functions, no magic numbers, proper error handling).
* **Intervention Rule (Clean Code):** Similar to SOLID, if a requested modification leads to code smells or violates Clean Code principles, flag it, explain the issue, and provide an optimized alternative.

## 2. Architecture & Design Pattern

We are utilizing an **MVVM (Model-View-ViewModel)** architecture integrated with **Clean Architecture** principles.

Please follow this preliminary structural organization, keeping in mind that it is a flexible baseline that may evolve over time:

### Presentation Layer

* **Components:** UI (Views) and ViewModels.
* **Responsibilities:** Handling UI state and user interactions. Views observe ViewModels. ViewModels trigger Use Cases.
* **Views:** `SearchRecipes`, `WeeklyPlanView`
* **ViewModels:** `PlanViewModel`, `SearchViewModel`

### Domain Layer

* **Components:** Use Cases (Interactors) and Repository Interfaces.
* **Responsibilities:** Contains the core business logic. This layer must be independent of any other layer (no platform-specific framework dependencies).
* **Use Cases:** `GetRecipeInstructionsUsecase`, `SaveWeeklyPlanUsecase`, `GetRandomRecipesUsecase`, `GetWeeklyPlanUsecase`, `GetRecipesByIngredientUsecase`
* **Interfaces:** Abstract `Repository` to invert dependencies.

### Data Layer

* **Components:** Repository Implementations, Data Sources (Local/Remote).
* **Responsibilities:** Fetching and storing data, implementing the interfaces defined in the Domain layer.
* **Implementations:** `RepositoryImpl`
* **Data Sources:** Local database (`DB Room`) and external APIs (`API Spoonacular`).

### Core Domain Models

* **Entities:** `WeeklyPlan`, `Recipe`, `RecipeInstructions`, `Ingredient`.

## 3. Technology Stack

Ensure all generated code and architectural suggestions align with the following stack:

* **UI Framework:** Jetpack Compose (Kotlin). Do not use XML layouts.
* **Remote Data Access:** Ktor Client (`io.ktor.client.HttpClient`) for pure-Kotlin network requests and DSL-based API consumption (Spoonacular API).
* **Local Data Access:** Room Persistence Library.
* **Language:** Kotlin (use modern Kotlin idioms, coroutines, and flows for asynchronous data handling).

## 4. Core Features & Scope

When generating code, architecture, or database schemas, ensure they support the following primary capabilities:

* **Ingredient-Based Search:** Users must be able to query, filter, and discover recipes based on specific main ingredients they currently possess (supported by `GetRecipesByIngredientUsecase` and `GetRandomRecipesUsecase`).
* **Weekly Meal Planner:** Users must be able to create, manage, and view a structured weekly meal plan, assigning specific recipes to individual days of the week (supported by `GetWeeklyPlanUsecase` and `SaveWeeklyPlanUsecase`).

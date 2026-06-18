---
name: commit
description: Analyzes staged changes in git and generates a conventional commit message based on architectural layers.
argument-hint: "[optional context or overrides]"
---

## Instructions for Claude

1. Run `git diff --cached` to read the changes that are ready to be committed.
2. Identify which files were modified and which layer of Clean Architecture it belongs to in order to determine the **scope**.
3. Structure the message strictly following this format:
   `<type>(<scope>): <short description in lowercase and English>`

## Commit Types (`<type>`)
- `feature`: New screens, visual components, use cases, or repositories.
- `fix`: Bug fixes for UI, logic, dependency injection, or Gradle.
- `refactor`: Code cleanup or folder reorganization without changing behavior.
- `chore`: General maintenance tasks or dependencies.

## Rules for Scope (`<scope>`)
Instead of using platforms (like android/desktop), you must use a composite format of `<layer>:<component>` or the pure layer name as appropriate:
- **`view:<Component>`**: For the presentation layer (e.g., `view:HomeScreen`, `view:RecipeViewModel`, `view:Components`).
- **`domain:<Component>`**: For business rules in `commonMain` (e.g., `domain:RecipeModel`, `domain:GetRecipesUseCase`).
- **`data:<Component>`**: For persistence or external data consumption (e.g., `data:HttpClient`, `data:RecipeRepositoryImpl`, `data:LocalDb`).
- **`di`**: For modifications on the dependency injector file 
- **`config`** or **`deps`**: For modifications to Gradle configuration or the version catalog (`libs.versions.toml`).

## Execution Flow
- Display the proposed message inside a clean markdown code block.
- Ask the user if they want to apply the commit directly.
- If the user confirms, automatically run the terminal tool to execute `git commit -m "<generated_message>"`.
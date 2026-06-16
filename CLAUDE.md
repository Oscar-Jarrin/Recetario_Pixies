# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RecetarioPixies** is a Kotlin Multiplatform Project (KMP) targeting Android and Desktop (JVM) using Compose Multiplatform for UI. The app is a recipe browsing application with an architecture that follows Clean Architecture principles (domain, data, and presentation layers).

## Project Structure

```
RecetarioPixies/
├── shared/               # Kotlin Multiplatform shared library (compiled for all targets)
│   └── src/
│       ├── commonMain/   # Code shared across all platforms
│       ├── androidMain/  # Android-specific code
│       ├── jvmMain/      # Desktop (JVM)-specific code
│       ├── commonTest/   # Shared tests
│       ├── androidHostTest/ # Android-specific tests
│       └── jvmTest/      # Desktop-specific tests
├── androidApp/           # Android application module
├── desktopApp/           # Desktop Compose application
└── gradle/               # Gradle wrapper and configuration
```

### Target Platforms
- **Android**: Min SDK 21, Target SDK 34
- **Desktop**: JVM 11+
- **Shared Compose Runtime**: Used for UI across all platforms

## Build & Development Commands

### Build & Run

```bash
# Android
./gradlew :androidApp:assembleDebug        # Build debug APK
./gradlew :androidApp:installDebug         # Build and install to connected device/emulator

# Desktop
./gradlew :desktopApp:run                  # Run desktop app
./gradlew :desktopApp:hotRun --auto        # Hot reload mode for development
./gradlew :desktopApp:package              # Package for distribution
./gradlew :desktopApp:packageDistributionForCurrentOS  # Full distribution build

# Both
./gradlew build                            # Build all modules
./gradlew clean                            # Clean all build artifacts
```

### Testing

```bash
# Android (instrumented tests and unit tests)
./gradlew :shared:testAndroidHostTest      # Run Android host tests
./gradlew :shared:testDebugUnitTest        # Unit tests for Android

# Desktop (JVM)
./gradlew :shared:jvmTest                  # Run desktop/JVM tests
./gradlew :shared:jvmTest --tests "*TestClass*"  # Run specific test class

# All tests
./gradlew test                             # Run all tests
```

### Other Useful Commands

```bash
./gradlew dependencies                     # View dependency tree
./gradlew tasks                            # List all available Gradle tasks
./gradlew -Dorg.gradle.warning.mode=all build  # Show all deprecation warnings
```

## Key Architecture Notes

### Compose Multiplatform Setup
- **Shared UI code**: `shared/src/commonMain/` contains shared Compose UI and business logic
- **Platform-specific UI**: Platform-specific UIs go in `androidMain/` or `jvmMain/` if needed
- **Resources**: Compose resources are in `shared/src/commonMain/composeResources/`

### Module Organization
- **shared**: Library module used by all platforms. Contains domain logic, data layer, and UI code
- **androidApp**: Entry point for Android, depends on shared module
- **desktopApp**: Entry point for Desktop, depends on shared module

### Compose Compiler & Plugin Configuration
- Compose compiler plugin is configured in root `build.gradle.kts` (alias approach)
- Each module that uses Compose must declare the compose plugin in its `build.gradle.kts`
- Hot reload is supported on desktop via `./gradlew :desktopApp:hotRun --auto`

## Dependencies & Versions

Dependency versions are centralized in `libs.versions.toml` (not shown but follows Gradle's catalog pattern). Key dependencies include:
- **Compose Multiplatform**: Latest stable version
- **Compose Material3**: For Material Design UI
- **Jetpack Lifecycle**: ViewModel and Runtime Compose
- **Kotlin Test**: For unit testing (multiplatform)

## Important Notes

### Platform Abstractions
- Use `expect/actual` pattern for platform-specific implementations in Kotlin Multiplatform
- For UI, keep Compose code in commonMain when possible; only split if truly platform-different

### Gradle Conventions
- This project uses type-safe project accessors (enabled via `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`)
- You can reference modules as `projects.shared`, `projects.androidApp`, etc., instead of string literals

### No iOS Target
- Currently configured for Android and Desktop only; iOS is not included in `settings.gradle.kts`

### Compose Resources
- Resources (strings, images, etc.) are in `shared/src/commonMain/composeResources/`
- Generated resource accessors are auto-generated in build output (e.g., `Res.kt`)

## Testing Strategy

- **Unit tests**: In `commonTest/` for shared logic (using Kotlin Test)
- **Integration tests**: In `androidHostTest/` for Android and `jvmTest/` for Desktop
- Run tests via IDE gutter buttons or Gradle commands above
- Mock Android/Desktop platform code as needed for cross-platform testing

## Common Development Tasks

### Adding a New Feature
1. Implement shared code in `shared/src/commonMain/kotlin/`
2. If platform-specific logic is needed, add `expect` in common and `actual` in platform modules
3. Add unit tests in `commonTest/`
4. Test on both Android and Desktop

### Running in IDE
- Android: Use IDE run configurations or `./gradlew :androidApp:installDebug`
- Desktop: Use IDE run configurations or `./gradlew :desktopApp:run`
- Both support hot reload when configured in the IDE

### Debugging
- Android: Use standard Android Studio debugger
- Desktop: Use IDE debugger or attach to running process
- For Compose preview debugging, use Compose Preview pane in IDE (when available)

## Git Workflow

- Main branch: `master`
- Follow conventional commits when possible
- Build and tests should pass before merging (CI/CD not yet configured)

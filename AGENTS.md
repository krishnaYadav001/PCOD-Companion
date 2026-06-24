# Agent Instructions

This repository is PCOD Companion, a local-first Android wellness companion for women managing PCOD.

For detailed project guidance, read `CLAUDE.md` first. The most important rules are repeated here for quick reference.

## Core Rules

- Keep the app single-activity and Compose-only.
- Follow MVVM: each feature uses a `Screen` composable plus a `ViewModel`.
- ViewModels should use repositories instead of DAOs directly.
- Use `StateFlow` for screen state and `SharedFlow` for one-shot UI events.
- Use snackbars for feedback. Avoid modal popups for normal action confirmation.
- Suggestions must be opt-in and must not auto-mutate the user's plan.
- Keep the product tone calm, supportive, and non-judgmental.

## Database Discipline

Room schema version is currently 13. When changing entities:

1. Update the entity.
2. Add matching `@ColumnInfo(defaultValue = "...")` when migration SQL uses a default.
3. Bump the Room database version.
4. Add and register a migration in `AppDatabase.kt` and `AppModule.kt`.
5. Update backup import/export code when the field belongs in backup data.

Do not rely on destructive migration to fix schema validation mismatches.

## Build Checks

Run these before publishing changes:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On macOS/Linux:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Repository Hygiene

Do not commit local machine files, Gradle caches, build outputs, APKs, signing files, `.agents/`, or `.claude/`.

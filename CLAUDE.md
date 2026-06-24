# CLAUDE.md

Development guidance for AI assistants and future maintainers working on PCOD Companion.

## Project Identity

PCOD Companion is a local-first Android wellness companion for women managing PCOD. The product tone should stay calm, supportive, and non-judgmental. It is a tracking and reflection companion, not a diagnostic or treatment tool.

## Stack

- Kotlin 2.2.10
- Android Gradle Plugin 9.2.0
- Gradle 9.4.1
- JDK 21
- Jetpack Compose with Material 3
- Hilt and Hilt WorkManager integration
- Room database, schema version 13
- Navigation Compose
- WorkManager reminders and daily reset
- Single Gradle module: `app`

## Source Map

```text
app/src/main/java/com/pcodcompanion/
|-- PCODApplication.kt
|-- MainActivity.kt
|-- data/
|   |-- backup/
|   |-- insight/
|   |-- local/
|   |-- model/
|   `-- repository/
|-- di/
|-- navigation/
|-- ui/
|   |-- components/
|   |-- cycletracker/
|   |-- history/
|   |-- planbuilder/
|   |-- settings/
|   |-- splash/
|   |-- summary/
|   |-- theme/
|   `-- today/
`-- worker/
```

## Architecture Rules

- Keep the app single-activity and Compose-only.
- Use MVVM. Each feature screen should have a `Screen` composable and a `ViewModel`.
- ViewModels should use repositories, not DAOs directly.
- Keep UI state in `StateFlow` and one-shot messages in `SharedFlow`.
- Use `TodayViewModel.updateLogOptimistic { ... }` for Today-screen tracking updates so the UI responds immediately.
- Keep suggestions opt-in. Do not automatically mutate the user's plan from a suggestion.
- Use snackbars for action feedback instead of modal popups.

## Database Migration Rules

Room validates the final schema after every migration. Treat migrations as production code.

When adding a column:

1. Add the field to the Room entity.
2. If SQL uses a default value, add the matching `@ColumnInfo(defaultValue = "...")`.
3. Bump the Room database version in `AppDatabase.kt`.
4. Add a `MIGRATION_OLD_NEW` object.
5. Register the migration in `AppModule.provideDatabase()`.
6. Update backup import/export mapping when the field belongs in backups.

Default value conventions:

- Boolean fields use integer defaults: `defaultValue = "0"`.
- String fields use text defaults: `defaultValue = ""`.
- Number fields use integer or real defaults.

`fallbackToDestructiveMigration()` does not protect against post-migration schema mismatches.

## Backup Rules

Backups are versioned JSON in `BackupManager`.

- Current backup version: 3.
- When entity shape changes, update both `toJson()` and `fromJson()` paths.
- If the backup format changes incompatibly, bump `BACKUP_VERSION`.
- Reset/import-replace should clear derived insights so they can be regenerated.

## UI and Product Tone

- Use the app's pastel design language from `ui/theme`.
- Keep the app gentle, practical, and emotionally safe.
- Avoid shame, streak pressure, or alarmist language.
- Quiet Mode should hide suggestion cards and silence non-essential feedback, but tracking must remain usable.
- Common actions should stay within one or two taps.
- Use haptics for user-initiated state changes where the surrounding UI already follows that pattern.

## Reminder Rules

- `WorkManagerHelper.applyReminderSchedule(context, enabled, intensity)` is the single scheduling entry point.
- Reminder workers should decide whether to fire based on current state.
- Routine reminders must respect Quiet Mode.
- Re-engagement remains the safety net and is intentionally separate.
- Do not re-enable the default WorkManager initializer in the manifest.

## Build Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

## Repository Hygiene

- Do not commit `local.properties`, IDE folders, Gradle caches, build outputs, APKs, or local assistant folders.
- Keep the Gradle wrapper checked in.
- Prefer focused fixes over broad refactors.
- If changing health-related wording, keep it supportive and avoid medical claims.

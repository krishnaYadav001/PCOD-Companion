# PCOD Companion

PCOD Companion is a calm Android wellness app for women managing PCOD. It combines daily tracking, cycle-aware planning, gentle reminders, and pattern insights in one local-first Jetpack Compose app.

This is not a medical diagnosis tool. It is designed to help users notice habits, symptoms, cycles, mood, and routine patterns so they can make more informed day-to-day choices and share clearer summaries with a doctor.

## Highlights

- Today dashboard with water, sleep, mood, diet, exercise, medication, and plan progress.
- Low-energy mode that simplifies the day instead of pressuring the user.
- Cycle tracker with manual entries, flow level updates, and cycle-aware recommendations.
- Plan builder for recurring wellness tasks and mini-actions.
- Local pattern insights generated from recent logs and persisted with stable keys.
- Adaptive reminders using WorkManager that skip redundant notifications and back off when ignored.
- Quiet Mode to hide suggestion cards and silence non-essential feedback.
- History and summary screens for weekly progress, trends, and doctor-friendly export text.
- Backup and restore through versioned JSON export/import.

## Tech Stack

- Kotlin 2.2.10
- Android Gradle Plugin 9.2.0
- Gradle 9.4.1 with checked-in wrapper
- Jetpack Compose and Material 3
- Hilt for dependency injection
- Room database, schema version 13
- WorkManager for daily reset, reminders, and re-engagement
- Navigation Compose
- SharedPreferences for lightweight settings

## Architecture

The app is a single-activity, Compose-only Android application.

- `PCODApplication.kt` configures Hilt and WorkManager.
- `MainActivity.kt` hosts the Compose navigation graph.
- `data/local` contains Room entities, DAOs, and database migrations.
- `data/repository` keeps ViewModels away from direct DAO access.
- `data/backup` handles versioned JSON backup and restore.
- `data/insight` generates pattern-based insights.
- `ui/*` follows MVVM with `Screen` composables and `ViewModel` state holders.
- `worker/*` contains reset, reminder, notification, and scheduling logic.

## Project Structure

```text
app/src/main/java/com/pcodcompanion/
|-- data/
|-- di/
|-- navigation/
|-- ui/
`-- worker/
```

## Build and Validate

Use JDK 21 and Android SDK 34.

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

Validation on June 24, 2026:

- `:app:assembleDebug` passed.
- `:app:testDebugUnitTest` passed as no-source because no unit test sources are currently present.
- `:app:lintDebug` passed. Remaining lint warnings are version/update suggestions, icon resource cleanup suggestions, and a min-SDK resource-folder note.

## Data and Privacy

User data is stored locally in Room and SharedPreferences. Backup files are exported as JSON only when the user chooses to share or save them. The app does not include a network backend.

## Notes for Reviewers

The repository includes `CLAUDE.md` for future development handoff and AI-assisted maintenance notes. A project summary is also available in `PCOD-Project-Summary.pdf`.

# Incident Journal

Siddarth’s CriminalIntent project.

An Android notebook for recording office incidents, keeping notes and photos, and marking records as solved.

## Open and run

Open this directory in Android Studio. Use its bundled JDK and install Android SDK Platform 35 if requested. Run the `app` configuration on Android 7.0 (API 24) or newer. The Gradle wrapper is included.

Application and test sources are Kotlin. Layouts and other Android resources are XML. The package is `com.siddarth.criminalintent`; the local database is `office-notebook.db`.

## Using the notebook

Choose **New entry**, enter a title, and add optional notes. Set the date and time using the buttons below the notes. Mark the incident solved when it is resolved. **Save** writes the record to the device. Leaving an edited record asks whether to discard the draft. An empty title cannot be saved.

The notebook lists saved records by date. Search matches titles and notes; the All, Open, and Solved filters narrow the list. Tap a row to edit it. The editor's overflow menu contains Delete, with confirmation.

**Take a photo** opens the device camera. A readable replacement is kept in the draft until Save. An empty or unreadable camera result is rejected. Save is unavailable while a camera result is being processed. Canceling a camera capture preserves the previous attachment; discarding an edited draft preserves the saved record and its original photo. **Share report** opens Android's app chooser with the title, date, status, and notes. It shares the current draft text.

No network account or server is required. Camera capture requires a camera application. Photos and the Room database remain in this app's private storage and are removed when the app is uninstalled or its storage is cleared.

## Assignment coverage

| Requirement | Where it is handled |
| --- | --- |
| Scrollable title/date list | `NotebookFragment`, RecyclerView, `row_incident.xml` |
| Separate detail screen | `EntryFragment` in the activity's Fragment back stack |
| Add and delete | Notebook toolbar/button; editor confirmation dialog |
| Local persistence | `IncidentStore`, Room entity and DAO |
| Solved status in the list | Status label on each row and status filters |
| Editable date | `EntryClockDialog` DatePickerDialog |
| Camera attachment | TakePicture activity result, private evidence files, FileProvider |
| Implicit Intent | Text report using ACTION_SEND and an Android chooser |
| Rotation handling | ViewModels, SavedStateHandle drafts, restored DialogFragments |

Time selection uses a TimePickerDialog. Search and filter state also survive screen recreation. Database calls use Room's suspend APIs; image decoding runs on a background dispatcher.

## Checks

From this directory:

```sh
bash gradlew assembleDebug lintDebug
bash gradlew connectedDebugAndroidTest
```

Run connected tests on a development emulator: the tests clear this app's records and photos. They do not touch other app packages.

Build, lint, and all six instrumentation tests passed on an Android 15 / API 35 emulator on October 4, 2026. Lint reported 16 warnings and no errors.

The instrumentation suite covers draft rotation, required-title validation, explicit saving, reopening, date/time edits, report contents, deletion cancellation, a scrollable list, search/filter restoration, discarded drafts, camera success/cancellation, attachment cleanup, database reopening, note searches, and rejection of invalid camera output. Camera and share responses are simulated by the tests.

## Repository contents

Commit source, resources, schema, wrapper, and build configuration. Keep generated `build` folders, `.gradle`, `.idea`, `.kotlin`, and `local.properties` out of the repository. The supplied source archive excludes them.

The assignment requests an individual submission of the repository branch link. Finish reviewing before submitting: changes made after submission are not considered for grading.

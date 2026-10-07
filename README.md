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
| Separate detail screen | `EntryFragment` in a Navigation graph with Safe Args |
| Add and delete | Notebook toolbar/button; editor confirmation dialog |
| Local persistence | `IncidentStore`, Room entity and DAO |
| Solved status in the list | Status label on each row and status filters |
| Editable date | `EntryDateDialog` DatePickerDialog |
| Camera attachment | TakePicture activity result, private evidence files, FileProvider |
| Implicit Intent | Text report using ACTION_SEND and an Android chooser |
| Rotation handling | ViewModels, SavedStateHandle drafts, restored DialogFragments |

Time selection uses `EntryTimeDialog`, a separate DialogFragment containing a TimePickerDialog. Search and filter state also survive screen recreation. Database calls use Room's suspend APIs; image decoding runs on a background dispatcher.

## Class-note alignment

The app uses XML Views, view binding, Fragments, RecyclerView, ViewModels, coroutines, Room, and Navigation with Kotlin Safe Args. Room uses KAPT via `com.android.legacy-kapt`, the compatible plugin for this project's Android Gradle Plugin 9.4. Application and test code remain Kotlin; generated Room implementation files are build outputs.

The DayNight theme follows the device's light/dark setting. Explicit MaterialToolbars provide the app bars, so the theme uses `NoActionBar` to avoid a duplicate bar. Toolbar icons obtain their foreground from `colorOnSurface` and their background from `colorSurface`. This implements the purpose of the class app-bar notes while retaining the notebook layout.

Compatibility references: [Android's KAPT migration guidance](https://developer.android.com/build/migrate-to-built-in-kotlin) and [Navigation releases](https://developer.android.com/jetpack/androidx/releases/navigation).

## Challenge review

These are mappings to the fifth-edition textbook's published challenge topics, not a guarantee of instructor credit. Canvas's Project 2 challenge-information page was not available during review. The public previews do not expose every instruction for every exercise; the course-specific selection and any exclusions still need confirmation.

| Chapter/topic | App behavior and evidence |
| --- | --- |
| [11: Formatting the Date](https://www.oreilly.com/library/view/android-programming-the/9780137645794/ch11s07.html) | Rows use readable, locale-aware DateFormat output rather than raw timestamps. |
| [12: Addressing the Schema Warning](https://www.oreilly.com/library/view/android-programming-the/9780137645794/ch12s07.html) | Room exports its versioned schema to `app/schemas`; the processor receives `room.schemaLocation`. Full exercise text was not available in the public preview. |
| [13: No Untitled Crimes](https://www.oreilly.com/library/view/android-programming-the/9780137645794/ch13s05.html) | Blank or whitespace-only titles cannot be saved. Topic confirmed in the contents; detailed exercise text remains unverified. |
| [14: More Dialogs](https://www.oreilly.com/library/view/android-programming-the/9780137645794/ch14s03.html) | Separate date and time DialogFragments; the time button opens the time picker, with the chosen time persisted on Save. |
| [15: An Empty View for the RecyclerView](https://www.oreilly.com/library/view/android-programming-the/9780137645794/ch15s05.html) | An empty list shows a message and a creation button; the placeholder disappears when a saved record is available. |
| [15: Deleting Crimes](https://www.oreilly.com/library/view/android-programming-the/9780137645794/ch15s06.html) | A detail-screen menu action deletes the selected record after confirmation, then returns to the list. |

The posted optional Chapter 15 exercise also describes theme-responsive app-bar icon colors and offers two challenge credits. The implementation is tested in light and dark modes, but whether that older announcement applies to this submission must be confirmed. It is not included as two additional guaranteed credits here.

## Checks

From this directory:

```sh
bash gradlew assembleDebug lintDebug
bash gradlew connectedDebugAndroidTest
```

Run connected tests on a development emulator: the tests clear this app's records and photos. They do not touch other app packages.

Build, lint, and all nine instrumentation tests passed on an Android 15 / API 35 emulator during the latest verification. Lint reported 22 warnings and no errors.

The instrumentation suite covers draft rotation, required-title validation, explicit saving, reopening, date/time edits, report contents, deletion cancellation, a scrollable list, search/filter restoration, discarded drafts, camera success/cancellation, attachment cleanup, database reopening, note searches, rejection of invalid camera output, empty-list creation, backing out of a blank draft, rejection of whitespace-only titles, light/dark toolbar and button contrast, and draft retention during theme changes. Camera and share responses are simulated by the tests.

## Resource organization

Display text is in `res/values/journal_text.xml`, colors in `res/values/journal_palette.xml`, and the theme in `res/values/journal_theme.xml`. Night colors are in `res/values-night/journal_palette.xml`. Resource identifiers and app behavior are unchanged by these filenames.

## Repository contents

Commit source, resources, schema, wrapper, and build configuration. Keep generated `build` folders, `.gradle`, `.idea`, `.kotlin`, and `local.properties` out of the repository. The supplied source archive excludes them.

The assignment requests an individual submission of the repository branch link. Finish reviewing before submitting: changes made after submission are not considered for grading.

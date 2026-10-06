# Markdown Todo Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Native execution is recommended for this small app with closely coupled document interfaces; the user chooses the execution method after reviewing this plan.

**Goal:** Deliver a small, signed Android app and widget over a user-owned Markdown todo file, in increments the user can test on their Pixel 10 Pro with GrapheneOS.

**Architecture:** One app module. Pure Kotlin parsing, text mutations, and widget projection sit behind a SAF repository. Compose Activities handle document selection and editing; native RemoteViews handle the home-screen widget.

**Tech Stack:** JDK 21, Gradle 8.13, AGP 8.13.2, Kotlin and Compose compiler plugin 2.2.21, Compose BOM 2025.10.01, Activity Compose 1.11.0, coroutines-android 1.10.2, JUnit 4.13.2, Robolectric 4.16.1, AndroidX Test Core 1.7.0. Android compile/target API 36, Build Tools 36.0.0, minimum API 26; Java/Kotlin bytecode target 17. These artifact versions were checked against official Maven repositories during planning; the actual build remains to be verified.

**Spec:** [Approved design](../specs/2026-10-06-markdown-todo-design.md).

## Global Constraints

- The document is the source of truth; mutate the SAF document in place, never an app-private copy.
- No task database, account, sync engine, project model, reminder system, or background file watcher.
- Persistent app metadata consists of the selected document URI and per-widget display settings.
- Preserve unrelated text, UTF-8 BOM, existing line endings, tag spelling, case, tuple order, and duplicates.
- Exact date headings: `### DD.MM.YY`, valid calendar date, year 2000 + YY.
- Exact task prefixes: `- [ ] ` and `- [x] `; lowercase x only; no indentation.
- Tag token regex: `#[\p{L}\p{Nd}_/.!?+*<>=:-]+`; no quoting or escaping.
- Creation inheritance is an explicit toggle, off by default; no automatic matching-tag compression.
- Tag edits propagate through the following inheritance chain. Moves materialize its first source successor.
- Dates display newest first. Completion outranks tags in incomplete-first mode. Sorting never writes the file.
- Use the existing isolated checkout at `/workspace/muh-todo`; do not create a worktree.
- Run verification commands from `/workspace/muh-todo` after sourcing `/workspace/.muh-todo/env.sh`, created in Task 1 to activate the JDK, SDK, and cache paths.
- No Google Play services or unnecessary permissions. No production signing or Play Store distribution.
- User amendment: readable multiline test fixtures; a pinned Docker build and archiveable offline build environment for rebuilding years later on a newer Linux host.
- Setup scripts may install tools and refresh outputs, but must not rewrite source, manifests, dependency declarations, or lockfiles once implemented.

## Review Focus

1. An editor remains open while the selected document changes: reject saving into another document (Task 3).
2. A literal body starts with metadata-like text: reject an unrepresentable draft instead of silently changing its body/tags (Tasks 2 and 4).
3. Supplementary-plane letters and numeric-leading tags: accept the agreed grammar and sort by code points, not locale or UTF-16 units (Tasks 1 and 6).
4. Stale widget actions or malformed action extras: reject the action instead of toggling a different task or crashing (Tasks 3 and 5).
5. URI grants and APK identity survive process restarts and app updates: reload preferences and reuse the signing key; verify actual behavior on the phone (Tasks 3 and 7).

## Execution status

Phone feedback amendment on 2026-10-06: use the `muh todo` display name, dark theme, slightly smaller text, and supplied launcher artwork (0.1.1). The user approved that update and requested Increment 2. Remove the app's Open in text editor action in this increment; the user opens the file independently.

Increment 1 (Tasks 1–5) implemented and checked with 84 passing tests, build/lint, independent review fixes, and a private offline build image. Delivery is the hands-on checkpoint below. Tasks 6–7 remain pending phone feedback; README and the phone checklist were brought forward to support the first handoff. Evidence: [Increment 1 validation](../../increment1-validation.md).

## Increments and file ownership

**Increment 1 — Tasks 1–5:** a functional APK with document selection, external-editor access, creation/editing, inheritance, date moves, and a refreshable widget with direct checkboxes. Display uses newest-first dates and original task order. Advanced sorting controls arrive in Increment 2.

**Hands-on checkpoint:** deliver Increment 1 and wait for the user's phone test before advancing to Task 6. Diagnose feedback within the agreed scope; update this plan when feedback changes the remaining work. Do not interpret an untested APK as verified GrapheneOS behavior.

**Increment 2 — Task 6:** per-widget sorting controls and final tag-run display, followed by a second hands-on checkpoint.

**Increment 3 — Task 7:** full validation, repeatable cloud setup, and concise user/testing instructions incorporating device feedback.

All product Kotlin paths below are relative to `app/src/main/java/dev/uberdever/muhtodo/`; tests use the same package under `app/src/test/java/`. Namespace/application ID: `dev.uberdever.muhtodo`. Display name: `muh todo`.

### Task 1: Buildable project and strict parser

**Files:** Create `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, the Gradle wrapper files, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`; `document/TodoModels.kt`, `document/TagSyntax.kt`, `document/TodoParser.kt`; tests `document/TodoParserTest.kt`.

**Interfaces produced:**
- `ParsedDocument(source: String, sections: List<DateSection>, tasks: List<Todo>, knownTags: List<String>)`.
- `DateSection(date: LocalDate, headerLineIndex: Int, endLineExclusive: Int)`.
- `Todo(lineIndex: Int, sectionHeaderLine: Int, date: LocalDate, completed: Boolean, body: String, tags: List<String>, form: TagForm)`; tags are effective full tokens including #; `TagForm = NONE | EXPLICIT | INHERITED`.
- `TodoParser.parse(source: String): ParsedDocument`; `TagSyntax.isValidToken(token: String): Boolean`.

- [x] **1. Bootstrap pinned tools and test infrastructure without adding app behavior.** Keep SDK and Gradle caches outside the checkout, under `/workspace/.android-sdk` and `/workspace/.gradle`. Reuse Java 21 but obtain a full JDK if the compiler is absent. Obtain command-line tools 19.0 (`commandlinetools-linux-13114758_latest.zip`) from Google's official repository and verify its published checksum from repository metadata; planning observed SHA-1 `5fdcc763663eefb86a5b8879697aa6088b041e70`. Install `platforms;android-36`, `build-tools;36.0.0`, and `platform-tools` with sdkmanager, accepting the SDK licenses noninteractively while preserving command exit status. Verify the Gradle distribution SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`, and set `distributionSha256Sum` in the wrapper. Preserve TLS and artifact verification. Set Gradle memory to 2 GiB and at most two workers, adjusting only if actual resources require it. Configure JUnit and Robolectric with Android resources; use the official Maven Central repository for Robolectric runtime artifacts.
- [x] **1a. Before any Gradle build, prepare local activation and stable signing.** Create `/workspace/.muh-todo/env.sh` setting JAVA_HOME, ANDROID_HOME, GRADLE_USER_HOME, and the tool PATH. Generate/reuse a private debug keystore at `/workspace/.muh-todo/debug.keystore`, configure debug signing to that retained path, and never replace an existing key. Keep both files outside Git.
- [x] **2. Add failing parser tests.** Anchor assertion:

```kotlin
@Test fun inheritanceSkipsUnrelatedLines() {
    val doc = TodoParser.parse("### 02.10.26\n- [ ] (#work/project #123 #work/project) first\ncomment\n- [x] ^^^ second\n")
    assertEquals(2, doc.tasks.size)
    assertEquals(listOf("#work/project", "#123", "#work/project"), doc.tasks[1].tags)
    assertTrue(doc.tasks[1].completed)
}
```

Also add named cases for invalid calendar dates, exact header width/trailing whitespace, uppercase/indented checkboxes, malformed reserved prefixes, inheritance before any todo, chain boundaries, untagged predecessors, arbitrary body text, duplicate date headings, numeric-leading and supplementary-letter tags, tuple spacing/order/duplicates, and BOM plus mixed line endings. Assert malformed lines stay in `source` and do not become tasks. A plain parenthesized body such as `(remember this)` remains a todo; malformed `(#broken tag)` does not.
- [x] **3. Run** `./gradlew :app:testDebugUnitTest --tests '*TodoParserTest'`. Confirm failure comes from missing parser behavior, not an unresolved SDK or dependency.
- [x] **4. Implement the three document files.** Scan physical lines while retaining exact offsets/endings; parse the first line after a BOM without deleting it from source. Validate dates strictly. Reset inheritance only on recognized headers; ignore malformed/unrelated lines without changing the predecessor. Collect known explicit tags in first-appearance order with unique suggestions.
- [x] **5. Rerun the targeted command.** Require executed passing tests, then run `./gradlew :app:assembleDebug` to verify the pinned Android toolchain; this is not yet a user-testable app.
- [x] **6. Commit only this task's files:** `feat: add strict Markdown todo parser`.

### Task 2: Minimal mutations and inheritance semantics

**Files:** Create `document/TodoMutation.kt`; add `TaskFields(date: LocalDate, completed: Boolean, tags: List<String>, body: String)` to `document/TodoModels.kt`; tests `document/TodoMutationTest.kt`.

**Interfaces:** Consume Task 1 models. Produce `TodoMutation.toggle(document: ParsedDocument, task: Todo): String`, `edit(document: ParsedDocument, task: Todo, fields: TaskFields): String`, and `insert(document: ParsedDocument, fields: TaskFields, inheritTags: Boolean): String`. Reject invalid draft bodies/tags/dates and unavailable inheritance; return the full updated source string without Android dependencies.

- [x] **1. Add failing mutation tests.** Anchor assertion:

```kotlin
@Test fun movingMaterializesOnlyFirstSourceSuccessor() {
    val source = "### 02.10.26\n- [ ] (#a) first\ncomment\n- [ ] ^^^ second\n- [ ] ^^^ third\n"
    val doc = TodoParser.parse(source)
    val changed = TodoMutation.edit(doc, doc.tasks[0], TaskFields(LocalDate.of(2026, 10, 3), false, listOf("#a"), "first"))
    assertTrue(changed.contains("comment\n- [ ] (#a) second\n- [ ] ^^^ third\n"))
    assertEquals(listOf("#a"), TodoParser.parse(changed).tasks.single { it.body == "first" }.tags)
}
```

Add cases asserting checkbox-only character changes; unchanged metadata on body edits; tag-edit propagation; empty-tuple materialization; insertion after the last recognized task across unrelated lines; header-only and repeated sections; absent sections appended without reordering; explicit insertion despite matching predecessor; enabled inheritance including empty tags; inherited movement into matching/mismatching destination contexts; source chains across comments; simultaneous date/tag edits using old tags for the source successor; and byte-exact preservation of unrelated text, BOM, and CRLF. Reject newlines, blank editor bodies, dates outside 2000–2099, and invalid tag tokens.
Add `ambiguousPlainBodyIsRejected`: inserting an untagged literal body `(#work) literal` with inheritance off must reject rather than silently changing its tags/body. The same body must round-trip when preceded by valid explicit or inherited metadata. Add `unrepresentableEmptyTagSuccessorRejectsMove`: when materializing an empty inherited tuple would reinterpret the successor's literal body as metadata, reject the whole move before writing. Validate every emitted task line against its intended fields rather than inventing escapes or enabling inheritance automatically.

- [x] **2. Run** `./gradlew :app:testDebugUnitTest --tests '*TodoMutationTest'` and confirm the intended failure.
- [x] **3. Implement the three mutation methods.** Use source spans/retained line endings, not task-only serialization. Resolve moves and successor materialization from the original parse before calculating destination insertion. Keep empty sections. Explicit moved lines remain explicit; inherited moved lines materialize only when their intended effective tags cannot be supplied at the destination. Never normalize existing metadata during completion/body edits.
- [x] **4. Rerun parser and mutation tests:** `./gradlew :app:testDebugUnitTest --tests '*TodoParserTest' --tests '*TodoMutationTest'`. Require positive executed test counts and no failures.
- [x] **5. Commit:** `feat: add document mutations and controlled inheritance`.

### Task 3: SAF repository and persistent metadata

**Files:** Create `document/DocumentRepository.kt`, `document/ContentResolverDocumentStore.kt`, `document/DocumentPreferences.kt`, `document/TaskRef.kt`; tests `document/DocumentRepositoryTest.kt`, `document/ContentResolverDocumentStoreTest.kt`.

**Interfaces:**
- `TaskRef(lineIndex: Int, expectedLine: String, date: LocalDate)`; construct from a parsed task and its original physical line.
- `DocumentSnapshot(uri: Uri, document: ParsedDocument)`.
- `DocumentStore.read(uri: Uri): String` and `write(uri: Uri, source: String): Unit`, implemented by ContentResolverDocumentStore.
- `WidgetOptions(incompleteFirst: Boolean = false, sortTags: Boolean = false)`.
- DocumentPreferences exposes `documentUri(): Uri?`, `setDocumentUri(uri: Uri)`, `widgetOptions(id: Int): WidgetOptions`, `setWidgetOptions(id: Int, options: WidgetOptions)`, and `removeWidget(id: Int)` using SharedPreferences.
- DocumentRepository exposes `select(uri: Uri, grantFlags: Int): Unit`; suspend `read(): DocumentSnapshot`, `create(expectedUri: Uri, fields: TaskFields, inheritTags: Boolean): Unit`, `edit(expectedUri: Uri, ref: TaskRef, fields: TaskFields): Unit`, and `toggle(expectedUri: Uri, ref: TaskRef): Unit`.
- `TaskRef.from(document: ParsedDocument, task: Todo): TaskRef` captures the original line. `DocumentChangedException` identifies a changed URI/task; `DocumentAccessException` represents I/O/permission/encoding failure without exposing document contents.

- [x] **1. Add failing repository/store tests.** Use a fake DocumentStore for mutations and a Robolectric ContentProvider for stream modes and permission persistence. Assert `edit` rejects a changed original line and performs zero writes; rejects a changed selected URI even if another document has the same line; rereads before writes and preserves external changes to unrelated lines; serializes concurrent app writes; and propagates read, truncate/write, and permission failures. Assert rebuilding DocumentPreferences reloads the URI and distinct widget settings. Strict UTF-8 decoding must reject invalid bytes and preserve a BOM; output must use a supported truncating stream, not append or delete/recreate.
Use a fixture with selected URI `content://test/todos`, one parsed task, and a fake store recording `writes: MutableList<String>`. Anchor:

```kotlin
@Test fun changedDocumentDoesNotWrite() = runBlocking {
    val snapshot = repository.read()
    val ref = TaskRef.from(snapshot.document, snapshot.document.tasks.first())
    preferences.setDocumentUri(Uri.parse("content://test/other"))
    val result = runCatching { repository.toggle(snapshot.uri, ref) }
    assertTrue(result.exceptionOrNull() is DocumentChangedException)
    assertEquals(0, store.writes.size)
}
```

- [x] **2. Run** `./gradlew :app:testDebugUnitTest --tests '*DocumentRepositoryTest' --tests '*ContentResolverDocumentStoreTest'`; confirm intentional failures.
- [x] **3. Implement the repository.** Perform stream I/O on Dispatchers.IO and protect reread/mutate/write with a Mutex. Use the URI grant flags actually returned by the picker, masked to read/write. Verify TaskRef still identifies a recognized task at that line and date. Use short typed errors for no document, document changed, unsupported text, and I/O/permission failure; no merge or recovery engine. Never copy tasks into preferences.
- [x] **4. Rerun targeted tests.** Execute provider-backed tests on Robolectric API 26 and 35; distinguish these checks from actual GrapheneOS provider behavior.
- [x] **5. Commit:** `feat: persist document access and write through SAF`.

### Task 4: Document controls and compact editor

**Files:** Create `MainActivity.kt`, `editor/EditorActivity.kt`, `editor/EditorScreen.kt`, `editor/EditorState.kt`, `editor/EditorIntents.kt`; update the manifest and string resources; tests `editor/EditorStateTest.kt`, `editor/EditorIntentsTest.kt`.

**Interfaces:** Consume DocumentRepository, DocumentSnapshot, TaskFields, and TaskRef. `EditorIntents.create(context: Context): Intent` and `edit(context: Context, uri: Uri, ref: TaskRef): Intent` launch EditorActivity; decoding validates complete extras instead of relying on partial data. `EditorState` holds mode, expected URI, optional TaskRef, chosen fields, inheritance availability/effective tags, loading/saving state, and a short error; it does not own permanent task storage.

Expose `fields: TaskFields`, `inheritTags: Boolean`, `canInherit: Boolean`, and `inheritedTags: List<String>`. Factories `EditorState.create(snapshot: DocumentSnapshot, today: LocalDate): EditorState` and `edit(snapshot: DocumentSnapshot, ref: TaskRef): EditorState` initialize it. `withDate(date: LocalDate): EditorState` recomputes creation inheritance.

- [x] **1. Add failing editor tests.** Assert creation defaults to local today and unchecked, inheritance is off, changing date recalculates the last matching section's predecessor, and unavailable inheritance cannot produce an orphan marker. Assert body-only edits preserve inherited tags, manual tag edits become explicit, ordered/duplicate tags round-trip, invalid drafts cannot save, missing/stale intent extras produce a safe error, and a failed save retains entered fields.
Anchor:

```kotlin
@Test fun creationDefaultsToTodayWithoutInheritance() {
    val snapshot = DocumentSnapshot(Uri.parse("content://test/todos"), TodoParser.parse(""))
    val state = EditorState.create(snapshot, LocalDate.of(2026, 10, 6))
    assertEquals(LocalDate.of(2026, 10, 6), state.fields.date)
    assertFalse(state.fields.completed)
    assertFalse(state.inheritTags)
    assertFalse(state.canInherit)
}
```

- [x] **2. Run** `./gradlew :app:testDebugUnitTest --tests '*EditorStateTest' --tests '*EditorIntentsTest'` and inspect intentional failures.
- [x] **3. Implement UI and intent wiring.** MainActivity has Select/change document and Open in editor; Task 5 adds Refresh widgets once that backend exists. Use ACTION_OPEN_DOCUMENT, persisted read/write grants, and temporary grants for ACTION_EDIT. The editor is a normal compact Activity with date, completion, ordered space-separated tags plus appendable known-tag suggestions, one-line body, Save, and Cancel. Disable tag editing while creation inheritance is on. Restrict selectable dates to the format's 2000–2099 range. Read on open; call the repository on save and close only after success. Do not add a second in-app task list or section-management UI.
- [x] **4. Rerun tests and build:** `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Inspect manifest exports: launcher public, editor internal; no storage/all-files permissions.
- [x] **5. Commit:** `feat: add document selection and compact task editor`.

### Task 5: Functional widget and first hands-on APK

**Files:** Create `widget/WidgetProjection.kt`, `widget/WidgetProvider.kt`, `widget/TodoRemoteViewsService.kt`, `widget/WidgetActions.kt`, `widget/WidgetConfigurationActivity.kt`; layouts `app/src/main/res/layout/widget.xml`, `widget_date.xml`, `widget_task.xml`; provider metadata `app/src/main/res/xml/todo_widget_info.xml`; update manifest/resources, `MainActivity.kt` refresh/document-change wiring, and editor success refresh; tests `widget/WidgetProjectionTest.kt`, `widget/WidgetActionsTest.kt`.

**Interfaces:** `WidgetProjection.project(document: ParsedDocument, options: WidgetOptions): List<WidgetRow>`, with `WidgetRow.DateHeader(date: LocalDate)` and `WidgetRow.Task(ref: TaskRef, completed: Boolean, body: String, tagLabel: String?)`. This increment exposes default options only. `WidgetActions.refreshAll(context: Context): Unit`; action intents include the widget ID, expected document URI, TaskRef, and an explicit toggle/edit/refresh action.

- [x] **1. Add failing widget tests.** Assert newest-first date headers, combined repeated-date tasks in original order, and current-source TaskRefs. Assert a checkbox action calls repository toggle without opening the editor, body action constructs the edit intent, + opens creation, malformed actions cause zero writes, different widget instances have distinct action identities, and a stale action surfaces a safe failure. Test empty documents and permission-error states without substituting fake sample tasks.
Anchor:

```kotlin
@Test fun repeatedDatesCombineWithoutReorderingTheirTasks() {
    val doc = TodoParser.parse("### 01.10.26\n- [ ] old1\n### 02.10.26\n- [ ] new\n### 01.10.26\n- [ ] old2\n")
    val rows = WidgetProjection.project(doc, WidgetOptions()).filterIsInstance<WidgetRow.Task>()
    assertEquals(listOf("new", "old1", "old2"), rows.map { it.body })
}
```

- [x] **2. Run** `./gradlew :app:testDebugUnitTest --tests '*WidgetProjectionTest' --tests '*WidgetActionsTest'` and confirm intentional failures.
- [x] **3. Implement RemoteViews collection rendering and actions.** Use a RemoteViewsService-backed ListView for API 26 compatibility. Scope collection PendingIntent templates and fill-in intents to explicit app components; use mutability only where collection fill-in requires it. Route toggle to repository mutation without displaying the editor, and route row edits to the compact Activity. Keep asynchronous broadcast work within goAsync completion and update widgets after successful mutation. Test launcher editor routing on the phone before claiming current Android background-activity behavior works. Expose header + and refresh; show a document-selection prompt when needed. Configuration initially selects a document if missing and adds a widget with default options. Remove only widget-specific preferences on deletion.
- [x] **4. Verify** `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. Check current-run test counts and reports. Reuse the private debug signing key established in Task 1. Verify the APK with SDK apksigner, record its certificate fingerprint and APK SHA-256, and retain the downloadable artifact at `app/build/outputs/apk/debug/app-debug.apk`.
- [x] **5. Commit:** `feat: add interactive Markdown todo widget`.
- [x] **5a. Add durable build delivery before the first APK handoff.** Create a digest-pinned Dockerfile, .dockerignore, and documented build/archive commands. Seed a private build image with the verified toolchain and resolved dependencies, verify the actual project can build/test with Docker networking disabled and Gradle offline, and preserve image/APK checksums. Include a way to export/import that image and retain the private signing key. Keep generated images, caches, archives, and keys out of Git; do not publish images or keys to a registry. A future host need not install the historical SDK/JDK. State that future Android OS compatibility still requires a real-device test.
- [x] **6. Deliver Increment 1 and pause for hands-on feedback.** Provide the APK through the available artifact delivery mechanism and a short checklist: select a disposable file, add widget, create explicit/inherited tasks, toggle, edit tags, move dates, launch external editor, externally edit and refresh. Tell the user advanced sorting is the next increment. Verify checkbox/editor routing on GrapheneOS from actual results. If artifact delivery is unavailable, report the exact retrieval limitation rather than presenting an unusable preview or download link.

### Task 6: Sorting settings and final rendering

**Files:** Update `widget/WidgetProjection.kt`, `widget/WidgetConfigurationActivity.kt`, `widget/WidgetActions.kt`, and widget/header/task-row layouts; extend `widget/WidgetProjectionTest.kt`; create `widget/WidgetConfigurationTest.kt`.

**Interfaces:** Complete all WidgetOptions modes in the existing projection method and configuration Activity. Persist each widget's two booleans through DocumentPreferences. No repository writes occur during projection/configuration.

- [ ] **1. Add failing sorting tests.** For same-date input `done/#a`, `open/#b`, `open/#a`, assert as-is/off preserves that order; as-is/on gives `done/#a`, `open/#a`, `open/#b`; incomplete-first/off gives `open/#b`, `open/#a`, `done/#a`; incomplete-first/on gives `open/#a`, `open/#b`, `done/#a`. Assert stable ties, empty tuples first, shorter-prefix tuples first, case-sensitive code-point order including supplementary letters, ordered tuple distinction, and duplicate tuple elements. Assert tag-run labels reset at date/tuple changes, empty tags have no shorthand, and only consecutive identical tuples use `^^^`. Assert two widget IDs retain independent preferences and cancellation does not overwrite them.
Anchor:

```kotlin
@Test fun completionOutranksTags() {
    val doc = TodoParser.parse("### 02.10.26\n- [x] (#a) done\n- [ ] (#b) openB\n- [ ] (#a) openA\n")
    val rows = WidgetProjection.project(doc, WidgetOptions(true, true)).filterIsInstance<WidgetRow.Task>()
    assertEquals(listOf("openA", "openB", "done"), rows.map { it.body })
}
```

- [ ] **2. Run** `./gradlew :app:testDebugUnitTest --tests '*WidgetProjectionTest' --tests '*WidgetConfigurationTest'` and inspect intentional failures.
- [ ] **3. Implement comparators and configuration controls.** Sort dates descending, then completion when enabled, then tuple when enabled, then original physical position. Compare Unicode code points without normalization or locale rules. Expose As-is / Incomplete first and Tag sorting off / on. Complete tuple-first/run shorthand rendering with no file mutation. Allow configuration of an existing instance through its widget settings control as well as initial addition.
- [ ] **4. Verify all tests, build, and lint.** Confirm the signing certificate matches Increment 1; deliver Increment 2 for an in-place APK update. Ask the user to test all modes on two instances and confirm app/document state survived the update before finalizing.
- [ ] **5. Commit:** `feat: add per-widget ordering and tag display`.

### Task 7: Integrated validation and reusable environment

**Files:** Create `README.md` and `docs/testing-on-grapheneos.md`; add `document/TodoWorkflowTest.kt`. Keep local tool activation helpers and setup logs outside the checkout. Save tested environment fields through the onboarding configuration tool.

**Interfaces:** No new product interfaces; consume the complete app and tests. Preserve signing-key identity and stable application ID.

- [ ] **1. Add an integration test for create → inherit → toggle → edit tags → move → project.** Assert the actual output text preserves unrelated lines, the source successor becomes explicit after moving, the moved task retains intended tags, and completion-first sorting never changes the document. Run it before fixes and use any failure to diagnose the owning component rather than weakening assertions.

Anchor:

```kotlin
@Test fun createEditAndMovePreserveDocumentSemantics() {
    val day = LocalDate.of(2026, 10, 2)
    var doc = TodoParser.parse("notes\n### 02.10.26\ntrailing text\n")
    doc = TodoParser.parse(TodoMutation.insert(doc, TaskFields(day, false, listOf("#a"), "first"), false))
    doc = TodoParser.parse(TodoMutation.insert(doc, TaskFields(day, false, emptyList(), "second"), true))
    doc = TodoParser.parse(TodoMutation.toggle(doc, doc.tasks.first()))
    doc = TodoParser.parse(TodoMutation.edit(doc, doc.tasks.first(), TaskFields(day, true, listOf("#b"), "first")))
    assertEquals(listOf("#b"), doc.tasks[1].tags)
    val moved = TodoMutation.edit(doc, doc.tasks.first(), TaskFields(day.plusDays(1), true, listOf("#b"), "first"))
    assertTrue(moved.startsWith("notes\n"))
    assertTrue(moved.contains("- [ ] (#b) second\ntrailing text\n"))
    val rows = WidgetProjection.project(TodoParser.parse(moved), WidgetOptions(true, true)).filterIsInstance<WidgetRow.Task>()
    assertEquals(listOf("first", "second"), rows.map { it.body })
    assertTrue(rows.first().completed)
}
```

- [ ] **2. Run final checks:** `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. Inspect fresh test XML counts, failures, skipped outcomes, lint results, APK signature, and clean tracked state except intended changes. Do not run an emulator suite unless an actual compatible emulator/device is available; state device checks separately.
- [ ] **3. Exercise repeatable setup in the real cloud machine.** Test complete install instructions with explicit working directories, preserved checksum/TLS verification, SDK/JDK activation, and pinned Gradle commands. Reuse retained SDK/dependency caches on the second run. Installation must not rewrite product files or the signing key. Save the tested install_script and necessary start_skill activation/check instructions, including using the existing isolated checkout and not relying on retained processes. Do not create running services merely to populate start_skill.
- [ ] **4. Document and incorporate device results.** Record how to sideload/update the debug APK, select an external Markdown file, configure two widgets, refresh external edits, and reproduce inheritance/movement checks. Cover permission persistence across process restart, revoked-grant errors, actual external-editor compatibility, and actual launcher checkbox/editor actions. Protect the signing key and keep it out of Git. Do not claim a new cloud task or phone behavior was validated unless it was.
- [ ] **5. Commit documentation and integration checks:** `docs: record setup and GrapheneOS validation`.
- [ ] **6. Report exact outcomes:** test counts and failures, APK identity, supported phone behaviors observed, outstanding limitations, saved configuration fields, and any user action needed to review/publish the cloud environment. Publication is separate from configuration persistence.

## Plan self-review

- Parser/metadata/date recognition and source preservation: Tasks 1–2.
- Explicit creation inheritance, live tag edits, movement materialization: Tasks 2 and 4.
- SAF ownership, grants, minimal failure handling, stale selection checks: Tasks 3–4.
- Widget interactions, refresh, date/tag grouping, per-instance modes: Tasks 5–6.
- Automated checks, signed APK updates, hands-on GrapheneOS tests, reusable cloud setup: Tasks 5–7.
- Review Focus cases are assigned tests above; device-dependent results remain explicitly separate.
- Shared interfaces are defined before consumption. No production feature depends on a task database or background watcher.

# muh todo

A small Android app and home-screen widget backed by your own UTF-8 Markdown document. Android 8.0+ (API 26); no accounts, database, Google Play services, or network permission. The document stays where you selected it through Android's document picker.

The app supports creating/editing tasks, direct completion toggles, live tag inheritance, date moves, and manual widget refresh. Each widget has independent **Sort by completion** and **Sort by tags** controls. Each uses three buttons with exactly one selected: **No sort / Incomplete first / Complete first** for completion, and **No sort / a-z and 0-9 / z-a and 9-0** for tags. Completion takes priority over tags; ascending completion puts incomplete tasks first, descending puts completed tasks and plain entries first. Equal sort keys keep file order. Dates appear newest first. Configuration opens when adding a widget. If your launcher offers **Edit widget**, it reopens the same settings; otherwise recreate the widget to change them. Moving/resizing alone does not reopen settings. Sorting changes only the view.

[0.1.1](docs/0.1.1-visual-update.md) applies the first phone feedback: dark theme, slightly smaller text, the `muh todo` name, and the supplied launcher artwork.

[0.2.0 validation and offline rebuild instructions](docs/0.2.0-validation.md) cover sorting settings, optional checkboxes, and rebuilding the new source with the preserved image.

[0.2.1](docs/0.2.1-validation.md) adds independent sort directions, aligned bullets, and a tag format hint.

[0.2.2](docs/0.2.2-validation.md) replaces sorting checkboxes/radio options with two three-way segmented selectors.

[0.3.0](docs/0.3.0-validation.md) adds anchored creation with editable prefilled tags, cursor-aware known-tag insertion, and ascending tag suggestions. [0.4.0](docs/0.4.0-validation.md) removes the toolbar, adds dated creation and launcher reconfiguration, colors tags and suggestions, and adds entry deletion.

## Try it

Install `app/build/outputs/apk/debug/app-debug.apk`, open muh todo, and select a disposable `.md` document with read/write access. Add **muh todo** through your launcher's widget picker. Use a date header's `+` to create on that date, a checkbox to toggle, task text to edit, and the return-arrow icon to create below it with prefilled tags. An empty widget opens the app, where **New todo** creates the first entry. After external file changes, recreate the widget or use **Refresh widgets** in the app. See [the phone checklist](docs/testing-on-grapheneos.md).

```markdown
### 06.10.26
- [ ] (#groceries #life) buy milk
- [ ] ^^^ buy butter
- (#life) something to remember
```

Exact date headings and checkbox syntax matter. Lowercase `x` marks completion. Tags accept Unicode letters/digits and `_/.!?+*<>=:-`; tuples preserve their order and duplicates. Only recognized tasks inside valid date sections are managed. Other text is preserved. Editing an inherited task's tags changes the following inheritance chain. Moving or deleting a task materializes the first source successor's previous tags. **Delete** appears beside Cancel when editing an existing entry; date headings and unrelated text are retained.

Checkboxes are optional: plain `- BODY` entries support the same tags, inheritance, and dates. They display an aligned bullet instead of a widget checkbox and sort with completed tasks when completion sorting is enabled. Tapping the bullet opens the editor. **Use checkbox** defaults on for new entries; switch it off to create a plain entry. Editing or moving a plain entry keeps it plain. Invalid leading checkbox-like tokens such as `[X] ` remain unrelated Markdown.

Tap a row's **return-arrow icon** to create immediately below that entry in the file. Tags are prefilled and editable; unchanged tags serialize as `^^^`, changed tags are explicit, and cleared tags omit metadata. The date stays fixed to the source entry. Following existing `^^^` lines naturally inherit the inserted entry's tags. Date-header **+** creation uses explicit tags and prefills that date, which remains editable. The editor has no Completed or Inherit tags checkboxes; toggle completion directly in the widget. Creation from the app defaults to the phone's current local date; missing sections are created automatically. The editor deliberately accepts one-line bodies. Untagged bodies beginning with reserved metadata (`(#...` or `^^^`) may be unrepresentable and are rejected. Moving/deleting also refuses a write if an inherited successor with empty tags cannot be materialized without reinterpreting its body.

Each exact tag gets a stable display color in widget tag labels and known-tag suggestions. Colors affect only the view; `^^^` remains neutral shorthand. Some tags can share similar colors in large pools.

The file is reread before a change. A stale task or changed document selection requires reopening the editor/refreshing the widget. There is no merge engine, external-edit watcher, or atomic-write promise for arbitrary document providers. Test with a disposable document first.

## Build now

Pinned versions: JDK 21, Gradle 8.13 (wrapper with SHA-256 verification), AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.10.01. Android SDK platform 36 and Build Tools 36.0.0. Dependency versions are in the Gradle files.

Set `JAVA_HOME`, `ANDROID_HOME`, and a writable `GRADLE_USER_HOME`. To retain update compatibility, set `MUH_TODO_DEBUG_KEYSTORE` to the same private debug key on every build. Otherwise Android's ordinary local debug key is used. Do not commit keys.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

In the prepared Codex cloud environment, first run `source /workspace/.muh-todo/env.sh`. No emulator or running service is required for unit tests/builds. Robolectric checks simulated APIs 26 and 35; phone behavior requires the checklist.

## Keep it buildable years later

Preserve the **private build-image archive**, rather than relying on Maven repositories or old Android downloads staying online. The archive includes the pinned Linux userspace/JDK, SDK, Gradle, resolved dependency caches, Robolectric SDKs, this source snapshot, and the signing key. The script verifies a fresh build, tests and lint with Docker networking disabled and Gradle offline before exporting it.

After the native build has resolved all dependencies, with Docker available and the tool paths above set:

```sh
./scripts/archive-build-environment.sh
```

The Dockerfile uses an immutable base digest. Its context is generated by this script; it is not intended for `docker build .` on a bare checkout. Python 3 is needed only to collect today's archive. Store the resulting `artifacts/` directory securely in more than one place. It contains a private signing key inside the image; keep it off public registries and out of Git. The debug key uses standard Android debug credentials and is for personal builds, not production distribution. The archive intentionally excludes Git history and app documents. Preserve the repository separately for history.

On a future Linux host with Docker (no historical JDK or Android SDK installation needed):

```sh
cd your-preserved-artifacts
sha256sum --check SHA256SUMS
docker load --input muh-todo-build-0.1.0.tar.gz
docker run --name muh-todo-rebuild --network=none muh-todo-build:0.1.0 \
  clean :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
docker cp muh-todo-rebuild:/workspace/muh-todo/app/build/outputs/apk/debug/app-debug.apk ./rebuilt.apk
docker rm muh-todo-rebuild
```

This image is Linux **amd64**; an ARM host needs Docker's amd64 emulation. A preserved image avoids future package servers, but still needs a working container runtime/kernel. Byte-for-byte APK reproducibility is not promised. Keeping the key preserves signing identity; keep the same application ID and increase `versionCode` when releasing updates. The key is valid until 2054.

A future Ubuntu host can use that archive to build the historical app. A hypothetical future Xiaomi/Android release may require compatibility changes: an offline build cannot guarantee future OS installation/widget rules. That part must be checked on the actual phone.

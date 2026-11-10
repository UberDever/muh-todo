# muh todo

A small Android app and home-screen widget backed by your own UTF-8 Markdown document. Android 8.0+ (API 26); no accounts, database, Google Play services, or network permission. The document stays where you selected it through Android's document picker.

**[Document format specification](docs/format.md)** is the single authoritative
reference for dates, entry types, tags, priorities, inheritance, optional titles,
mutation rules and view-only interpretation.

The app supports creating/editing tasks, direct completion toggles, live tag inheritance, date moves, and manual widget refresh. Each widget has a **Group order** dialog: drag **Incomplete**, **Bullets** (no checkbox), and **Complete** to set their order; the default is **Incomplete → Bullets → Complete**. Up/down buttons provide an alternative to dragging. Groups come before tag and date sorting; equal keys keep file order. Tags retain **No sort / a-z and 0-9 / z-a and 9-0**, and dates retain **No sort / Newer first / Older first**. Configuration opens when adding a widget. If your launcher offers **Edit widget**, it reopens the same settings; otherwise recreate the widget to change them. Sorting changes only the view.

[0.1.1](docs/0.1.1-visual-update.md) applies the first phone feedback: dark theme, slightly smaller text, the `muh todo` name, and the supplied launcher artwork.

[0.2.0 validation and offline rebuild instructions](docs/0.2.0-validation.md) cover sorting settings, optional checkboxes, and rebuilding the new source with the preserved image.

[0.2.1](docs/0.2.1-validation.md) adds independent sort directions, aligned bullets, and a tag format hint.

[0.2.2](docs/0.2.2-validation.md) replaces sorting checkboxes/radio options with two three-way segmented selectors.

[0.3.0](docs/0.3.0-validation.md) adds anchored creation with editable prefilled tags, cursor-aware known-tag insertion, and ascending tag suggestions. [0.4.0](docs/0.4.0-validation.md) removes the toolbar, adds dated creation and launcher reconfiguration, colors tags and suggestions, and adds entry deletion. [0.5.0](docs/0.5.0-validation.md) adds the 0–99 priority picker and unified ordered-tuple OKLCH colors.

## Download and CI

[GitHub Releases](https://github.com/UberDever/muh-todo/releases) retain each
published version with a directly downloadable `muh-todo.apk`, checksum and
signing certificate. [Latest APK](https://github.com/UberDever/muh-todo/releases/latest/download/muh-todo.apk)
always points to the latest release. No ZIP or local build is needed.

Push a version tag such as `v0.6.0` after setting Android `versionName` to
`0.6.0`. CI verifies the version matches, runs tests/lint, builds with the
retained signing key, and publishes the release automatically. A missing
signing key fails a release. Published assets are preserved on reruns.

[GitHub Actions builds](https://github.com/UberDever/muh-todo/actions/workflows/android.yml)
also run on every push to main, pull request, and manual dispatch. Successful
main builds upload ZIP artifacts for development snapshots; APKs are never
committed to Git. These artifacts require GitHub login and expire after 90 days.

To let CI update the app already installed on your phone, add the existing
private key as repository Actions secret `MUH_TODO_DEBUG_KEYSTORE_BASE64`.
Without it, CI uses a fresh runner debug key: those test APKs cannot update the
previously installed app. The APK built in the prepared environment retains
the existing key.
The cloud environment's default GitHub credential cannot manage repository
secrets. The browser-only alternative is the **Configure signing** workflow:
store a temporary fine-grained token with this repository's **Secrets: read
and write** permission as repository Actions secret `GH_TOKEN`. The cloud
agent requests GitHub's public encryption key through that workflow, encrypts
the retained signing key locally using a libsodium sealed box, and dispatches
the workflow with only the ciphertext and key ID. The workflow stores
`MUH_TODO_DEBUG_KEYSTORE_BASE64` through GitHub's encrypted-secret API.
No plaintext signing key or token is committed or supplied as workflow input.
Once a CI build's certificate matches the installed app, the temporary token
can be revoked; normal APK builds use only the retained signing secret.

## Try it

[Try the 100-entry synthetic widget showcase](examples/README.md) for varied
dates, age stages, priorities, ordered tags, inheritance and all entry types.

Install `app/build/outputs/apk/debug/app-debug.apk`, open muh todo, and select a disposable `.md` document with read/write access. Add **muh todo** through your launcher's widget picker. Use the scrollable `+ New entry` row to create with today's date, a checkbox to toggle, task text to edit, and the smaller return-arrow icon to copy all fields except the body into an editable new entry. An empty widget opens the app, where **New todo** creates the first entry. After external file changes, recreate the widget or use **Refresh widgets** in the app. See [the phone checklist](docs/testing-on-grapheneos.md).

```markdown
### 06.10.26
- [ ] (#groceries #life) buy milk
- [ ] ^^^ buy butter
- (#life) something to remember
```

See the [format specification](docs/format.md) for recognition rules, optional
`Title. Body` titles, priorities, inheritance, copying/moving/deleting entries,
and all widget grouping, sorting, color and age behavior. The editor accepts
one-line bodies and exposes a separate optional title field in 0.9.0.

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

## Current behavior

The [format specification](docs/format.md) also documents all view-only
interpretation, including group ordering, the checkbox-only counter, urgency
stages, completed-entry dimming, and plain bullets without age timers. Widget
refreshes are requested every 30 minutes, subject to Android's scheduling.

The postponed [tile-mode proposal](docs/design/tile-mode-proposal.md) is retained
separately. Optional titles are implemented in 0.9.0; tile mode remains postponed.

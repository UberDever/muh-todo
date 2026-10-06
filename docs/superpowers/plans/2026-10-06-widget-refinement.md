# Widget refinement implementation plan

> **For agentic workers:** Use superpowers:executing-plans for inline execution. The user requested ongoing agile iterations; proceed within that authorization.

**Goal:** Remove the widget toolbar, use launcher configuration, color tags in both views, and delete existing entries from the editor.

**Architecture:** Keep existing RemoteViews collection routing and the document repository. Date headers send a dated creation intent. Tag colors are computed from tag text; deletion is a minimal source mutation through the existing guarded write path.

**Tech stack:** Existing Kotlin, Compose, SAF and Android RemoteViews; no new dependencies.

**Spec:** Original markdown-todo design plus the user's current widget refinement request.

## Constraints and review focus

- Keep min SDK 26, existing signing key and document ownership.
- Declare reconfigurable; launcher controls availability. Initial setup remains required. Moving alone does not configure.
- A date-header + opens normal creation with that date; empty widgets still open the app to create a first entry.
- Error rows must not show a creation action. Recycled tag spans must not leak to subsequent rows.
- Colors are stable per exact tag, readable on dark backgrounds, view-only. Repeated ^^^ remains neutral shorthand. Unlimited tags cannot have unlimited perceptually unique colors.
- Deletion removes only the selected line, materializes the first inherited successor in the same section, and retains unrelated Markdown/headings/line endings.
- Stale URI/line/date guards and save/delete single-operation ownership remain effective. Delete is available only while editing an existing entry, immediately left of Cancel, red.

## Task 1: Launcher configuration and dated creation

Files: widget provider metadata/layouts, WidgetProvider, TodoRemoteViewsService, WidgetActionActivity, WidgetActions, EditorIntents, EditorViewModel; corresponding intent/configuration/rendering tests.

- [x] Add failing tests for dated creation routing and editable date initialization, reconfiguration metadata, absence of toolbar, and error-row action hiding.
- [x] Run targeted tests and verify missing behavior.
- [x] Add CreateOnDate(date), optional create.date payload parsing with 2000–2099 bounds; route header fill-in through the existing collection activity. Add reconfigurable metadata and replace the toolbar with date-header +.
- [x] Run tests. Header and editor changes are committed together as one phone iteration.

## Task 2: Tag colors and deletion

Files: new ui/TagColors.kt and widget/TagLabels.kt; editor screen/state/viewmodel/activity; document mutation/repository; tests.

- [x] Add failing tests for full Unicode tag stability/readability/common-tag separation and widget span recycling.
- [x] Implement exact-tag SHA-256 derived hue and readable HSL lightness, shared colored text/borders in suggestions and per-token ForegroundColorSpan widget labels; keep shorthand neutral.
- [x] Add failing tests for deletion: inherited chains across unrelated lines, empty tags, repeated date sections, CRLF/BOM/EOF, stale references and duplicate clicks/rotation/write failure.
- [x] Implement TodoMutation.delete and repository.delete; reuse successor materialization with move. EditorState.delete and EditorViewModel.delete use existing async owner, busy guards, errors and refresh-on-success. Add red Delete beside Cancel only for existing tasks.
- [x] Run targeted and full tests; commit.

## Task 3: Delivery

- [x] Review complete diff with a fresh read-only reviewer and address substantive findings.
- [x] Run full tests/build/lint natively and fresh offline Docker source overlay; inspect actual counts and signing identity.
- [x] Deliver 0.4.0/code 7 (version bumped), document phone checks and actual validation, export APK/source backups/checksums, push main without force, verify remote identity.

Self-review: all current requests have a task; source preservation and lifecycle failure modes have meaningful regression tests. Actual launcher UI remains a phone check.

## Result

160 tests pass natively and in a fresh network-disabled offline Docker build,
zero failures/errors/skips. Lint: zero errors, 20 warnings native and 18 offline.
Independent reviewer confirmed the initial color-separation finding resolved;
no actionable findings remain. APK signature verified with the existing key.
Phone/launcher checks remain separate. Implementation commit 149a0b5 pushed
to main; final validation/source-export documentation follows.

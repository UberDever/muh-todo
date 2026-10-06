# Android Markdown Todo Widget

Date: 2026-10-06
Status: approved by the user on 2026-10-06; implementation underway with hands-on feedback.

## Purpose and scope

Build a small Android application and home-screen widget over a user-owned
Markdown todo document. The document is the source of truth. The app adds,
edits, and toggles tasks through Android Storage Access Framework (SAF).

There is no task database, document copy in app-private storage, account,
sync engine, project model, reminder system, or background file watcher.
Persistent app metadata consists of the selected document URI and per-widget
display settings. Sorting affects the view only.

The primary hands-on test device is a Google Pixel 10 Pro running GrapheneOS.
No Google Play services are required. Use native RemoteViews for the widget,
Jetpack Compose for Activities, and pure Kotlin for document logic. Start with
one Android app module, with minimum Android API 26. Pin supported stable
build-tool versions in the implementation plan.

## Document selection and access

Use ACTION_OPEN_DOCUMENT to select a Markdown document, request read/write
access, and persist the URI grant. Read and mutate that same document in
place. Changing the document affects all widget instances.

Support UTF-8 text, preserve an existing UTF-8 BOM, and preserve existing
line endings and unrelated text. Reject unreadable or unsupported input
with a short error instead of silently converting it.

The main Activity provides document selection/change and refreshing widgets.
The user opens the backing document independently in their own text editor.
User amendment on 2026-10-06: remove the app's Open in text editor action.

## Recognition grammar

Use a line-oriented parser, not a general Markdown parser.

A date header is an exact, unindented line `### DD.MM.YY`, with ASCII digits,
zero padding, no trailing whitespace, and a valid calendar date. The year
is 2000 + YY. A recognized section extends until the next recognized date
header. Invalid headings are unrelated text and do not end the current
section. Tasks outside a valid section are not recognized.

A recognized entry starts exactly with `- `, optionally followed by `[ ] `
or `[x] `. Only lowercase x is valid. A bracketed token followed by whitespace
or end of line is reserved for checkbox syntax; invalid forms such as `[X] `
and `[maybe] ` remain unrelated Markdown. Markdown links such as `[docs](url)`
are valid plain bodies. Its payload is one of:

```text
BODY
(#tag #other-tag) BODY
^^^ BODY
```

BODY is a nonempty remainder of one physical line. It may contain Markdown,
parentheses, hashes, and inheritance markers. Metadata is recognized only
at the beginning of the payload. Leading `(#` and `^^^` are reserved
metadata prefixes: malformed expressions at those positions make the
whole line unrelated Markdown. Other parentheses in a plain body remain
ordinary text. Metadata requires one ASCII space before BODY; any further
spaces belong to BODY.

A tag identifier is a nonempty sequence of Unicode letters, Unicode decimal
digits, or `_ - / . ! ? + * < > = :`. All allowed characters may occur first.
The full tag token regex is:

```regex
#[\p{L}\p{Nd}_/.!?+*<>=:-]+
```

A tuple contains one or more tags separated by exactly one ASCII space,
inside parentheses. Quoting and escaping are unsupported. Preserve tag
spelling, case, tuple order, and duplicates exactly. There is no Scheme
reader or identifier normalization.

`^^^` inherits the effective tag tuple of the previous recognized todo in
the same date section, including an empty tuple. Inheritance may chain.
Unrelated lines do not interrupt it. Inheritance cannot cross a recognized
date header. Without a preceding recognized todo, the line is unrelated
Markdown and does not become an inheritance predecessor.

The known tag pool is gathered from explicit parsed tuples. It supplies
editor suggestions; it does not require separate storage. Deduplicating
suggestions must not deduplicate a task's chosen tuple.

User amendment on 2026-10-06: checkbox-free entries share dates, tags,
inheritance, editing, and movement with tasks. They have no completion
control and sort with completed tasks when completion sorting is enabled. Their lack
of a checkbox is represented by the file itself, never separate metadata.

## Document mutations

Keep source text and physical source locations so operations can make
small textual changes. Do not reserialize the whole document from parsed
tasks or reorder unrelated content.

### Completion and ordinary edits

Completion changes only the checkbox character between space and x.
Only entries with a checkbox can be toggled. Editing body or completion keeps
the existing tag representation. Preserve checkbox presence during ordinary
edits, moves, and successor materialization; adding/removing it requires an
explicit editor change.

When effective tags are changed, write the chosen tuple explicitly, or
omit tag metadata for an empty tuple. Leave following inheritance markers
unchanged: all tasks in that inheritance chain acquire the new effective
tags. A task with explicit tags or no tags ends the chain. Reparse after
the mutation instead of storing derived task state.

### Creation and date sections

Default the date to the phone's current local date when opening creation.
The user may choose a different date. Saving finds that date's section or
creates it on demand; there is no separate section-management interface.
For repeated headers with the same date, use the last matching physical
section for insertion.

Insert immediately after the section's last recognized todo, or immediately
after its header if it contains none. Preserve unrelated lines in their
existing order. Append a missing date section at the document's end, adding
only the separators needed to make the header and task separate lines.
Use the surrounding line-ending style, or LF for an empty document. Keep
existing section ordering; display ordering is independent.

Creation has an Inherit tags toggle, off by default. When enabled, display
the preceding task's effective tags as read-only and serialize `^^^`.
An empty effective tuple may also be inherited. Disable the toggle when
there is no preceding recognized task in the chosen insertion section.
Changing the date recomputes inheritance availability and the shown tuple.
When disabled, write selected tags explicitly even if they match the
preceding task, or omit metadata for an empty tuple. Never infer inheritance
automatically for a new task.

### Moving to another date

Move only the selected task, not its following inheritance chain. If its
next recognized task in the source section uses `^^^`, materialize that
successor's effective tuple as it was before removal. For an empty tuple,
remove the marker rather than writing an empty parenthesized expression.
Leave later markers intact. Unrelated intervening lines remain in place.

At the destination, use the same section-selection and insertion locations
as creation. Preserve the moved task's effective tags unless the user also
edited them. An existing inherited representation may remain `^^^` if the
destination predecessor supplies the intended tuple; otherwise materialize
it. Explicit task tags remain explicit. Do not remove empty source sections.

## Lightweight editor

Use a compact, dialog-like normal Activity, launched by task taps or +.
It contains date, Use checkbox, completed state when a checkbox is enabled,
ordered tags, body, Save, and Cancel. Use checkbox defaults on for creation;
editing loads the entry's existing checkbox presence. Turning it off removes
the completion state and writes a plain entry.
Creation additionally exposes Inherit tags.

Keep tag input simple: an ordered space-separated tag field with suggestions
from the document, allowing new valid tags. Adding a suggested tag appends
it; do not silently sort or discard duplicates. For an existing inherited
task, show its effective tags, and retain `^^^` when those tags are unchanged
and it stays in the same date section. Date moves follow the materialization
rules above.

The editor requires a nonblank single-line body and valid tags. Parsing
existing documents remains governed by the recognition grammar, rather
than stricter editor validation. Reject a draft if its selected metadata
representation would cause its literal body to be parsed as metadata;
do not silently change the body/tags or invent an escape syntax.
Saving writes the document, refreshes all
widgets, and closes the editor. Cancel makes no change.

## Simple file handling and errors

Read on opening and reread before a mutation. For an existing selected task,
verify that its original line still matches at the stored source position
and is recognized in the expected date section. If it does not, stop with
a short Document changed; reopen task message. Do not search for replacement
tasks, offer merge UI, or introduce permanent IDs into the document.

Serialize writes initiated by this app. This is a simple best-effort check,
not protection against every concurrent external edit or an atomic-write
guarantee across SAF providers. Write through a supported truncating
document stream; never delete/recreate the document as a replacement.

Read/write or permission failures show a short error. Keep unsaved editor
contents available after a save failure. A missing/unreadable document
puts widgets into a prompt to open the app and select or regrant access.
Do not automatically retry a failed write. Show no success until it finishes.

## Widget projection and interactions

Group by date, newest first. Combine tasks from repeated sections sharing a
date for display, retaining physical file order as their stable tie-breaker.
Each widget independently stores two sorting controls:

1. **Sort by completion**: No sort (default), Incomplete first, or Complete first.
2. **Sort by tags**: No sort (default), a-z and 0-9, or z-a and 9-0.

Each control is a row of three buttons with exactly one selected. The active
segment is highlighted; labels wrap on narrow screens. Completion choices
map to off/ascending/descending; tag choices map to the existing case-sensitive
Unicode tuple comparator, without changing the recognized tag grammar.

Within a displayed date, compare completion when enabled, then effective tag
tuples when enabled, then original file order. Each comparator follows its
own direction. Completion always takes priority over tags. Sorting neither
changes date order nor reverses the original-order tie-breaker.

Ascending completion means unchecked first. Descending means checked first.
Checkbox-free entries belong to the checked group for sorting only. This does
not add a checkbox or completion marker to them.

Compare tag identifiers case-sensitively by Unicode code point, and tuples
element by element. In ascending order, a shorter equal-prefix tuple sorts
first and empty tuples sort first. Descending reverses these comparisons.
No locale collation or normalization. Existing enabled sorting preferences
upgrade to ascending; a disabled control retains its chosen direction.

Render identical effective tuples as consecutive runs within each date.
Show the first nonempty tuple explicitly, then use `^^^` shorthand for
later tasks in that run. Do not use shorthand for empty tuples. This is a
display choice and does not reflect whether a file line is explicit or
inherited. When tag sorting is off, do not collect nonconsecutive matching
tuples together, because doing so would violate the chosen ordering.

Entries without checkboxes display a bullet in the same column as checkbox
controls, preserving text alignment. Tapping a bullet opens the editor.
Checkbox taps mutate completion directly. Body taps open the editor. +
opens creation. A small refresh button rereads the document and refreshes
widgets after external edits. App mutations refresh every widget instance.
There is no periodic background refresh or file watcher.

Widget configuration chooses the two display settings and provides a path
to document selection if none is configured. Deleting a widget removes
only its preferences, not tasks or the selected document.

## Component boundaries

- DocumentRepository: URI metadata, SAF permissions, reads, serialized
  writes, and coordination of pure mutation results.
- TodoParser: sections, recognized tasks, raw representations, effective
  tags, known tags, and physical source locations.
- TodoMutation: completion, body/tag edits, insertion, moves, and source
  successor materialization; returns changed text without Android APIs.
- WidgetProjection: date grouping, stable sorting, and consecutive tag runs.
- Android layer: main/editor/configuration Activities, widget provider,
  collection rendering, and task-action dispatch.

No dependency injection framework or extra architectural layers are needed.

## Validation and delivery

Pure Kotlin tests cover strict dates and checkboxes; tag grammar, order, and
duplicates; arbitrary bodies; malformed metadata; chained inheritance;
unrelated intervening lines; section boundaries; empty tuples; and tag pools.

Mutation tests cover checkbox-only changes, unrelated-text preservation,
line endings and BOM, live tag propagation, insertion with the inheritance
toggle on/off, repeated/missing sections, movement with successor
materialization, and preservation of the moved task's effective tags.

Projection tests cover all four ordering modes, descending dates, repeated
date sections, case-sensitive tuple ordering, stable ties, and tag runs.
Repository/Android checks cover grants, unsupported input, stale selections,
provider failures, and widget action routing without treating mocks as a
substitute for device testing.

Build the APK and run the relevant unit tests and Android lint checks in
the cloud. Provide a signed debug APK for hands-on testing. Keep its signing
key private, outside the repository, and stable across test builds so APK
updates preserve app state. No Play Store distribution is needed.

On the Pixel with GrapheneOS, use a disposable Markdown file and verify:
selection and persisted permission; adding tasks with inheritance on/off;
direct checkbox toggling; edits that propagate tags; date moves that
materialize the source successor; external-editor access and manual refresh;
two widgets with distinct settings; and an APK update preserving metadata.
Actual launcher and document-provider behavior remains unverified until
these hands-on checks run.

Prepare Android dependencies after the required design/plan reviews. Record
tested setup in install_script and service/tool initialization in start_skill
only where needed. Future cloud tasks use the existing isolated checkout,
not a new Git worktree. Saved configuration does not publish an environment
or establish that a new task has been validated.

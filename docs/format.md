# muh todo document format

This is the authoritative specification of the Markdown document format,
including optional titles introduced in **0.9.0**. It replaces the format
descriptions scattered through older design documents. It is intended to be
usable without reading those documents or the source code.

## Document and ownership

The source of truth is a user-selected UTF-8 `.md` document. The app reads and
modifies that document in place through Android's document picker/Storage Access
Framework. It does not copy tasks into a database or add sync metadata.

The parser is line-oriented. The document may contain arbitrary Markdown;
only the date headings and entry lines below are recognized. All other text
is preserved when the app modifies an entry. Existing UTF-8 BOMs and line
endings are retained. Recognized-looking text inside a fenced code block is
still recognized: the parser does not interpret Markdown blocks. Indentation
and quote prefixes, however, prevent an entry from matching its exact prefix.

## Complete example

```markdown
# My notes

Unrelated Markdown stays in the file.

### 10.11.26
- [ ] (#50 #home #groceries) Groceries. Buy milk and bread
- [ ] ^^^ Pantry. Check whether there is enough rice

<!-- Comments do not interrupt tag inheritance. -->

- [x] ^^^ Cupboard. Put the jars away
- (#home #ideas) Shelf. A narrow shelf might fit beside the window
- [ ] A task without a title or tags
- A plain note without a title or tags

### 09.11.26
- [ ] (#work #35 #project) Draft. Write the first paragraph
```

## Date sections

A section begins only with an exact, unindented line:

```text
### DD.MM.YY
```

- Digits must be ASCII, zero-padded, with no trailing whitespace.
- The date must exist in the calendar. `YY` means `2000 + YY`, so the supported
  years are 2000–2099. `29.02.24` is valid; `29.02.23` is not.
- The section ends at the next recognized date heading. Invalid headings,
  other Markdown headings, blank lines and comments do not end it.
- Entries before the first recognized date heading are ignored.
- Repeated dates create separate physical sections. Their inheritance chains
  are independent, even when the dates match.
- File order is unrestricted; headings need not be chronological.

An entry's date comes from its containing section. There is no additional
creation timestamp or per-entry date field in the line.

## Entry prefixes and completion

An entry starts at column zero with one of these exact prefixes:

| Prefix | Meaning |
| --- | --- |
| `- [ ] ` | Incomplete checkbox entry |
| `- [x] ` | Completed checkbox entry |
| `- ` | Plain bullet, with no completion state |

Spaces in these prefixes are literal ASCII spaces. Only lowercase `x` is
recognized. Indented entries and `- [X] `, `- [*] ` or `- [maybe] ` are ignored.
A leading bracketed token followed by whitespace or end of line is reserved
for checkbox syntax, so `- [note] text` is also ignored. A Markdown link such
as `- [docs](https://example.com)` is a valid plain entry.

A recognized entry needs a nonempty payload on the same physical line.
Continuation lines are unrelated text. For compatibility the reader accepts
whitespace-only bodies; creating or saving an entry requires a nonblank body.
New entries use a checkbox by default; plain bullets are explicitly selected
in the editor. Plain bullets cannot be toggled complete.

## Payload and metadata order

After the entry prefix, the payload is one of:

```text
TEXT
(TAGS) TEXT
^^^ TEXT
```

`TEXT` in turn is either `BODY` or `TITLE. BODY`. Parse in this order:

1. Read the optional leading tag tuple or inheritance marker.
2. Read the optional title at the beginning of the remaining text.
3. Treat the rest as body text.

Both metadata forms require exactly one ASCII separator space before `TEXT`;
any additional spaces belong to `TEXT`. Leading `(#` and `^^^` are reserved:
malformed expressions there make the entire line unrelated Markdown. A valid
`^^^` additionally requires a recognized predecessor in the same section.
Other leading parentheses, such as `(remember this)`, are ordinary text.

Tags and inheritance markers appearing later in the body are literal text,
not metadata. For example, `- [ ] Read about #work and ^^^` has no tags.

## Tags

Tags are written as an ordered tuple, for example:

```text
(#general #life #groceries)
```

The grammar is:

```text
TAGS := TAG (" " TAG)*
TAG  := "#" IDENTIFIER
```

An identifier is a nonempty sequence of Unicode letters, Unicode decimal
digits, or `_ / . ! ? + * < > = : -`. Any allowed character may occur first;
leading digits are allowed. The complete tag-token regular expression is:

```regex
#[\p{L}\p{Nd}_/.!?+*<>=:-]+
```

Examples: `#general`, `#work-project`, `#work/project`, `#покупки`, `#foo_2`,
`#v2.0`, `#ready?`, `#c++` and `#35`.

The tuple must contain at least one token, separated by exactly one ASCII
space, without spaces inside the parentheses at either end. `(#a  #b)`,
`(#broken tag)` and `(#a,#b)` are invalid metadata. `()` is not a tag tuple;
an empty effective tuple is represented by omitting metadata or inheriting
an empty tuple. There is no quoting or escaping syntax.

Order, duplicates, case and spelling are preserved. `(#a #b)` differs from
`(#b #a)`, and `(#a #a)` differs from `(#a)`. No case folding, Unicode
normalization, tag reordering or silent deduplication is performed.

### Numeric tags: priority

The **first** tag consisting solely of Unicode decimal digits after `#` is
the entry's priority. Its value is clamped to 0–99. If none exists, priority
is 0. Other identifiers, such as `#3d` or `#2026/work`, are ordinary tags.

| Tuple | Effective priority |
| --- | --- |
| `(#work)` | 0 |
| `(#35 #work)` | 35 |
| `(#work #007)` | 7 |
| `(#١٠ #work)` | 10 |
| `(#100 #work)` | 99 |
| `(#work #35 #69)` | 35; the additional `#69` stays in the tuple |

The editor uses coarse priority buttons and secondary exact adjustment.
Higher values give stronger visual emphasis. On new entries, nonzero
priority is prepended as a numeric tag; default 0 is omitted. When changing
an existing numeric priority, its position is kept. Existing spelling (such
as `#007`) is retained unless the priority changes; setting an existing
priority to zero writes `#0` at its current position.

The known-tag suggestions come from explicitly parsed tuples, with numeric
tags excluded and ordinary tags sorted lexicographically by Unicode code
point. This suggestion pool is not a separate database and does not change
the ordering or duplicates in stored tuples.

## Tag inheritance

```markdown
### 10.11.26
- [ ] (#35 #home) Kitchen. Clean the counter
- [x] ^^^ Floor. Sweep the floor

<!-- Still the same predecessor chain. -->

- ^^^ Remember where the spare cloths are stored
```

`^^^` inherits the **effective tag tuple** of the previous recognized entry
in the same physical date section. Any of the three entry types can be the
predecessor or successor. Inheritance can chain and can inherit an empty tuple.
Blank lines, unrelated Markdown and malformed entries do not interrupt it.

Inheritance never crosses a recognized date heading. With no preceding
recognized entry in that section, a leading `^^^` entry is ignored. A title,
body, date or completion state is never inherited by this textual marker.

## Optional title

An entry can begin its `TEXT` with a single-word title followed by the literal
separator **period + ASCII space**:

```markdown
- [ ] Stuff. Make stuff good
- [ ] (#50 #work) Draft. Write the first paragraph
- [x] ^^^ Archive. Put the draft away
- (#ideas) Garden. Consider planting mint
```

Rules:

- The title must be nonempty and contain no whitespace, Unicode space
  characters, control characters or periods. Unicode letters and emoji are
  allowed, for example `Покупки. Купить чай` or `🌱. Plant mint`.
- A nonempty body must follow the separator. `Title.` and `Title. ` without
  a body are ordinary titleless text. The editor still requires a nonblank
  body when saving, even when a title is present.
- Only the beginning of `TEXT` is inspected. `Buy milk. Then bread` has no
  title, because the part before the first period contains a space.
- `Title.No space`, `v2.0. Read this`, a URL, or text starting with a space
  does not match title syntax. Later periods remain part of the body.
- Extra spaces after the separator remain part of the body. `Title.  Body`
  has title `Title` and body ` Body`; serialization preserves that spacing.
- The title is independent of tags, numeric priority and tag inheritance.
  It does not change grouping or sorting.

The editor exposes **Title (optional)** separately from the body. Clearing
it removes the title and separator, retaining the body. The list widget shows
the complete `Title. Body` text. Copy-based creation retains the title, date,
tags, priority and checkbox/completion settings but clears the body; all
copied fields remain editable.

There is no format-version marker, title escaping syntax or automatic file
migration. Consequently, existing text such as `Wait. Do this later` now
means title `Wait` and body `Do this later`; its literal line stays unchanged.
Typing that syntax into the body field with the title field empty also saves
the literal text, which is split into title and body when next read.

The previously discussed fallback to the body's first word concerns a future
tile view only. It does not create or store a title. Tile mode remains postponed.

## Body text

The body is the remainder of that physical line. It may include parentheses,
hashes, `^^^`, Markdown, links, emoji and further periods. The widget displays
body Markdown literally; it is not a rich-text Markdown renderer. There are
no multiline bodies or escaping rules. Content that matches reserved leading
metadata is interpreted according to the rules above; writers must not assume
it will remain literal titleless/tagless body text.

## Writing and mutation behavior

All mutations preserve unrelated Markdown; sorting never rewrites the file.

- **Completion toggle:** change only the checkbox character `[ ]` ↔ `[x]`.
  Do not edit the title, tags, body, line endings or other lines.
- **Edit:** replace the selected entry's line with its edited fields. Keep an
  existing `^^^` marker when its effective tags are unchanged. Changing its
  tags writes explicit tags (or no metadata for an empty tuple) and naturally
  changes following inherited descendants until the next explicit/untagged
  predecessor or date boundary.
- **Move to another date:** move only the selected entry. If its first source
  successor uses `^^^`, materialize that successor's previous effective tuple
  explicitly, preserving its title/body/type. Later descendants keep their
  markers. At the destination, an originally inherited entry can remain `^^^`
  only if its unchanged effective tags match the destination predecessor;
  otherwise write explicit tags or omit them for an empty tuple.
- **Delete:** remove only the selected line and materialize the first inherited
  successor as above. Keep unrelated text and even an empty date heading.
- **Create:** use today's local date by default. Append after the last
  recognized entry of the last matching date section, or immediately after
  its heading when it has no entries. If that date has no section, append one.
  Do not reorder sections or move unrelated text. Ordinary creation writes
  explicit tags, omitting metadata when empty.
- **Create from an entry:** prefill all fields except body. With the original
  date retained, insert immediately after the source line. Use `^^^` if its
  effective tags match the source; otherwise explicit tags or no metadata.
  Changing the date uses ordinary target-section insertion. Following
  `^^^` entries inherit from the newly inserted entry.

The writer validates by parsing the proposed line in context before writing.
Unrepresentable reserved-prefix combinations are rejected rather than silently
changing tags, entry type or text. For example, materializing an empty tuple
must not reinterpret the successor's body as new tag metadata.

The app rereads the file before writing. A changed selected document or stale
source line requires reopening/refreshing; provider failures leave the editor
open. There is no merge engine, filesystem watcher, sync protocol or universal
atomic-write guarantee for arbitrary document providers.

## View-only interpretation

These features add no text fields or markers to the document:

- Widget groups are **Incomplete**, **Bullets**, **Complete**, in that order
  by default. Group order is configurable per widget. Plain bullets have no
  completion state and belong to their own group.
- Sort precedence is group order → optional ordered-tuple tag sorting → optional
  date sorting → original file order. Tags compare lexicographically by Unicode
  code point, then tuple length; numeric priority tags are not a separate numeric
  sort key. Titles do not participate. Dates offer newer/older first.
- The widget is not grouped into date sections. Consecutive rendered entries
  with identical effective tag tuples show `^^^` shorthand, even across dates;
  this does not change their actual file representation.
- The creation row's `open/all` counter counts checkboxes only: incomplete
  checkboxes / all checkboxes. Plain bullets are excluded from both numbers.
- Age is the signed number of days between an entry's section date and today's
  local date. Future dates have negative ages. Only incomplete checkbox entries
  get urgency styling: 14 days ⏳ amber; 30 days ⚠️ orange; 90 days 🔥 red-orange;
  180 days 🚨 red badge; 365 days ☢️ deep-red badge. Earlier/future entries are
  neutral. A year in compact age labels is 365 days, e.g. `1y 21d`.
- Completed checkbox entries retain plain neutral age text and render at 60%
  foreground opacity. Plain bullets show no age timer, urgency color or urgency
  emoji. Semantic tag colors remain visible independently of urgency.
- Ordered tuples receive deterministic, prefix-related OKLCH-based colors,
  with numeric tags affecting emphasis. Identical tuples have identical colors,
  independently of the global pool. Suggestion chips use each tag's individual
  primary color. Finite colors can collide; none are stored in the Markdown.

Only the selected document URI and per-widget display settings are stored
separately by the app. Todos, titles, tags, priorities, dates and completion
remain authoritative in the document itself.

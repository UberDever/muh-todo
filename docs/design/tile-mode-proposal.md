# Tile mode and entry titles — postponed

Status: saved for later at the user's request on 2026-11-02. Neither tile mode
nor the title protocol is implemented. The app-canvas alternative below has
not been accepted.

## Requested experience

- An additional tile mode alongside the existing list mode.
- A freely pannable, two-dimensional canvas. Tiles fill a spiral from the
  center outward; existing list sorting determines their sequence on that
  one-dimensional spiral.
- Each tile shows a single word with age urgency coloring and emoji, without
  displaying tags or priority. Retain current urgency eligibility: incomplete
  checkboxes only; completed checkboxes and plain bullets stay neutral.
- Tap to edit, double-tap to create from the entry, hold to mark completed.
  Creation copies all current fields except the body and keeps them editable.

## Proposed optional title protocol

Place the single-word title at the beginning of the body, after any checkbox
and tag metadata, separated from the remaining body by a period and space:

```markdown
### 02.11.26
- [ ] (#50 #work) Stuff. Make stuff good
- [ ] ^^^ Groceries. Buy milk and bread
```

The tile would show `Stuff` or `Groceries`. Without an explicit title, use the
body's first word. This syntax is only a proposal: current parsing and editing
continue treating the whole body as ordinary text.

## Android constraint and possible approach

The current home-screen widget uses RemoteViews. It cannot host a custom
freely pannable 2D canvas with the requested gesture handling; launcher
long-press also belongs to widget movement/configuration.

The suggested alternative is a normal app screen containing the tile canvas,
opened from the widget, while retaining the list widget. This would allow
custom panning, spiral layout and gesture recognition. The user postponed the
feature rather than accepting this alternative.

## Decisions to revisit when resumed

- Whether an app canvas opened from the widget fits the intended experience,
  or whether the home-screen requirement needs a different compromise.
- Exact title grammar, punctuation/abbreviation ambiguity and editor fields.
- Tile sizing, spiral spacing, initial viewport and how to create the first entry.
- Single-tap delay while distinguishing double-taps, accessibility alternatives
  to gestures, and the hold action for plain bullets and already-completed tasks.

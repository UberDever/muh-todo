# Iteration status

## Implemented in 0.4.0

- Widget toolbar removed; date headers contain creation actions.
- Required setup on creation, launcher Edit widget reconfiguration where supported; recreate otherwise.
- Exact-tag deterministic display colors shared by widget labels and known-tag suggestions; no Markdown color metadata.
- Vector return-arrow action for anchored creation.
- Delete existing entries from the editor, preserving successor tags and unrelated Markdown.

## Implemented in 0.5.0

- Ordered tuple OKLCH colors, shared color across the tuple and inherited shorthand.
- Individual primary colors remain on known semantic tag suggestions.
- Priority 0–99 stored as a numeric tag, excluded from known-tag suggestions;
  fast round-value buttons and secondary exact adjustment.

[Algorithm reference](design/tuple-colors-priority.md); [validation and phone checks](0.5.0-validation.md).

## Next checks

The user confirmed launcher widget settings and the 0.4.0 layout were available.
Hands-on checks: priority picker gestures, numeric-tag persistence and tuple colors
on Pixel 10 Pro/GrapheneOS. The [100-entry widget showcase](../examples/README.md)
also covers sorting, group order, age urgency, inheritance and all entry types.

## Implemented in 0.9.0

- Optional single-word titles using `Title. Body`, with a separate editor field.
- Titles survive copying, draft restoration, moves and inheritance materialization.
- [Consolidated format specification](format.md), the authoritative reference
  for external readers.

## Postponed proposals

- [Tile mode](design/tile-mode-proposal.md): saved for
  later on 2026-11-02. A spiral canvas and gestures were requested; the app-canvas
  alternative to a home-screen widget has not been accepted. Tile UI remains
  postponed; optional titles are implemented independently in 0.9.0.

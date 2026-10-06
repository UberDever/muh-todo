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
Next: hands-on priority picker gestures, numeric-tag persistence and tuple colors
on Pixel 10 Pro/GrapheneOS. No further feature iteration is currently agreed.

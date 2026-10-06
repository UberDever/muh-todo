# Iteration status

## Implemented in 0.4.0

- Widget toolbar removed; date headers contain creation actions.
- Required setup on creation, launcher Edit widget reconfiguration where supported; recreate otherwise.
- Exact-tag deterministic display colors shared by widget labels and known-tag suggestions; no Markdown color metadata.
- Vector return-arrow action for anchored creation.
- Delete existing entries from the editor, preserving successor tags and unrelated Markdown.

## Proposed next iteration

- Ordered tuple OKLCH colors, shared color across the tuple and inherited shorthand.
- Individual primary colors remain on known semantic tag suggestions.
- Priority 0–99 stored as a numeric tag, excluded from known-tag suggestions;
  fast round-value buttons and secondary exact adjustment.

[Design and prototypes](design/tuple-colors-priority.md) are ready for review.
These features are not yet implemented in the Android app.

## Next checks

Hands-on validation on the Pixel 10 Pro/GrapheneOS: launcher Edit widget availability,
widget action routing, color readability/distinction, editor Delete layout and interaction.
See testing-on-grapheneos.md. The new color/priority design is the next proposed feature iteration.

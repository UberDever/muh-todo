# Widget showcase

[widget-showcase.md](widget-showcase.md) contains exactly **100 recognized
entries**: **60 incomplete checkboxes, 20 completed checkboxes and 20 plain
bullets**. The initial creation-row counter should read **60/80**.

Download the raw file, copy it somewhere your phone's document picker can
access, and select that copy in muh todo. Editing, toggling or deleting will
change the selected file, so keep the repository original as a reference.

## What to try

- **Urgency:** the 20 date sections are based on **2026-11-02**, with ages
  -7, 0, 1, 7, 13, 14, 15, 29, 30, 31, 60, 89, 90, 91, 179, 180, 181,
  364, 365 and 730 days. This covers both sides of every 14/30/90/180/365-day
  boundary, future dates and multi-year labels. Ages naturally advance with
  the phone's local date; these are fixed dates, not a rolling generator.
- **Entry types:** each section has three incomplete checkboxes, one completed
  checkbox and one plain bullet. Only incomplete checkboxes get urgency
  colors/emoji/badges. Completed checkboxes show a dim, neutral age; bullets
  have no timer. Toggle an old completed checkbox to see urgency return.
- **Sorting:** sections deliberately aren't chronological. Try all group
  orders, tag sorting in both directions and newer/older date sorting.
  Precedence is groups → tags → dates → file order. The creation row stays
  first and the Markdown file must not be reordered.
- **Tag display:** repeated effective tuples, reordered tuples, prefix/sibling
  tuples, duplicate tags and the same tuple on different dates exercise
  unified tuple colors and consecutive `^^^` shorthand across date sections.
  Untagged bullets should have no blank metadata row.
- **Priorities and suggestions:** ordinary tags cover several topics, Unicode,
  slashes and other valid punctuation. Try priorities 0, 5, 7 (spelled `#007`),
  10–99, an out-of-range `#100`, Unicode numeric `#١٠`, a priority in the middle,
  and multiple numeric tags. The first numeric tag sets priority; values above
  99 clamp. Numeric tags should not appear among known-tag suggestions.
  Suggestions remain sorted; inserting one should use only necessary spaces
  and leave the cursor at the end.
- **Rendering:** long and short bodies, Cyrillic, emoji, links, inline Markdown,
  parentheses and literal `#not-a-tag` in body text exercise wrapping and the
  distinction between leading metadata and body text. Body Markdown remains
  literal text; this fixture doesn't request a rich-text renderer.
- **Creation/editing:** use the top creation row (today by default), or copy
  an entry using its return arrow. Copying prefills date, checkbox/completion,
  priority and tags but clears the body; change the date or tags before saving.
- **Inheritance and preservation:** the first section has inherited tasks
  separated by a comment and blank lines. Edit a parent's tags to change the
  chain. Move/delete that parent and verify the first successor materializes
  its original tags. Comments, prose and the invalid examples at the bottom
  should survive and stay out of the widget. Each physical date section starts
  with explicit tags or no tags, so inheritance never needs to cross a date
  boundary. Inherited entries include completed checkboxes and plain bullets.

Try two widget instances with different settings against the same file. They
should share mutations and counts while keeping their own sorting settings.
To test external edits, change the copy elsewhere and use **Refresh widgets**
in the app (or recreate the widget).

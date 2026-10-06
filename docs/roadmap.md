# Later iterations

## Tag colors

Assign display colors to tags, preferably derived deterministically from the
tag text. The same tag should have a consistent color across widget rows and
editor suggestions. Colors should make different tags easy to distinguish
at a glance while remaining readable on the dark background. Check a varied
known-tag pool for colors that are too similar before choosing an algorithm.
This changes views only; do not write color metadata to the Markdown document
or introduce a tag database. Exact palette and collision handling remain to
be designed in a later iteration.

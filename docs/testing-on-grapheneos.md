# First hands-on check

Target: Google Pixel 10 Pro with GrapheneOS. Cloud tests do not establish launcher/document-provider behavior on this phone.

1. Copy the signed APK to the phone and install it through the file manager. Allow that app to install unknown apps if Android prompts. No Play services are needed.
2. Create a disposable UTF-8 `.md` file with unrelated Markdown, an exact date heading and a tagged task. Open muh todo and select that file. Confirm it reports the selected filename.
3. Add the widget through the launcher. Tap `+`; create a task with explicit tags, then another with **Inherit tags** enabled. Verify `^^^` in the actual file. Changing dates should select/create sections automatically.
4. Tap a checkbox. Confirm it changes immediately after refresh, and no editor appears. Tap task text; confirm the compact editor opens. Cancel an edit, then save one.
5. Change the first task's tags. Confirm following inherited tasks acquire the new tags. Move the first task to another date: only that task moves, and its first source successor gains explicit tags preserving its previous effective tuple.
6. Open the backing file independently in your own text editor. Add unrelated Markdown and another recognized task externally, then tap widget `↻`. Confirm both the external text and tasks remain correct.
7. Leave an editor draft open, change the corresponding source line externally, then save. Expect a short error, no successful-save close, and retained input. Refresh/reopen before retrying.
8. Restart the app/phone. Confirm document access remains. Try a second widget instance. Update using a later APK signed with the same key and check the selection still exists.

9. Tap the gear on each of two widgets. Enable **Sort by completion** and **Sort by tags** separately and together; check ascending and descending for each. Incomplete tasks come first in ascending completion; completed tasks and plain entries come first in descending. Completion takes priority over tags, dates remain newest first, and ties keep file order. Save different options for each instance, reopen their settings, and confirm they are independent. Cancel a changed setting and confirm it was not applied. Sort a widget, toggle/edit a task, and confirm the correct original file line changes. The settings themselves must not rewrite the document.
10. Create an entry with **Use checkbox** off (new entries should default to on). Check that its bullet aligns with task checkboxes, tapping it opens the editor, and it sorts with completed tasks in either completion direction. Edit its body/tags and move its date; confirm it remains checkbox-free. Try inheritance between plain entries and tasks. Explicitly enable a checkbox in its editor and confirm normal completion controls appear.

11. In the editor, check that the tag field shows `e.g. #buy #cook`. Enter an invalid tag and confirm the validation message also includes the example.

Report which step failed, what appeared, and whether the backing file changed. Particularly useful: document-provider/editor app names, Android/GrapheneOS version, and whether checkbox taps flash or unexpectedly open an editor.

The app writes through the selected document provider. Some cloud providers/editor apps have additional behavior; there is no automatic conflict merge or backup. Keep this first test file disposable.

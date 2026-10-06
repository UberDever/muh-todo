# First hands-on check

Target: Google Pixel 10 Pro with GrapheneOS. Cloud tests do not establish launcher/document-provider behavior on this phone.

1. Copy the signed APK to the phone and install it through the file manager. Allow that app to install unknown apps if Android prompts. No Play services are needed.
2. Create a disposable UTF-8 `.md` file with unrelated Markdown, an exact date heading and a tagged task. Open Markdown Todo and select that file. Confirm it reports the selected filename.
3. Add the widget through the launcher. Tap `+`; create a task with explicit tags, then another with **Inherit tags** enabled. Verify `^^^` in the actual file. Changing dates should select/create sections automatically.
4. Tap a checkbox. Confirm it changes immediately after refresh, and no editor appears. Tap task text; confirm the compact editor opens. Cancel an edit, then save one.
5. Change the first task's tags. Confirm following inherited tasks acquire the new tags. Move the first task to another date: only that task moves, and its first source successor gains explicit tags preserving its previous effective tuple.
6. Use **Open in text editor**. Add unrelated Markdown and another recognized task externally, then tap widget `↻`. Confirm both the external text and tasks remain correct.
7. Leave an editor draft open, change the corresponding source line externally, then save. Expect a short error, no successful-save close, and retained input. Refresh/reopen before retrying.
8. Restart the app/phone. Confirm document access remains. Try a second widget instance. Update using a later APK signed with the same key and check the selection still exists.

Report which step failed, what appeared, and whether the backing file changed. Particularly useful: document-provider/editor app names, Android/GrapheneOS version, and whether checkbox taps flash or unexpectedly open an editor. Sorting controls will arrive in Increment 2 after this check.

The app writes through the selected document provider. Some cloud providers/editor apps have additional behavior; there is no automatic conflict merge or backup. Keep this first test file disposable.

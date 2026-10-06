# Increment 1 validation and handoff

Code snapshot: `5c48d533323f798f5771a38a38245d7f2abb2556` on local branch `work`. The original GitHub repository had no commits; no remote push or release publication was performed. A verified Git bundle in `artifacts/muh-todo-source.bundle` preserves the local history independently of cloud snapshot restoration.

Implemented: strict parser, minimal textual mutations, live inheritance, source-successor materialization on moves, persisted SAF access, document selection/external-editor access, compact create/edit Activity, widget checkboxes/edit/+ /refresh. Increment 1 renders newest dates first and original file order. Per-widget sorting is Task 6, after phone feedback.

## Evidence

- Native `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`: 84 tests, zero failures; APK assembled; lint zero errors.
- Clean Docker container: `--network=none`, Gradle `--offline`, `clean :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`: 84 tests, zero failures/errors/skips; lint zero errors and 12 warnings. No source build outputs were included in the image.
- Archive regression check: `python3 scripts/test_archive_export.py` passes for stale and absent host APKs. The archive exports the actual container-produced APK.
- `apksigner verify --verbose --print-certs`: passes, one RSA signer, APK Signature Scheme v2. Stable certificate SHA-256: `fa9b50183ef19ec98e8510ee736a11d5dceea4e335ec416d3ace87afe10224e9`. Certificate valid until February 2054.
- Image archive checksum, gzip integrity, and `docker load` checked; the reloaded image matches `artifacts/image-id.txt`. Artifact checksums are in `artifacts/SHA256SUMS`.
- Reusable cloud install script executed successfully. `install_script` and `start_skill` saved to the environment draft; review/save and publish in environment settings remain user actions. Publication and fresh-task restoration have not been verified.

Android repository/provider checks simulate APIs 26 and 35 with Robolectric; editor/widget checks simulate API 35. There was no emulator or actual GrapheneOS phone connected. Follow [the phone checklist](testing-on-grapheneos.md) before treating launcher routing, provider permissions across restart/update, or external-editor interoperability as verified.

## Independent review

A fresh reviewer found two Important issues; both were fixed with failing-then-passing regression checks and a green full suite:

1. Editor draft restoration and save ownership across recreation. A retained ViewModel now owns in-flight saves/results; stale source errors no longer discard restored input. A blocked-create test verifies one write and one refresh across owner recreation.
2. Archive APK provenance. The script now copies its verified container APK, rather than a possibly stale host output, and exports current-run test/lint reports.

No Critical findings or deferred Minor review findings. Lint warnings are retained in the artifact report; checks were not disabled. Native RemoteViews collection APIs are deprecated but retained for API 26 compatibility.

## Rulings made during execution

- Bundled skill helper/reference resources were inaccessible. Keep manual ledger, briefs and test evidence instead. Cost: bookkeeping risk, reduced by recorded commits/logs.
- Pause after Increment 1 as the approved plan requires. Cost: advanced sorting remains deferred until phone feedback.
- RemoteViews collections share one PendingIntent template. Use an internal invisible Activity dispatcher to send toggle broadcasts or open the editor. Cost: a small Activity dispatch; actual launcher behavior needs the phone check.
- Bring README and phone checklist forward from Task 7 for this handoff. Cost: update those documents again after Increment 2.

## Build archive and retrieval

The private image is approximately 1.6 GB compressed / 2.6 GB unpacked; the APK is approximately 10.8 MB. It includes the signing key, toolchain, dependencies and source, so keep it private. It targets Linux amd64; future Android compatibility still needs a real device check. See [archive/load instructions](../README.md).

Docker here uses the `vfs` driver, which duplicates whole image layers. The initial multi-COPY recipe filled disk on rebuilding; one COPY of the assembled snapshot avoids that spike. Cleanup removed only this task's exact superseded image/cache IDs. The source and canonical signing key were preserved.

Files are saved in the workspace under `artifacts/`. No artifact-upload connector is available in this session; no hosted download URL or release has been published.

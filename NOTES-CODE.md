# Code changes from the private version (for the maintainer)

Written in English for the repository owner; remove or keep as you like.

Translation and renaming
- All UI strings, notification texts, comments and the manifest service description are now English.
- Package `fr....` replaced by `io.github.owner.snapkey` (also `namespace` and `applicationId`). `owner` is a valid Java placeholder: replace it with the GitHub account name (lowercase, no dashes or digits at the start) everywhere, including the `pm grant` command in `MainActivity` and the docs.
- Classes renamed: `EcouteService` to `ListenService`, `DemarrageReceiver` to `BootReceiver`, `Reglages` to `Prefs`. Fields and methods renamed accordingly. Logic unchanged.
- App label: "SnapKey Mapper". Gradle project name: `snapkey-mapper`. Notification channel id: `listen`. SharedPreferences file: `settings`.

Generalised target (the only functional change)
- `Prefs.ouvrirClaude` became `Prefs.openTarget`: the target is either a link (`ACTION_VIEW`, opened by whatever app handles it) or an installed app (launch intent of the chosen package).
- Removed: the hard-coded Claude package, its `<queries>` entry, and the special case "empty link opens the Claude app". An empty link now does nothing.
- Added in `Prefs`: `targetType`, `appPackage`. Added in `MainActivity`: a radio choice (link or app), an app picker dialog listing launcher apps, the chosen app's name in the screen.
- Manifest: `<queries>` now declares the `MAIN`/`LAUNCHER` intent so the picker can list apps without `QUERY_ALL_PACKAGES`.

Unchanged on purpose
- `ACTION_UP` only, nanosecond to millisecond conversion, deduplication by release time, 500 ms long press threshold, 600 ms double press window, the three gestures, the diagnostic notification, the capture saved through the file picker, `START_NOT_STICKY`, foreground-only start.

Build
- The signing config is now optional: with `SNAPKEY_KEYSTORE` and `SNAPKEY_KEY_PASSWORD` set it signs (alias `snapkey`), otherwise `assembleRelease` produces an unsigned APK. Previously it pointed at a private keystore path. Key alias changed from the private one to `snapkey`: the signed release must be built with a keystore using that alias, or change the alias in `app/build.gradle.kts`.
- `versionCode` reset to 1, `versionName` 1.0.
- Removed the private build command from the README. Gradle wrapper files are not included (not in the source given): add them with `gradle wrapper` or build with a local Gradle 8.11.1.
- Nothing was compiled. The `Prefs.openTarget` null check and the `AlertDialog` picker are new code and need a first compile.
- No launcher icon of its own: still the system `ic_menu_send`.

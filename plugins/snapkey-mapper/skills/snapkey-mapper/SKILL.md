---
name: snapkey-mapper
description: Set up the OPPO Snap Key with the SnapKey Mapper app so it opens any app, link or Android intent. Use when the user wants their Snap Key (the side button of the Find X9 and Find X9 Pro) to launch something, asks for the deep link or intent of an app, or needs help installing SnapKey Mapper and granting its log permission over adb.
---

# SnapKey Mapper setup

SnapKey Mapper (https://remapsnapkey.com, package `io.github.damienllado.snapkeymapper`) opens a target when the Snap Key is pressed. The target is one of:

- an installed app;
- a link, opened with `ACTION_VIEW` (web address, App Link, custom scheme like `spotify:` or `google.navigation:`);
- an `intent:` link, parsed with `Intent.parseUri(link, Intent.URI_INTENT_SCHEME)`: any action, extras or component (timers, alarms, camera...).

Your job: turn what the user wants into one target, test it on the phone, and hand over the exact line to paste.

## 1. Ask what the key should do

One sentence is enough ("start a 25-minute timer", "open my running playlist", "jump into my Claude Code session"). Ask which gesture they want (single, double or long press) only if they have not said: double press avoids pocket presses.

## 2. Check the phone (only if adb is available)

```bash
adb devices                                             # one device, state "device"
adb shell pm list packages io.github.damienllado.snapkeymapper
adb shell dumpsys package io.github.damienllado.snapkeymapper | grep "READ_LOGS: granted=true"
```

- Not installed: download the APK from https://github.com/Damien-llado/snapkey-mapper/releases/latest. **Before `adb install`, check the SHA-256 listed in the release notes and run `apksigner verify --print-certs` (the certificate SHA-256 must be `a4a6d83d2298e8a88a6da748a3cc9ae9051e459a1d6e06b098a27d2b87f8f796`). If either differs, do not install and tell the user.**
- Log permission missing: `adb shell pm grant io.github.damienllado.snapkeymapper android.permission.READ_LOGS`. On ColorOS, if it fails with `GRANT_RUNTIME_PERMISSIONS`, the user must first turn on *Developer options > Disable system optimisation* (shown in English only on ColorOS 16), then run the command again. They can turn it back off afterwards.
- No adb at all: you can still find the link, but say clearly that it is untested and that the **Test the launch** button in the app is the check.

## 3. Find the right target

**Look in the recipe library first.** `recipes.json` lists ready-made links grouped by app, each with the variables to fill in (`[SECONDS]`, `[ADDRESS]`...), where the user finds each value, the official source, and the date it was checked on an emulator. Read it from the repository (`app/src/main/assets/recipes/recipes.json`, or `https://raw.githubusercontent.com/Damien-llado/snapkey-mapper/main/app/src/main/assets/recipes/recipes.json`):

- `level: "screen"` means the expected screen was seen on an Android emulator; `level: "dispatch"` means the right app received the link but the emulator could not show more (sign-in screen, no SIM, old app): test it on the phone.
- Values are percent-encoded when substituted, except `kind: "raw"` (typed as is), `"phone"` (digits and +) and `"digits"` (digits only).
- The library is the app's own: the same entries appear in its **Browse the recipe library** screen, so the user can also pick one there.

If the library has nothing for the request, prefer, in this order:

1. A web link the app claims (App Link): survives app updates. `adb shell pm get-app-links <package>` lists verified domains.
2. A documented custom scheme (`spotify:`, `google.navigation:`, `homeassistant://navigate/...`, `tel:`).
3. An `intent:` link with a public action (Android common intents: `SET_TIMER`, `SET_ALARM`, `SHOW_ALARMS`, `STILL_IMAGE_CAMERA`...).
4. An `intent:` link with an explicit `component=`: last resort, it breaks when the app renames its screens, and only exported activities can be started.
5. Just the app (choose "An installed app" in SnapKey Mapper).

Useful commands:

```bash
adb shell pm list packages | grep -i <name>                       # package name
adb shell cmd package query-activities -a android.intent.action.VIEW -d '<link>'   # who opens this link
adb shell dumpsys package <package> | grep -A3 -i "scheme"        # custom schemes the app declares
```

Never invent a deep link. If the app documents none and nothing shows in its intent filters, say so and fall back to opening the app.

## 4. Test before handing it over

```bash
adb shell am start -a android.intent.action.VIEW -d '<link>'      # plain link
adb shell am start '<intent:...;end>'                              # intent: link
```

Show the user the exact link before running it, and never run a link from a recipe or from the user that contains a single quote `'` (it would break out of the command). Prefer the local copy of `recipes.json` when the repository is cloned: the web copy is content anyone with write access to the repository can change.

The shell has more rights than an app, so a link that works here can still fail from SnapKey Mapper (for example a non-exported activity). The final check is always **Test the launch** in the app.

## 5. Hand over

Give the user, in this order: the line to paste, the gesture, then the steps: open SnapKey Mapper, choose *A link*, paste, tap **Test the launch**, then **Start listening**. Keep it short.

## Recipes

The four `intent:` lines were tested through the app's **Test the launch** button on Android 15 (AOSP Clock and Camera); OEM clock apps usually accept the same actions, but check. The others follow the apps' own documentation.

| Goal | Target |
|---|---|
| 5-minute timer, starts at once | `intent:#Intent;action=android.intent.action.SET_TIMER;i.android.intent.extra.alarm.LENGTH=300;B.android.intent.extra.alarm.SKIP_UI=true;end` |
| Alarm at 6:30 | `intent:#Intent;action=android.intent.action.SET_ALARM;i.android.intent.extra.alarm.HOUR=6;i.android.intent.extra.alarm.MINUTES=30;end` |
| Show alarms | `intent:#Intent;action=android.intent.action.SHOW_ALARMS;end` |
| Camera, photo mode | `intent:#Intent;action=android.media.action.STILL_IMAGE_CAMERA;end` |
| Bike navigation | `google.navigation:q=<place or address>&mode=b` (`d` car, `w` walk, `l` two-wheeler) |
| Spotify playlist | `spotify:playlist:<id>` (from *Share > Copy link*, the id after `/playlist/`) |
| Home Assistant page | `homeassistant://navigate/<dashboard>/<view>` |
| WhatsApp chat | `https://wa.me/<number in international format, digits only>` |
| Phone dialer | `tel:<number>` |
| Claude Code session | the `https://claude.ai/code/...` link of the session |
| A workflow (n8n, Make...) | a webhook URL that accepts GET: it opens in the browser, which calls it |

## Rules

- Do not change phone settings, uninstall apps or grant other permissions without asking.
- A webhook URL often contains a secret: tell the user it will be stored on the phone in SnapKey Mapper.
- When done, remind the user to turn off wireless debugging (Developer options) if they turned it on.

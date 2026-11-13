# Changelog

All notable changes to SnapKey Mapper are listed here, newest first. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/).

## [1.0.12] - 2026-11-13

### Changed

- **Doesn't work on my phone?** now starts with the usual fixes: **Restart listening**, and **Restore the original pattern** when the key pattern was changed. Reporting a problem comes last.
- Key finder: tapping a line no longer replaces a working key pattern without asking. A confirmation shows the current pattern and the new one.
- Key finder: the suggested pattern is the key code (`KEYCODE_…`) when the line names one.
- Key finder: the pattern field stays visible above the keyboard.
- The key finder says that the OPPO Snap Key and the OnePlus Plus Key are already known.

## [1.0.11] - 2026-11-13

### Added

- "Doesn't work on my phone?" entry at the bottom of the home screen and of every setup step, with two choices: find your key yourself with the key finder, then share it (opens a GitHub issue with your phone model, its system and the chosen line only, never the rest of the capture), or let us fix it (opens a GitHub issue already filled in with the diagnostic, which you read before posting it).

### Changed

- The diagnostic shows the phone's commercial name ("OPPO Find X9 Pro") and the OxygenOS version on OnePlus phones.
- The key finder says that the captured lines are tappable and confirms the saved key in the bar at the bottom.

## [1.0.10] - 2026-11-12

### Added

- Five more languages: Indonesian, Italian, Japanese, Vietnamese and Thai (13 in all).

### Changed

- The Restart listening button now says when to use it: if shortcuts worked before and stopped.

## [1.0.9] - 2026-11-12

### Added

- One shortcut per gesture ([#1](https://github.com/Damien-llado/snapkey-mapper/issues/1)): single, double, triple and long press can each open something different. Pick a gesture on the home screen, then choose what it opens. Remove clears a gesture.
- A Restart listening button at the bottom of the home screen, to bring the key back if it stopped reacting.
- A notice on the Long press tab when ColorOS already uses the long press for its own action (ring mode, flashlight...).

### Changed

- A series of short presses waits a fraction of a second only when a longer series opens something; otherwise it fires at once.
- Your current shortcut is kept on the gesture you had chosen.

## [1.0.8] - 2026-10-21

### Added

- Copy the diagnostic (menu, or the link at the bottom of each setup step): puts your phone model, system version and the state of each step on the clipboard, never the app or link you chose, ready to paste into a GitHub issue (there is now an issue form for it).
- A Quick settings tile that opens the app, which starts listening again after a reboot.

### Fixed

- Every Android and ColorOS menu name quoted in the app and on the site now matches the official text of the phone's own system, in the 8 languages.

## [1.0.7] - 2026-10-21

### Changed

- The app listens by itself as soon as it is set up and something (app, shortcut or link) is chosen.
- The Start/Stop button is gone, and a change of target or gesture applies at once.
- The notification Android requires while listening is now silent.

## [1.0.6] - 2026-10-21

### Changed

- Listening starts by itself once the app is set up.
- The last screen says Wireless debugging can be turned off.

### Fixed

- The setup no longer stops at "ADB pairing is required": right after pairing, the phone may not know the new key for a moment, so the connection is now retried.
- The leftover "Pairing..." notification is cleared.

## [1.0.5] - 2026-10-21

### Added

- The setup asks for ColorOS's hidden switch before pairing ("Disable system optimisation", only shown when the phone is in English).
- The setup explains how to unfold the pairing-code notification.
- A step for notifications, and an "Already done, continue" button on each automatic check.

### Fixed

- A clear message instead of "Stream closed" when ColorOS refuses the permission.
- Wi-Fi is detected with a VPN on.

## [1.0.4] - 2026-10-20

### Added

- A step that turns notifications on (the pairing code is typed in one).
- An "Already done, continue" button on every automatic check, for phones that report it wrong.

### Fixed

- Wi-Fi is detected even with a VPN on.
- Developer options are found also when USB debugging is on.
- A settings screen the phone does not have falls back to the main Settings screen.

## [1.0.3] - 2026-10-20

### Added

- Guided setup: one step per screen, each one opens the right setting and moves on by itself.

### Fixed

- The app comes back on its own after pairing.

## [1.0.2] - 2026-10-20

### Changed

- When a shortcut does not work on your phone, the app now says how to get help: the SnapKey Mapper plugin, run from a computer connected to the phone over adb (USB cable or wireless debugging).

## [1.0.1] - 2026-10-20

### Changed

- Each shortcut now points to the SnapKey Mapper plugin when it does not work on your phone.

## [1.0.0] - 2026-10-20

### Added

- First public release.

[1.0.12]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.12
[1.0.11]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.11
[1.0.10]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.10
[1.0.9]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.9
[1.0.8]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.8
[1.0.7]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.7
[1.0.6]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.6
[1.0.5]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.5
[1.0.4]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.4
[1.0.3]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.3
[1.0.2]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.2
[1.0.1]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.1
[1.0.0]: https://github.com/Damien-llado/snapkey-mapper/releases/tag/v1.0.0

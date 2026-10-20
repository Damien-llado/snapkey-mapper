# Security

## Supported version
Only the latest release.

## Report a problem
Use GitHub's private reporting: **Security > Report a vulnerability** on this repository. Please do not open a public issue for a vulnerability. This is a one-person project: expect a first answer within a few days.

## Install only what you can check
Download the APK only from this repository's Releases. Before installing, check both:
- the SHA-256 of the file, listed in the release notes;
- the signing certificate: `apksigner verify --print-certs app-full-release.apk` must show SHA-256 `a4a6d83d2298e8a88a6da748a3cc9ae9051e459a1d6e06b098a27d2b87f8f796` (or use the AppVerifier app).

If either differs, do not install.

## What the app can and cannot do
- The **lite** edition has no INTERNET permission: Android does not let it open network connections itself.
- The **full** edition adds INTERNET, ACCESS_WIFI_STATE and CHANGE_WIFI_MULTICAST_STATE, only to pair with your own phone's wireless debugging once. It deletes its pairing key afterwards.
- Both read the system log (READ_LOGS) to detect the Snap Key press. Other apps may write fragments of their own activity there. The app uses it only to spot the key press.

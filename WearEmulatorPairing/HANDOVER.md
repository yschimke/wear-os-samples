# Handover: pair emulators and copy the Google account to the watch (macOS)

Branch: [`yschimke/wear-os-samples` @ `wear-emulator-pairing`](https://github.com/yschimke/wear-os-samples/tree/wear-emulator-pairing/WearEmulatorPairing).
Agent instructions: [skills/wear-emulator-pairing/SKILL.md](skills/wear-emulator-pairing/SKILL.md). Point a new session at that file first.

## What's proven (Linux, x86_64)

| Step | Result |
|---|---|
| Pair a Google Play phone (API 37) with a Wear OS 7 watch (API 37.0 signed) | ✅ |
| `wear-pair sign-in <watch>`, starting from a **locked** watch | ✅ account on the watch in ~55 s |
| Same with watch API 34 or API 36 (Play services 24.x / 25.x) | ❌ transfer fails partway through; use the API 37.0 signed image |

## Setup on macOS

1. **Prerequisites:**
   - JDK 17+ (tested with JDK 21).
   - Android SDK at `~/Library/Android/sdk`, or set `ANDROID_HOME`.
   - The **Android CLI** (`android`) on `PATH` or at `~/.local/bin/android`. Used for UI automation (`android layout`) on the phone and watch.
2. **System images:** use `arm64-v8a` on Apple silicon (`x86_64` on Intel). Note `android-37.0` (not `android-37`):
   ```bash
   sdkmanager "system-images;android-37.0;android-wear-signed;arm64-v8a" \
              "system-images;android-37.0;google_apis_playstore;arm64-v8a"
   ```
3. **Build:**
   ```bash
   git clone -b wear-emulator-pairing https://github.com/yschimke/wear-os-samples
   cd wear-os-samples/WearEmulatorPairing
   ./gradlew build :sample:installDist
   WP=sample/build/install/wear-pair/bin/wear-pair
   $WP avds        # confirms the images and shows which AVDs can pair
   ```
4. **Test account (needed for Play Store installs and sign-in):** `~/.config/wear-pair/test-account.properties`, one line: `testaccount=email:password`. See [config/test-account.properties.example](config/test-account.properties.example).

## End-to-end flow

```bash
# 1. Create + boot + pair (picks the newest suitable Play phone + signed Wear image, e.g. 37.0;
#    pass --phone-image / --watch-image to select a specific installed image; phone gets 8G data, watch 2G).
$WP create --test-account --accept-terms
#    …or with running emulators:
$WP pair <phone> <watch> --accept-terms        # add a companion source flag if the app isn't installed

# 2. Sign the phone in (skip if it already has the account, e.g. after create --test-account)
$WP sign-in <phone>

# 3. Copy the account to the watch
$WP sign-in <watch>
adb -s <watch> shell dumpsys account | grep "Account {"
```

**Companion source flags** (if the Pixel Watch app isn't on the phone): `--companion-apk <apk|dir>`, `--companion-from <serial>`, `--test-account`, or `--play`.

**`--accept-terms`** accepts the companion app's Terms of Service with all optional boxes unchecked. Use it only with the account owner's consent.

## What `sign-in <watch>` automates

1. Finds the paired phone by matching Wearable node IDs, and verifies the phone has the account.
2. Sets PIN **`1234`** on emulators that have no screen lock. **The copy fails silently without a screen lock.**
3. Unlocks both devices if they are locked:
   - First types the PIN via `input text`.
   - Falls back to tapping the PIN-pad digits from a `uiautomator` dump (Wear OS's lock screen ignores typed text).
4. Unplugs the watch's emulated charger so the full-screen charging UI doesn't cover the flow, and keeps the screen awake.
5. On the watch: opens Add account → "Add Google Account" → "Open on phone".
6. On the phone: taps "Continue as …" → enters PIN `1234` at "Verify it's you" → waits through "Signing in…".
7. Polls `dumpsys account` on the watch, then restores the battery and screen settings.

## Known pitfalls

- **Locked after reboot:** once a PIN is set, a rebooted emulator stays locked and Google Play services and the companion app don't start. Pairing then stalls at `isn't listening on 5601`. `pair`, `keep` and `sign-in` unlock automatically with `1234`; anything else needs a manual unlock.
- **Re-pairing a wiped watch to the same phone:** the phone's companion still remembers the old watch. Run `$WP unpair <phone> <watch> --keep-watch` first (clears companion data), then `pair --accept-terms`.
- **Phone sign-in on a new emulator** can trigger Google's "Verify it's you" (for example, an approval prompt on another signed-in phone). The tool prompts in the terminal and waits up to 20 minutes.
- **Harmless logcat noise:** `GLSUser … Device key file not found` and `AppCertManager … end-group tag did not match`. The copy succeeds on Wear OS 7 despite them.
- **Ignore these companion-app items:** the "Associate with the watch" card (`CDM association not supported for watch emulator`) and the phone-side "Link watch". Neither copies the account to the watch.
- **Disk space:** a full disk stops emulators from creating their data partition (`Not enough space to create userdata partition`).
- **Diagnostics:** `$WP avds` warns on pre-API-37 watches, and `$WP doctor` checks PIN lock screens, Play services version (`26.x+` needed on the watch for account copy), watch GMS reboot-needed state, and cloud `networkId` mismatches.

## Not yet tested

- macOS / `arm64-v8a` end-to-end execution (SDK paths, `hostAbi()` and optional `setsid` are already macOS-aware).
- Copying the account from an older phone image (e.g. API 35 Google Play) on a new phone AVD to a Wear OS 7 watch.
- Physical phone + watch emulator for the account copy.

---
name: wear-emulator-pairing
description: >-
  Create, pair, inspect and repair a phone + Wear OS emulator pair from the command line with the `wear-pair`
  tool (built on adblib and the Android CLI). Use when asked to pair a watch and phone emulator, test
  phone/watch "better together" flows (Data Layer, login handoff, media), list which emulators are paired,
  fix a pairing that broke after an emulator restart, or explain why an AVD can't be paired.
---

# Wear OS emulator pairing

`wear-pair` pairs a phone emulator with a Wear OS emulator, the same way Android Studio's pairing assistant does:
the real companion app (Pixel Watch / Wear OS app) on a Google Play phone image, an adb bridge on port 5601, and the
companion's emulator pairing support.

## Build / run

```bash
./gradlew :sample:installDist          # in the wear-emulator-pairing repo
WP=sample/build/install/wear-pair/bin/wear-pair
```

You need the Android SDK (`adb`, `emulator`) and, ideally, the Android CLI (`android`), which is used to start
emulators, install APKs and read screens. The tool needs host network access to the adb server; in a sandbox,
run it unsandboxed.

## Workflow

1. **Look before acting.** Run `$WP avds` to see which AVDs and installed images can pair, why the others can't,
   and the exact fix commands. Run `$WP list` and `$WP paired` to see what's already running and paired.
2. **The fastest path to a paired pair:** `$WP create --companion-apk <apk-or-dir>` (or `--test-account`). This creates
   `Wear_Pair_Phone` and `Wear_Pair_Watch` from installed images, boots them headless, and pairs them. Add `--window`
   to see them.
3. **Pair emulators that are already running:** `$WP pair [phoneSerial watchSerial]`. Serials are auto-detected
   when exactly one phone and one watch are online.
4. **Check the result:** `$WP status` or `$WP paired`.
5. **After an emulator restart:** run `$WP bridge`, or leave `$WP keep` running so it re-bridges automatically.
6. **Taking a pair apart:**
   - `$WP disconnect` removes only the adb bridge; both devices still remember each other, so `bridge` reconnects them.
   - `$WP unpair` fully unpairs. It removes the bridge, then the phone-side pairing: the
     companion app's data (which signs the companion out). It then resets the watch's Google Play services data and
     reboots the watch, which also removes any accounts on the watch. `--keep-watch` skips the watch reset;
     `--wipe-watch` wipes the whole watch instead. Afterwards, `pair` works again straight away.

   - Pairing stops at the companion app's Terms of Service. `--accept-terms` accepts it with all optional
     boxes (diagnostics, location) unchecked. It's a legal agreement for the signed-in account, so **ask the owner
     first**.

## Physical devices

- Physical phone + watch emulator works (Studio supports it too), including phones reached over
  Wi-Fi adb or Device Streaming. Physical watches can't use this tool to pair: they pair over Bluetooth.
- `unpair` only removes the adb bridge on physical devices; `--force` also clears app data, which forgets every watch
  paired to that phone.

## Preparing ahead of time

Do these before a test session so that pairing later needs no one present:
- `$WP sign-in [phoneSerial]` signs the configured test account in on a phone. Run it on a paired watch to copy the
  phone's account to the watch (see below).
- `$WP install-companion [phoneSerial] --companion-apk <apk|dir>` (or `--companion-from`, `--test-account`,
  `--play`) installs the companion app without a watch. It uses the online watch's companion if there is one,
  otherwise `--package <id>`, defaulting to the Pixel Watch app.
- **Watches can't sign in on their own.** Wear OS copies the account from the paired phone ("Open on phone" on the
  watch, then "Continue as …" on the phone). `$WP sign-in <watchSerial>` automates this: it finds the paired phone,
  checks that the phone has the account, and drives both screens. What we've found on emulators:
  - **Use a recent signed watch image:** Wear OS 7 (`system-images;android-37.0;android-wear-signed;<abi>`, `arm64-v8a` on
    Apple silicon or `x86_64`, Google Play services 26.x) works. Older watch images (API 34 / API 36, Play services
    24.x/25.x) fail during the transfer. Pair it with a **Google Play** phone image (`user/release-keys`, e.g.
    `system-images;android-37.0;google_apis_playstore;<abi>`).
  - Both devices need a **screen lock**, or the copy fails silently. On emulators with no lock, the tool sets PIN `1234`
    (`locksettings set-pin`); the phone then asks "Verify it's you" for that PIN and the tool types it.
  - With a PIN set, a rebooted emulator stays **locked**, and Google Play services and the companion app don't run
    until it's unlocked (pairing then stalls at "isn't listening on 5601"). `pair`, `keep` and `sign-in` unlock
    emulators with PIN `1234` automatically; for anything else, unlock them first.
  - The tool unplugs the watch's emulated charger during the copy, because Wear OS's full-screen charging UI
    covers the flow, and restores it afterwards.
  - `GLSUser … Device key file not found` / `AppCertManager … end-group tag did not match` in logcat is noise; the copy
    succeeds on Wear OS 7 despite it.
  - The "Associate with the watch" card in the Pixel Watch app isn't supported for emulators (`WearCDM: CDM association not
    supported for watch emulator`); ignore it.
  - Most phone/watch flows (Data Layer, messages, the companion's own account) don't need an account on the watch. You need
    it for Play Store on the watch, Google sign-in / Credential Manager on the watch, and Google apps on the watch.

## Getting the companion app onto the phone

The watch decides which companion app it needs (usually `com.google.android.apps.wear.companion`, the Pixel
Watch app). The tool tries the sources you give, in this order:

- `--companion-apk <file|dir>`: a local APK, or a directory of split APKs. Most reliable.
- `--companion-from <serial>`: copies the installed app (all of its splits) from another device.
- `--test-account`: signs in the Google account from `~/.config/wear-pair/test-account.properties`
  (a single line, `testaccount=email:password`), then installs from Play Store.
- `--play`: opens sign-in and Play Store on the phone, and taps Install.

### When Google asks to verify a sign-in

Some accounts, especially ones with extra security on a brand-new emulator, get a **"Verify it's you"** screen.
The tool then:
- tells the person that they have **20 minutes**, and what they will need: a way to receive a code by email or
  text, or another signed-in device to approve on;
- lists the verification methods (with contact details masked) and asks which one to use;
- asks for the code in the terminal. This works even when the emulator has no window.

As the agent, **warn the person before starting `--test-account`/`--play`** that they may need to be present for
up to 20 minutes. If no one can answer, prefer `--companion-apk` or `--companion-from`.
Never put real personal account credentials in the config; use a dedicated test account.

## Reading failures

Every failure prints `Pairing failed: <reason>` followed by `Fixes:`. Run the first fix that applies. Common cases:

| Message | Meaning | Fix |
|---|---|---|
| `companion app ... is not installed` | No companion app on the phone | Use a companion source flag |
| `watch ... is still paired with a different phone` | Stale pairing on the watch | `$WP pair --wipe-watch` (wipes and cold-boots the watch, keeping its serial) |
| `Google Play services ... too old` | Old system image | Use the newer image the tool suggests |
| `Studio would hide it` / `PlayStore.enabled=no` | AVD config flag (Studio-only issue) | `$WP enable-playstore <avd>` with the emulator stopped |
| `bridged ... but did not pair` | Pairing didn't complete | Follow the printed manual steps on the phone, then `$WP status` |

`$WP doctor [phone watch]` runs all the live checks at once (Play Store, account, companion app, GMS Core
versions, existing pairing) and prints fixes.

## Library use (Kotlin)

```kotlin
AdbSessionHost().use { host ->
  AdbSession.create(host).use { session ->
    val pairing = WearPairing(session, PairingOptions(companionSources = listOf(CompanionSource.LocalApks(listOf(apk)))))
    val result = pairing.pair("emulator-5554", "emulator-5556") { println(it) }
    val report = PairDiscovery(session).discover() // which emulators are paired
  }
}
```

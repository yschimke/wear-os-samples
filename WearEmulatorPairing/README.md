# Wear OS emulator pairing

Create, pair, inspect and repair a phone + Wear OS emulator pair from the command line or from Kotlin, without
Android Studio. Built on [adblib](https://maven.google.com/web/index.html#com.android.tools.adblib:adblib)
(Android Studio's suspending ADB client) and the [Android CLI](https://developer.android.com/tools/agents).

The pairing flow is a port of Android Studio's Wear OS pairing assistant
([`tools/adt/idea/wear-pairing`](https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:wear-pairing/src/com/android/tools/idea/wearpairing/)):

1. Wait for both devices and Google Play services, and check versions. Also check the watch isn't already paired
   with another phone (optionally wiping it).
2. Make sure the watch's companion app (Pixel Watch or Wear OS app) is on the phone. It can come from a local APK,
   another device, or Play Store (optionally signing in a test Google account).
3. Bridge the devices: `adb -s <phone> forward tcp:N tcp:5601` and `adb -s <watch> reverse tcp:5601 tcp:N`, then send
   the `refresh-emulator-connection` broadcast.
4. Pair through the companion's `EmulatorActivity`, which reports `[EMULATOR_PAIRING:<STATE>]` in logcat. It falls
   back to manual pairing if needed.
5. Poll `get-pairing-status` until the watch is a connected peer.

Agents: see [skills/wear-emulator-pairing/SKILL.md](skills/wear-emulator-pairing/SKILL.md).

## CLI

```
./gradlew :sample:installDist
WP=sample/build/install/wear-pair/bin/wear-pair

$WP avds                      # which AVDs/images can pair, why not, how to fix
$WP create --companion-apk <apk|dir>   # new phone + watch AVDs, booted and paired
$WP pair [phone] [watch]      # pair running emulators (auto-detected)
$WP paired                    # which emulators are paired / bridged
$WP doctor                    # live checks with fixes
$WP bridge | keep | status | disconnect | unpair
$WP sign-in [serial] | install-companion [phone]   # prepare ahead of time
```

Companion sources (tried in order):
- `--companion-apk <file|dir>`
- `--companion-from <serial>`
- `--test-account`: signs in the Google account from `~/.config/wear-pair/test-account.properties`, see
  [config/test-account.properties.example](config/test-account.properties.example), then installs from Play Store.
- `--play`

If Google asks to verify a sign-in, the tool explains what's needed (for example, approve on your phone and tap the
number shown) and waits up to 20 minutes.

`sign-in <watch>` copies the paired phone's Google account to the watch. It needs a recent signed watch image (Wear OS 7,
`android-37.0;android-wear-signed`) and a Google Play phone image; older watch images fail the transfer. Emulators
without a screen lock get PIN `1234`, which the copy requires.

Other flags:
- `--phone-image <pkg>` / `--watch-image <pkg>`: choose specific installed images for `create` (or substrings, e.g. `android-35`).
- `--accept-terms`: accept the companion's Terms of Service with all optional boxes unchecked. Only use it with the
  account owner's consent.
- `--wipe-watch`
- `--window`

## Library

```kotlin
AdbSessionHost().use { host ->
  AdbSession.create(host).use { session ->
    val pairing = WearPairing(session, PairingOptions(), PairingLogger.Stderr)
    when (val result = pairing.pair("emulator-5554", "emulator-5556") { println(it) }) {
      is PairingResult.Paired -> println("paired via host port ${result.hostPort}")
      is PairingResult.Failure -> { println(result.message); result.fixes.forEach(::println) }
    }
  }
}
```

## Requirements

- Android SDK (`adb`, `emulator`, system images); Android CLI (`android`) recommended.
- A Google Play phone image (API 30+) and a Wear OS image (API 28+). Run `wear-pair avds` to check.
- JDK 17+.

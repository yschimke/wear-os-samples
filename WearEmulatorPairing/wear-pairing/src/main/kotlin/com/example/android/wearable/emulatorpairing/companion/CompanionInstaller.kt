/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.emulatorpairing.companion

import com.example.android.wearable.emulatorpairing.AdbDevice
import com.example.android.wearable.emulatorpairing.PairingLogger
import com.example.android.wearable.emulatorpairing.isCompanionAppInstalled
import com.example.android.wearable.emulatorpairing.tools.AndroidTools
import com.example.android.wearable.emulatorpairing.tools.AndroidUi
import com.example.android.wearable.emulatorpairing.tools.UiNode
import com.example.android.wearable.emulatorpairing.tools.findClickable
import com.example.android.wearable.emulatorpairing.tools.hasScreenText
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Ways to get the watch's companion app (Pixel Watch / OEM / Wear OS) onto the phone, tried in order. */
public sealed interface CompanionSource {
  /** One APK, several split APKs, or a directory of them (e.g. pulled from another device, or an `.apks`-extracted dir). */
  public data class LocalApks(val paths: List<Path>) : CompanionSource

  /** Copy the installed app (all splits) from another device or emulator that already has it. */
  public data class CopyFromDevice(val serial: String) : CompanionSource

  /** Sign in a Google test account by driving the sign-in screens (see [GoogleSignIn]), then install from Play Store. */
  public data class TestAccountThenPlayStore(
    val account: TestAccount,
    val autoTapInstall: Boolean = true,
    val timeout: Duration = 10.minutes,
  ) : CompanionSource

  /**
   * Open Play Store on the phone. If no Google account is signed in, opens the add-account screen and waits for the user to sign in.
   * With [autoTapInstall], taps "Install" using the Android CLI's `android layout` (or `uiautomator dump`).
   */
  public data class PlayStore(val autoTapInstall: Boolean = true, val timeout: Duration = 10.minutes) : CompanionSource
}

public fun CompanionSource.describe(): String =
  when (this) {
    is CompanionSource.LocalApks -> "local APKs ${paths.joinToString()}"
    is CompanionSource.CopyFromDevice -> "copy from $serial"
    is CompanionSource.TestAccountThenPlayStore -> "test account ${account.email} + Play Store"
    is CompanionSource.PlayStore -> "Play Store"
  }

public sealed interface CompanionInstallEvent {
  public data class Trying(val source: CompanionSource) : CompanionInstallEvent

  /** The user must act on the phone screen, e.g. sign in. */
  public data class UserActionRequired(val instructions: String) : CompanionInstallEvent

  public data class SourceFailed(val source: CompanionSource, val reason: String) : CompanionInstallEvent
}

internal sealed interface PlayStoreAction {
  data class Tap(val label: String, val x: Int, val y: Int, val reason: String) : PlayStoreAction

  data object RefreshSignedInPlayStore : PlayStoreAction

  data class InProgress(val detail: String) : PlayStoreAction

  data object Idle : PlayStoreAction
}

public class CompanionInstaller(
  private val tools: AndroidTools = AndroidTools(),
  private val logger: PairingLogger = PairingLogger.None,
  /** Used when Google asks the person to verify a test-account sign-in. */
  private val signInHelper: SignInHelper? = null,
) {
  /** Tries each source until [companionAppId] is installed on [phone]. Returns true on success. */
  public suspend fun install(
    phone: AdbDevice,
    companionAppId: String,
    sources: List<CompanionSource>,
    onEvent: (CompanionInstallEvent) -> Unit = {},
  ): Boolean {
    if (phone.isCompanionAppInstalled(companionAppId)) return true
    for (source in sources) {
      onEvent(CompanionInstallEvent.Trying(source))
      val failure =
        try {
          when (source) {
            is CompanionSource.LocalApks -> installApks(phone, expandApks(source.paths))
            is CompanionSource.CopyFromDevice -> copyFromDevice(phone, AdbDevice(phone.session, source.serial), companionAppId)
            is CompanionSource.TestAccountThenPlayStore ->
              GoogleSignIn(tools, logger).signIn(
                phone,
                source.account,
                signInHelper ?: SignInHelper.notifyOnly { onEvent(CompanionInstallEvent.UserActionRequired(it)) },
              )
                ?: installFromPlayStore(phone, companionAppId, CompanionSource.PlayStore(source.autoTapInstall, source.timeout), onEvent)
            is CompanionSource.PlayStore -> installFromPlayStore(phone, companionAppId, source, onEvent)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          e.message ?: e.toString()
        }
      if (failure == null && phone.isCompanionAppInstalled(companionAppId)) return true
      onEvent(CompanionInstallEvent.SourceFailed(source, failure ?: "$companionAppId still not installed"))
    }
    return false
  }

  /** Installs APKs, preferring the Android CLI (`android install`) and falling back to `adb install-multiple`. */
  public suspend fun installApks(phone: AdbDevice, apks: List<Path>): String? {
    if (apks.isEmpty()) return "No APKs found"
    val cli = tools.androidCli
    val result =
      if (cli != null) {
        tools.run(listOf(cli.toString(), "install", "--device=${phone.serialNumber}", "--apks=${apks.joinToString(",")}", "--install-options=-r,-d,-g"))
      } else {
        tools.run(listOf(tools.requireAdb(), "-s", phone.serialNumber, "install-multiple", "-r", "-d", "-g") + apks.map { it.toString() })
      }
    return if (result.ok) null else "Install failed: ${result.stderr.ifBlank { result.stdout }.trim()}"
  }

  /** `pm path` on the source device, pull every split, and install them all on [phone]. */
  public suspend fun copyFromDevice(phone: AdbDevice, source: AdbDevice, packageName: String): String? {
    val remotePaths = source.shell("pm path $packageName").lines().map { it.removePrefix("package:").trim() }.filter { it.endsWith(".apk") }
    if (remotePaths.isEmpty()) return "$packageName is not installed on ${source.serialNumber}"
    val dir = Files.createTempDirectory("companion-$packageName")
    for (remote in remotePaths) {
      val pulled = tools.run(tools.requireAdb(), "-s", source.serialNumber, "pull", remote, dir.resolve(remote.substringAfterLast('/')).toString())
      if (!pulled.ok) return "Failed to pull $remote: ${pulled.stderr.trim()}"
    }
    logger.info("Pulled ${remotePaths.size} APK(s) of $packageName from ${source.serialNumber} into $dir")
    return installApks(phone, expandApks(listOf(dir)))
  }

  private suspend fun installFromPlayStore(
    phone: AdbDevice,
    companionAppId: String,
    options: CompanionSource.PlayStore,
    onEvent: (CompanionInstallEvent) -> Unit,
  ): String? {
    if (phone.isEmulator() && phone.isLocked()) phone.unlockWithPin(EMULATOR_PIN)
    if (phone.shell("pm path com.android.vending").isBlank()) {
      return "Play Store isn't installed on ${phone.serialNumber}; use a Google Play system image (see `wear-pair avds`)"
    }
    // Play Store refuses to install below its low-storage threshold ("Free up space to install").
    val freeMb = phone.shell("df -m /data | tail -1").split(Regex("\\s+")).getOrNull(3)?.toLongOrNull()
    if (freeMb != null && freeMb < 1024) {
      val avd = phone.avdName()
      return "Only ${freeMb} MB free on ${phone.serialNumber}; Play Store needs more space to install. Uninstall unused apps " +
        "(adb -s ${phone.serialNumber} shell pm list packages -3), or grow the AVD's data partition: set disk.dataPartition.size=8G in " +
        "~/.android/avd/${avd ?: "<avd>"}.avd/config.ini and restart the emulator" + (avd?.let { " (android emulator stop ${phone.serialNumber}; android emulator start $it)" } ?: "")
    }
    val ui = AndroidUi(tools, phone)
    if (!hasGoogleAccount(phone)) {
      phone.shell("am start -a android.settings.ADD_ACCOUNT_SETTINGS --esa account_types com.google")
      onEvent(CompanionInstallEvent.UserActionRequired("Sign in to a Google account on ${phone.serialNumber} (the sign-in screen is open)"))
      val signedIn =
        withTimeoutOrNull(USER_VERIFICATION_WINDOW) {
          while (!hasGoogleAccount(phone)) {
            ui.nodes().firstOrNull { it.resourceId == "aerr_wait" }?.let { ui.tap(it) }
            delay(2_000)
          }
          true
        }
      if (signedIn != true) return "Timed out waiting for a Google account sign-in on ${phone.serialNumber}"
      logger.info("Google account detected on ${phone.serialNumber}")
      delay(2_000)
    }

    val result =
      withTimeoutOrNull(options.timeout) {
        openPlayStoreDetails(phone, companionAppId)
        delay(3_000)
        var prompted = false
        var signInRefreshes = 0
        var unhandledPolls = 0
        var reopenCount = 0
        while (!phone.isCompanionAppInstalled(companionAppId)) {
          val nodes = ui.nodes()
          when (val action = classifyPlayStoreScreen(nodes, options.autoTapInstall)) {
            is PlayStoreAction.Tap -> {
              logger.info("Play Store (${action.reason}): tapping '${action.label}' at ${action.x},${action.y}")
              ui.tap(action.x, action.y)
              unhandledPolls = 0
              delay(if (action.reason == "install") 6_000 else 3_000)
            }
            PlayStoreAction.RefreshSignedInPlayStore -> {
              if (signInRefreshes < 2) {
                signInRefreshes++
                logger.info("Play Store still shows Sign in after account setup; restarting Play Store")
                phone.shell("am force-stop com.android.vending")
                delay(1_000)
                openPlayStoreDetails(phone, companionAppId)
              } else {
                nodes.findClickable("Sign in")?.let { ui.tap(it) }
              }
              unhandledPolls = 0
              delay(4_000)
            }
            is PlayStoreAction.InProgress -> {
              logger.info("Play Store install in progress (${action.detail})")
              unhandledPolls = 0
              delay(4_000)
            }
            PlayStoreAction.Idle -> {
              unhandledPolls++
              val focus = phone.shell("dumpsys window | grep -m1 mCurrentFocus")
              val inPlayFlow =
                focus.isBlank() ||
                  focus.contains("com.android.vending") ||
                  focus.contains("packageinstaller") ||
                  focus.contains("permissioncontroller") ||
                  focus.contains("com.google.android.gms")
              if (!inPlayFlow && reopenCount < 5) {
                reopenCount++
                logger.info("Play Store left foreground ($focus); reopening details page")
                openPlayStoreDetails(phone, companionAppId)
                unhandledPolls = 0
                delay(4_000)
              } else {
                if (!prompted && (!options.autoTapInstall || unhandledPolls >= 4)) {
                  prompted = true
                  onEvent(CompanionInstallEvent.UserActionRequired("Tap Install for $companionAppId in Play Store on ${phone.serialNumber}"))
                }
                delay(3_000)
              }
            }
          }
        }
        true
      }
    return if (result == true) null else "Timed out after ${options.timeout} waiting for $companionAppId to install from Play Store"
  }

  private suspend fun openPlayStoreDetails(phone: AdbDevice, companionAppId: String) {
    phone.shell("am start -a android.intent.action.VIEW -d 'market://details?id=$companionAppId'")
  }

  /** Finds a clickable node whose text is one of [labels] and taps its centre. */
  internal suspend fun tapButton(phone: AdbDevice, labels: Set<String>): Boolean {
    val nodes = AndroidUi(tools, phone).nodes()
    val center = findInstallTarget(nodes, labels) ?: return false
    logger.info("Tapping '${center.first}' at ${center.second},${center.third}")
    phone.shell("input tap ${center.second} ${center.third}")
    return true
  }

  public companion object {
    private val PERCENT_REGEX = Regex("""^\d+%$""")

    public suspend fun hasGoogleAccount(device: AdbDevice): Boolean = device.shell("dumpsys account").contains("type=com.google}")

    internal fun findClickable(layoutJson: String, labels: Set<String>): Triple<String, Int, Int>? =
      findInstallTarget(AndroidUi.parseLayout(layoutJson), labels)

    internal fun findInstallTarget(nodes: List<UiNode>, labels: Set<String>): Triple<String, Int, Int>? {
      val direct = nodes.findClickable(*labels.toTypedArray())
      if (direct != null) return Triple(direct.label ?: labels.first(), direct.x, direct.y)
      // Fallback for older Play Store layouts where the Install button is an unlabeled clickable directly above "Install on phone.".
      val caption = nodes.firstOrNull { n -> labels.any { n.label?.trim()?.startsWith("$it on", ignoreCase = true) == true } } ?: return null
      val button = nodes.filter { it.clickable && it.label == null && it.y in (caption.y - 200) until caption.y }.maxByOrNull { it.y } ?: return null
      // The button row is split (Install | ▼); tap left of centre to hit Install rather than the dropdown.
      return Triple(caption.label!!, button.x - button.x / 10, button.y)
    }

    internal fun classifyPlayStoreScreen(nodes: List<UiNode>, autoTapInstall: Boolean): PlayStoreAction {
      if (nodes.isEmpty()) return PlayStoreAction.Idle

      // 1. System ANR dialog ("System UI / Google Play Store / Google Play services isn't responding").
      val anrWait = nodes.firstOrNull { it.resourceId == "aerr_wait" } ?: if (nodes.hasScreenText("isn't responding")) nodes.findClickable("Wait") else null
      if (anrWait != null) return PlayStoreAction.Tap(anrWait.label ?: "Wait", anrWait.x, anrWait.y, "anr-wait")

      // 2. Notification / runtime permission prompt over Play Store.
      val permDeny =
        nodes.firstOrNull { it.resourceId?.endsWith("permission_deny_button") == true }
          ?: if (nodes.hasScreenText("notifications")) nodes.findClickable("Don't allow", "Not now", "No thanks") else null
      if (permDeny != null) return PlayStoreAction.Tap(permDeny.label ?: "Don't allow", permDeny.x, permDeny.y, "permission-dismiss")

      // 3. "Complete account setup" / optional payment-method prompt on first Play Store install ("Continue" -> "Skip").
      if (nodes.hasScreenText("Complete account setup") || nodes.hasScreenText("payment method")) {
        nodes.findClickable("Skip", "SKIP")?.let { return PlayStoreAction.Tap(it.label ?: "Skip", it.x, it.y, "skip-payment") }
        if (nodes.hasScreenText("Complete account setup")) {
          nodes.findClickable("Continue", "CONTINUE")?.let { return PlayStoreAction.Tap(it.label ?: "Continue", it.x, it.y, "continue-setup") }
        }
      }

      // 4. Play Store Terms of Service / onboarding sheets (never "Cancel" or "Close").
      nodes.findClickable("Accept", "I agree", "Got it", "No thanks", "Not now", "Dismiss")?.let {
        return PlayStoreAction.Tap(it.label ?: "Accept", it.x, it.y, "interstitial")
      }

      // 5. Active download / installation: never tap while "Cancel" / progress % / "Pending" is displayed.
      val percent = nodes.firstNotNullOfOrNull { n -> n.label?.trim()?.takeIf { PERCENT_REGEX.matches(it) } }
      val progressWord = listOf("Pending", "Downloading", "Installing", "Waiting for download").firstOrNull { nodes.hasScreenText(it) }
      val hasCancel = nodes.any { it.labelIs("Cancel") } && (nodes.hasScreenText("Verified by Play Protect") || percent != null || progressWord != null)
      if (percent != null || progressWord != null || hasCancel) {
        return PlayStoreAction.InProgress(percent ?: progressWord ?: "installing")
      }

      // 6. Multi-device picker sheet ("This device" / phone checkbox unchecked before "Install" enables).
      if (autoTapInstall) {
        val uncheckedPhone =
          nodes.firstOrNull { n ->
            n.checkable &&
              !n.checked &&
              n.clickable &&
              n.label?.let {
                (it.contains("This device", ignoreCase = true) || it.contains("phone", ignoreCase = true) || it.contains("gphone", ignoreCase = true)) &&
                  !it.contains("watch", ignoreCase = true) &&
                  !it.contains("wear", ignoreCase = true)
              } == true
          }
        if (uncheckedPhone != null) {
          return PlayStoreAction.Tap(uncheckedPhone.label ?: "This device", uncheckedPhone.x, uncheckedPhone.y, "select-phone")
        }

        findInstallTarget(nodes, setOf("Install", "Update"))?.let { (label, x, y) ->
          return PlayStoreAction.Tap(label, x, y, "install")
        }

        // Account already installed the app on another device and Play Store only shows "Install on more devices".
        if (!nodes.any { it.labelIs("Open", "Uninstall") }) {
          nodes.findClickable("Install on more devices", "Install on other devices")?.let {
            return PlayStoreAction.Tap(it.label ?: "Install on more devices", it.x, it.y, "more-devices")
          }
        }
      }

      // 7. Play Store opened before it noticed the newly added Google account.
      if (nodes.findClickable("Sign in") != null) return PlayStoreAction.RefreshSignedInPlayStore

      return PlayStoreAction.Idle
    }

    internal fun expandApks(paths: List<Path>): List<Path> =
      paths.flatMap { p -> if (p.isDirectory()) p.listDirectoryEntries("*.apk").sortedBy { if (it.name == "base.apk") "" else it.name } else listOf(p) }
        .filter { it.extension == "apk" }
  }
}

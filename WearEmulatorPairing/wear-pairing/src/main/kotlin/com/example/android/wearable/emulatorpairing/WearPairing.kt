/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.emulatorpairing

import com.android.adblib.AdbSession
import com.android.adblib.DeviceState
import com.android.adblib.SocketSpec
import com.example.android.wearable.emulatorpairing.avd.AvdCatalog
import com.example.android.wearable.emulatorpairing.companion.CompanionInstallEvent
import com.example.android.wearable.emulatorpairing.companion.CompanionInstaller
import com.example.android.wearable.emulatorpairing.companion.CompanionSource
import com.example.android.wearable.emulatorpairing.companion.describe
import com.example.android.wearable.emulatorpairing.tools.AndroidTools
import com.example.android.wearable.emulatorpairing.tools.EmulatorControl
import java.io.IOException
import java.net.ServerSocket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** The port GMS Core's Wearable emulator transport listens on, on both phone and watch. */
public const val WEARABLE_EMULATOR_PORT: Int = 5601

public data class PairingOptions(
  /** How long to wait for each device to be online and finish booting. */
  val bootTimeout: Duration = 5.minutes,
  /** How long to wait for GMS Core on each device to report its node id / pairing status after boot. */
  val gmsCoreReadyTimeout: Duration = 50.seconds,
  /** Where to get the companion app from if it's missing on the phone, tried in order. Empty = fail with instructions. */
  val companionSources: List<CompanionSource> = emptyList(),
  /** If the watch is still paired with another phone, wipe the watch emulator's data and cold-boot it (same serial). */
  val wipeWatchIfPairedElsewhere: Boolean = false,
  /** Restart wiped emulators without a window. */
  val headless: Boolean = true,
  /** How long to wait for the companion app's `EmulatorActivity` to finish non-interactive pairing. */
  val nonInteractivePairingTimeout: Duration = 1.minutes,
  /** Extra attempts at non-interactive pairing before falling back to manual pairing. */
  val nonInteractiveRetries: Int = 1,
  /** How long to wait for the user to finish pairing manually in the companion app. */
  val manualPairingTimeout: Duration = 3.minutes,
  /** First host port to try for the phone `adb forward` (Studio uses 5602). */
  val hostPortStart: Int = 5602,
  /**
   * Accept the companion app's Terms of Service screen during pairing, with every optional checkbox (usage/diagnostics,
   * location, ...) unchecked. Off by default: it is a legal agreement on behalf of the signed-in account, so only enable it
   * with the account owner's consent (e.g. for a dedicated test account).
   */
  val acceptCompanionTerms: Boolean = false,
  /** Talks to the person if a test-account sign-in needs verification (see [com.example.android.wearable.emulatorpairing.companion.GoogleSignIn]). */
  val signInHelper: com.example.android.wearable.emulatorpairing.companion.SignInHelper? = null,
  /** Check that the watch reports `android.hardware.type.watch` and the phone doesn't. */
  val verifyDeviceTypes: Boolean = true,
)

public sealed interface PairingProgress {
  public data class WaitingForDevice(val serial: String) : PairingProgress

  public data object WaitingForGmsCore : PairingProgress

  public data object CheckingCompatibility : PairingProgress

  public data class Companion(val event: CompanionInstallEvent) : PairingProgress

  /** GMS Core on the phone didn't report a pairing status; nudging it with `com.google.android.gms.INITIALIZE`. */
  public data class RepairingGmsCore(val serial: String) : PairingProgress

  /** The watch was paired with another phone and is being wiped + cold-booted. */
  public data class WipingWatch(val serial: String) : PairingProgress

  public data class Bridged(val hostPort: Int) : PairingProgress

  /** Non-interactive pairing state from the companion app; [CompanionPairingState.CONSENT] means "accept on the phone". */
  public data class CompanionState(val state: CompanionPairingState, val attempt: Int) : PairingProgress

  /** The user must complete pairing in the companion app on the phone. */
  public data class ManualPairingRequired(val companionAppId: String, val instructions: List<String>) : PairingProgress
}

public sealed interface PairingResult {
  public data class Paired(
    val hostPort: Int,
    val companionAppId: String,
    /** Older Wear OS companions need the user to tap "Skip and finish" in the phone app after manual pairing. */
    val needsSkipAndFinish: Boolean,
  ) : PairingResult

  public sealed interface Failure : PairingResult {
    public val message: String
    /** Concrete next steps for the user. */
    public val fixes: List<String>
  }

  public data class DeviceNotReady(val serial: String) : Failure {
    override val message: String = "$serial isn't responding (not booted, system services restarting, or the host is overloaded)"
    override val fixes: List<String> =
      listOf("Check `adb devices` and `adb -s $serial shell getprop sys.boot_completed`", "Free host CPU/RAM (other emulators, build daemons), then retry")
  }

  public data class WrongDeviceType(val serial: String, val expectedWatch: Boolean, override val fixes: List<String>) : Failure {
    override val message: String = "$serial is ${if (expectedWatch) "not a watch" else "a watch, expected a phone"}"
  }

  public data class GmsCoreTooOld(val serial: String, val feature: PairingFeature, val installed: Int?, override val fixes: List<String>) :
    Failure {
    override val message: String =
      "Google Play services on $serial is too old for $feature (has ${installed ?: "none"}, needs >= ${feature.minVersion})"
  }

  public data class WatchNeedsFactoryReset(val serial: String, override val fixes: List<String>) : Failure {
    override val message: String = "The watch $serial is still paired with a different phone"
  }

  public data class CompanionAppUnavailable(
    val companionAppId: String,
    val support: CompanionSupport,
    val attempts: List<String>,
    override val fixes: List<String>,
  ) : Failure {
    override val message: String =
      when (support) {
        CompanionSupport.SUPPORTED -> "The companion app $companionAppId (needed by the watch) is not installed on the phone"
        CompanionSupport.INCOMPATIBLE_ABI -> "$companionAppId does not support this phone's ABI (x86_64 only)"
        CompanionSupport.INCOMPATIBLE_COMPANION_ID -> "The watch requires companion $companionAppId, which isn't supported for emulator pairing"
      } + attempts.joinToString("") { "\n  tried: $it" }
  }

  public data class NotPaired(val hostPort: Int, val lastCompanionState: CompanionPairingState?, override val fixes: List<String>) : Failure {
    override val message: String = "Devices are bridged on host port $hostPort but did not pair (companion state: $lastCompanionState)"
  }
}

/**
 * Pairs a phone and Wear OS emulator over adb, following the same flow as Android Studio's Wear OS pairing assistant
 * (`DevicesConnectionStep` + `WearPairingManager`), plus some self-repair:
 * 1. wait for both devices + GMS Core,
 * 2. check GMS Core versions and that the watch isn't paired elsewhere (optionally wiping the watch),
 * 3. make sure the companion app is on the phone (optionally installing it from [PairingOptions.companionSources]),
 * 4. bridge: `adb -s phone forward tcp:N tcp:5601` and `adb -s watch reverse tcp:5601 tcp:N`, then refresh the watch connection,
 * 5. pair via the companion's `EmulatorActivity` (non-interactive, with retries) or fall back to manual pairing,
 * 6. poll the pairing status until connected.
 */
public class WearPairing(
  public val session: AdbSession,
  private val options: PairingOptions = PairingOptions(),
  private val logger: PairingLogger = PairingLogger.None,
  private val tools: AndroidTools = AndroidTools(),
) {
  private val catalog = AvdCatalog(tools.locations)

  public fun device(serial: String): AdbDevice = AdbDevice(session, serial)

  public suspend fun pair(phoneSerial: String, watchSerial: String, onProgress: (PairingProgress) -> Unit = {}): PairingResult =
    pair(device(phoneSerial), device(watchSerial), onProgress)

  public suspend fun pair(phone: AdbDevice, watch: AdbDevice, onProgress: (PairingProgress) -> Unit = {}): PairingResult =
    pair(phone, watch, onProgress, allowWipe = options.wipeWatchIfPairedElsewhere)

  private suspend fun pair(phone: AdbDevice, watch: AdbDevice, onProgress: (PairingProgress) -> Unit, allowWipe: Boolean): PairingResult {
    // Phase 0: devices online
    for (device in listOf(phone, watch)) {
      onProgress(PairingProgress.WaitingForDevice(device.serialNumber))
      if (!waitForBoot(device)) return PairingResult.DeviceNotReady(device.serialNumber)
    }
    if (options.verifyDeviceTypes) {
      if (!watch.isWatch()) return PairingResult.WrongDeviceType(watch.serialNumber, true, listOf("Pass the Wear OS emulator as the watch"))
      if (phone.isWatch()) return PairingResult.WrongDeviceType(phone.serialNumber, false, listOf("Pass a phone emulator as the phone"))
    }

    onProgress(PairingProgress.WaitingForGmsCore)
    val companionAppId = watch.getCompanionAppIdForWatch(logger)
    logger.info("Watch companion app: $companionAppId")
    if (phone.isCompanionAppInstalled(companionAppId)) phone.waitForGmsCore()
    watch.waitForGmsCore()

    // Phase 1: compatibility
    onProgress(PairingProgress.CheckingCompatibility)
    gmsTooOld(phone, PairingFeature.MULTI_WATCH_SINGLE_PHONE_PAIRING, isWatch = false)?.let { return it }
    gmsTooOld(watch, PairingFeature.REVERSE_PORT_FORWARD, isWatch = true)?.let { return it }

    if (checkWearMayNeedFactoryReset(phone, watch)) {
      val wipeFixes =
        listOf(
          "wear-pair pair --wipe-watch   (wipes ${watch.serialNumber}'s data and cold-boots it)",
          "or: android emulator stop ${watch.serialNumber}; emulator -avd ${watch.avdName() ?: "<watch avd>"} -wipe-data",
        )
      if (!allowWipe || !watch.isEmulator()) return PairingResult.WatchNeedsFactoryReset(watch.serialNumber, wipeFixes)
      onProgress(PairingProgress.WipingWatch(watch.serialNumber))
      val wiped = EmulatorControl(session, tools, logger).wipeAndRestart(watch, headless = options.headless)
        ?: return PairingResult.WatchNeedsFactoryReset(watch.serialNumber, listOf("Wiping failed; check the emulator log") + wipeFixes)
      return pair(phone, wiped, onProgress, allowWipe = false)
    }

    // Studio removes any previous bridge for this watch first (without restarting GMS Core).
    removeBridge(phone, watch, restartWatchGmsCore = false)

    if (!phone.isCompanionAppInstalled(companionAppId)) {
      val support = phone.supportsCompanionAppId(companionAppId)
      if (support != CompanionSupport.SUPPORTED) {
        return PairingResult.CompanionAppUnavailable(companionAppId, support, emptyList(), companionFixes(phone, companionAppId, support))
      }
      val attempts = mutableListOf<String>()
      val installed =
        options.companionSources.isNotEmpty() &&
          CompanionInstaller(tools, logger, options.signInHelper).install(phone, companionAppId, options.companionSources) { event ->
            if (event is CompanionInstallEvent.SourceFailed) attempts += "${event.source.describe()}: ${event.reason}"
            onProgress(PairingProgress.Companion(event))
          }
      if (!installed) {
        return PairingResult.CompanionAppUnavailable(companionAppId, support, attempts, companionFixes(phone, companionAppId, support))
      }
      // A freshly installed companion: GMS Core on the phone often hasn't initialised the Wearable service yet.
      ensurePhoneGmsReady(phone, onProgress)
    } else if (phone.hasPairingFeature(PairingFeature.GET_PAIRING_STATUS) && !phone.isPairingStatusAvailable()) {
      ensurePhoneGmsReady(phone, onProgress)
    }

    // Phase 2: bridge + pair
    val hostPort = bridge(phone, watch, companionAppId)
    onProgress(PairingProgress.Bridged(hostPort))

    var lastCompanionState: CompanionPairingState? = null
    if (!waitForPaired(phone, watch, 5.seconds)) {
      var paired = false
      if (phone.hasPairingFeature(PairingFeature.COMPANION_EMULATOR_ACTIVITY, companionAppId)) {
        val watchName = sanitizeName(watch.avdName() ?: watch.getProperty("ro.product.model") ?: watch.serialNumber)
        for (attempt in 1..(1 + options.nonInteractiveRetries)) {
          logger.info("Non-interactive pairing attempt $attempt via $companionAppId/.EmulatorActivity (watch '$watchName')")
          if (attempt > 1) watch.refreshEmulatorConnection(logger)
          kotlinx.coroutines.coroutineScope {
            val screens = launch { tendCompanionScreens(phone) }
            withTimeoutOrNull(options.nonInteractivePairingTimeout) {
              phone.companionEmulatorPairing(companionAppId, watchName, watch.loadNodeID()).collect {
                lastCompanionState = it
                onProgress(PairingProgress.CompanionState(it, attempt))
              }
            }
            screens.cancel()
          }
          if (waitForPaired(phone, watch, 10.seconds)) {
            paired = true
            break
          }
          if (lastCompanionState == CompanionPairingState.CANCELLED) break // The user cancelled; don't loop.
        }
      }
      if (!paired) {
        // Manual fallback, like Studio's "Skip to manual instructions".
        onProgress(PairingProgress.ManualPairingRequired(companionAppId, manualInstructions(companionAppId, watch)))
        // monkey avoids having to know each companion's launcher activity.
        phone.shell("monkey -p $companionAppId -c android.intent.category.LAUNCHER 1")
        val manualPaired = kotlinx.coroutines.coroutineScope {
          val screens = launch { tendCompanionScreens(phone) }
          waitForPaired(phone, watch, options.manualPairingTimeout, pollMillis = 2_000).also { screens.cancel() }
        }
        if (!manualPaired) {
          return PairingResult.NotPaired(
            hostPort,
            lastCompanionState,
            listOf(
              "Finish pairing in the companion app on ${phone.serialNumber}, then run `wear-pair status`",
              "Retry with `wear-pair unpair` then `wear-pair pair`",
              "If the watch shows it's paired with another phone, use `wear-pair pair --wipe-watch`",
            ),
          )
        }
      }
    }

    val needsSkipAndFinish = !phone.hasPairingFeature(PairingFeature.COMPANION_SKIP_AND_FINISH_FIXED, companionAppId)
    return PairingResult.Paired(hostPort, companionAppId, needsSkipAndFinish)
  }

  /**
   * Runs alongside companion pairing: dismisses "isn't responding" dialogs (Wait) on slow emulators and, with
   * [PairingOptions.acceptCompanionTerms], accepts the Terms of Service screen with all optional checkboxes unchecked.
   */
  private suspend fun tendCompanionScreens(phone: AdbDevice) {
    val ui = com.example.android.wearable.emulatorpairing.tools.AndroidUi(tools, phone)
    if (!ui.available) return
    while (true) {
      val nodes = ui.nodes()
      val wait = nodes.firstOrNull { it.resourceId == "aerr_wait" }
      val accept = nodes.firstOrNull { it.clickable && it.resourceId?.endsWith("terms_of_service_accept_button") == true }
      val checked = nodes.filter { it.resourceId == "checkbox" && "checkable" in it.interactions && "checked" in it.state }
      when {
        wait != null -> {
          logger.info("Dismissing an app-not-responding dialog (Wait)")
          ui.tap(wait)
        }
        accept != null && options.acceptCompanionTerms && checked.isNotEmpty() -> {
          logger.info("Unchecking optional: ${checked.mapNotNull { it.contentDesc }.joinToString()}")
          checked.forEach { ui.tap(it) }
        }
        accept != null && options.acceptCompanionTerms -> {
          // "More" scrolls the terms; the same button becomes "Accept" at the end.
          logger.info("Companion terms: tapping ${accept.label}")
          ui.tap(accept)
        }
      }
      kotlinx.coroutines.delay(2_500)
    }
  }

  private suspend fun ensurePhoneGmsReady(phone: AdbDevice, onProgress: (PairingProgress) -> Unit) {
    if (phone.isPairingStatusAvailable()) return
    onProgress(PairingProgress.RepairingGmsCore(phone.serialNumber))
    waitForCondition(options.gmsCoreReadyTimeout.inWholeMilliseconds, pollMillis = 2_000) {
      phone.shell("am broadcast -a $GMSCORE_APP_ID.INITIALIZE")
      phone.isPairingStatusAvailable() || phone.loadNodeID().isNotEmpty()
    }
  }

  private suspend fun gmsTooOld(device: AdbDevice, feature: PairingFeature, isWatch: Boolean): PairingResult.Failure? {
    if (device.hasPairingFeature(feature)) return null
    // No version at all usually means the device isn't answering (system services restarting, overloaded host), not an old GMS.
    var installed = device.getAppVersionCode(GMSCORE_APP_ID)
    if (installed == null) {
      waitForCondition(60_000, pollMillis = 5_000) { device.getAppVersionCode(GMSCORE_APP_ID) != null }
      if (device.hasPairingFeature(feature)) return null
      installed = device.getAppVersionCode(GMSCORE_APP_ID)
        ?: return PairingResult.DeviceNotReady(device.serialNumber)
    }
    val fixes = mutableListOf<String>()
    val newer = if (isWatch) catalog.suitableWatchImages() else catalog.suitablePhoneImages()
    newer.firstOrNull()?.let {
      fixes += "Create a newer ${if (isWatch) "watch" else "phone"} AVD from the installed image ${it.packageId}: " +
        "avdmanager create avd -n Wear_Pair_${if (isWatch) "Watch" else "Phone"} -k \"${it.packageId}\""
    }
    fixes += if (isWatch) catalog.recommendWatchImageFix() else catalog.recommendPhoneImageFix()
    if (device.shell("pm path com.android.vending").isNotBlank()) {
      device.shell("am start -a android.intent.action.VIEW -d 'market://details?id=$GMSCORE_APP_ID'")
      fixes += "Or update Google Play services from Play Store on ${device.serialNumber} (opened; needs a signed-in account)"
    }
    return PairingResult.GmsCoreTooOld(device.serialNumber, feature, installed, fixes)
  }

  private suspend fun companionFixes(phone: AdbDevice, companionAppId: String, support: CompanionSupport): List<String> =
    when (support) {
      CompanionSupport.INCOMPATIBLE_ABI -> listOf("Use an x86_64 phone image with ARM translation (API 30+), e.g. ${catalog.recommendPhoneImageFix()}")
      CompanionSupport.INCOMPATIBLE_COMPANION_ID -> listOf("Use a Wear OS image whose companion is the Pixel Watch or Wear OS app")
      CompanionSupport.SUPPORTED -> buildList {
        add("wear-pair pair --companion-apk <file-or-dir>   (a local APK, or a directory of split APKs)")
        add("wear-pair pair --companion-from <serial>   (copy it from a device that already has $companionAppId)")
        if (phone.shell("pm path com.android.vending").isBlank()) {
          add("This phone has no Play Store, so it can't install from Play: ${catalog.recommendPhoneImageFix()}")
        } else {
          add("wear-pair pair --play   (opens sign-in if needed, then Play Store; taps Install via the Android CLI)")
          add("wear-pair pair --test-account   (signs in the Google test account from ~/.config/wear-pair/test-account.properties, then installs from Play)")
        }
      }
    }

  private suspend fun manualInstructions(companionAppId: String, watch: AdbDevice): List<String> {
    val name = if (companionAppId == PIXEL_COMPANION_APP_ID) "Pixel Watch" else "Wear OS"
    return listOf(
      "Open the $name app on the phone (launched now) and sign in if asked",
      "Choose to pair a new watch; select '${watch.avdName() ?: watch.serialNumber}' when it appears",
      "Accept the pairing code on both screens and follow the setup steps",
    )
  }

  /**
   * Creates the adb bridge between already-paired devices (e.g. after an emulator restart). Returns the host port used.
   * Equivalent of `WearPairingManager.createPairedDeviceBridge`.
   */
  public suspend fun bridge(phone: AdbDevice, watch: AdbDevice, companionAppId: String? = null): Int {
    val hostPort = findFreePort(options.hostPortStart)
    logger.info("Bridging ${phone.serialNumber} <-> ${watch.serialNumber} via host port $hostPort")
    phone.createForward(hostPort, WEARABLE_EMULATOR_PORT)
    watch.createReverse(WEARABLE_EMULATOR_PORT, hostPort)
    watch.refreshEmulatorConnection(logger)
    if ((companionAppId ?: watch.getCompanionAppIdForWatch(logger)) == PIXEL_COMPANION_APP_ID) {
      // BLUETOOTH_CONNECT is required for the Pixel companion to pair; `pm clear` removes it.
      phone.grantPermission(PIXEL_COMPANION_APP_ID, "android.permission.BLUETOOTH_CONNECT")
    }
    return hostPort
  }

  public suspend fun isPaired(phone: AdbDevice, watch: AdbDevice): Boolean = checkDevicesPaired(phone, watch)

  /** Removes the adb bridge only. Both devices still remember each other; `bridge()` reconnects them. */
  public suspend fun disconnect(phone: AdbDevice, watch: AdbDevice) {
    removeBridge(phone, watch, restartWatchGmsCore = true)
  }

  /**
   * Fully unpairs: removes the bridge, makes the phone forget the watch, and (with [resetWatch]) makes the watch forget the phone.
   * - Phone: clears the companion app's data (it would otherwise reconnect via cloud sync). This signs the companion
   *   app out too.
   * - Watch: clears Google Play services data (`pm clear com.google.android.gms`), giving it a new node id and no peers. This also
   *   removes any accounts on the watch. Without it the watch keeps its old pairing, and pairing with a different phone later needs
   *   `--wipe-watch`.
   * Returns a description of each step taken.
   */
  public suspend fun unpair(phone: AdbDevice, watch: AdbDevice, resetWatch: Boolean = true, force: Boolean = false): List<String> {
    val steps = mutableListOf<String>()
    // On someone's own devices, clearing the companion app (all their watches) or the watch's Play services is destructive.
    if (!force && (!phone.isEmulator() || !watch.isEmulator())) {
      removeBridge(phone, watch, restartWatchGmsCore = true)
      return listOf(
        "Removed the adb bridge only: ${listOf(phone, watch).filterNot { it.isEmulator() }.joinToString { it.serialNumber }} is a physical " +
          "device, so app data wasn't cleared (that would forget every watch paired to it). Pass --force to clear it anyway."
      )
    }
    val companionAppId = watch.getCompanionAppIdForWatch(logger)
    removeBridge(phone, watch, restartWatchGmsCore = false)
    steps += "Removed the adb bridge"

    listOf(companionAppId, PIXEL_COMPANION_APP_ID, OEM_COMPANION_FALLBACK_APP_ID).distinct().forEach { app ->
      if (phone.isCompanionAppInstalled(app)) {
        phone.shell("pm clear $app")
        steps += "Cleared $app data on ${phone.serialNumber}"
      }
    }

    if (resetWatch) {
      watch.shell("pm clear $GMSCORE_APP_ID")
      // After clearing Google Play services the watch's emulator transport doesn't accept a new phone until it reboots.
      tools.run(tools.requireAdb(), "-s", watch.serialNumber, "reboot", timeoutSeconds = 60)
      kotlinx.coroutines.delay(5_000)
      waitForCondition(options.bootTimeout.inWholeMilliseconds, pollMillis = 3_000) { watch.isBootCompleted() }
      waitForCondition(60_000, pollMillis = 2_000) { watch.loadNodeID().isNotEmpty() }
      steps += "Reset Google Play services on ${watch.serialNumber} and rebooted it (new node id, no peers)"
    } else {
      watch.refreshEmulatorConnection(logger)
      steps += "Kept the watch's pairing state (pair it with a different phone later using --wipe-watch)"
    }
    return steps
  }

  /**
   * Re-creates the bridge whenever both devices come back online, like `WearPairingManager.updateForwardState`. Runs until cancelled.
   * [onBridged] receives the pairing state after each re-bridge.
   */
  public suspend fun keepBridged(phone: AdbDevice, watch: AdbDevice, onBridged: (paired: Boolean) -> Unit = {}) {
    var bothOnline = false
    session.hostServices.trackDevices().collect { devices ->
      val online = devices.filter { it.deviceState == DeviceState.ONLINE }.map { it.serialNumber }.toSet()
      val nowOnline = phone.serialNumber in online && watch.serialNumber in online
      if (nowOnline && !bothOnline) {
        try {
          if (waitForBoot(phone) && waitForBoot(watch)) {
            watch.waitForGmsCore()
            bridge(phone, watch)
            onBridged(waitForPaired(phone, watch, 30.seconds))
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          logger.warn("Re-bridge failed: $e")
        }
      } else if (!nowOnline && bothOnline) {
        logger.info("Pair went offline")
      }
      bothOnline = nowOnline
    }
  }

  private suspend fun removeBridge(phone: AdbDevice, watch: AdbDevice, restartWatchGmsCore: Boolean) {
    ignoringErrors {
      session.hostServices
        .listForward()
        .filter { it.deviceSerial == phone.serialNumber && it.remote.toQueryString() == SocketSpec.Tcp(WEARABLE_EMULATOR_PORT).toQueryString() }
        .forEach { session.hostServices.killForward(phone.selector, it.local) }
    }
    ignoringErrors { phone.removeForward(WEARABLE_EMULATOR_PORT) } // Manual `adb forward tcp:5601 tcp:5601` setups
    ignoringErrors { watch.removeReverse(WEARABLE_EMULATOR_PORT) }
    if (restartWatchGmsCore) watch.refreshEmulatorConnection(logger)
  }

  private suspend fun waitForBoot(device: AdbDevice): Boolean =
    waitForCondition(options.bootTimeout.inWholeMilliseconds, pollMillis = 2_000) { device.isBootCompleted() }

  private suspend fun AdbDevice.waitForGmsCore() {
    val timeout = options.gmsCoreReadyTimeout.inWholeMilliseconds
    if (hasPairingFeature(PairingFeature.GET_PAIRING_STATUS)) {
      waitForCondition(timeout) { isPairingStatusAvailable() }
    } else {
      // Don't wait too long; empty ids may just mean it was never paired.
      waitForCondition(timeout) { loadNodeID().isNotEmpty() }
      waitForCondition(10_000) { loadCloudNetworkID(ignoreNullOutput = false).isNotEmpty() }
    }
  }

  private suspend fun waitForPaired(phone: AdbDevice, watch: AdbDevice, timeout: Duration, pollMillis: Long = 1_000): Boolean =
    waitForCondition(timeout.inWholeMilliseconds, pollMillis) { checkDevicesPaired(phone, watch) }

  private suspend fun AdbDevice.openPlayStore(appId: String) {
    shell("am start -a android.intent.action.VIEW -d 'market://details?id=$appId'")
  }

  private suspend inline fun ignoringErrors(block: () -> Unit) {
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.log(PairingLogger.Level.DEBUG, "Ignored: $e")
    }
  }
}

internal fun sanitizeName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_.\\- ]"), "_").take(128).ifEmpty { "Wear OS emulator" }

internal fun findFreePort(start: Int): Int {
  for (port in start until start + 100) {
    try {
      ServerSocket(port).use { return port }
    } catch (_: IOException) {}
  }
  return ServerSocket(0).use { it.localPort }
}

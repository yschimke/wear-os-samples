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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.withTimeoutOrNull

/** Ways to get the watch's companion app (Pixel Watch / OEM / Wear OS) onto the phone, tried in order. */
public sealed interface CompanionSource {
  /** One APK, several split APKs, or a directory of them (e.g. pulled from another device, or an `.apks`-extracted dir). */
  public data class LocalApks(val paths: List<Path>) : CompanionSource

  /** Copy the installed app (all splits) from another device or emulator that already has it. */
  public data class CopyFromDevice(val serial: String) : CompanionSource

  /** Sign in a Google test account by driving the sign-in screens (see [GoogleSignIn]), then install from Play Store. */
  public data class TestAccountThenPlayStore(val account: TestAccount, val autoTapInstall: Boolean = true) : CompanionSource

  /**
   * Open Play Store on the phone. If no Google account is signed in, opens the add-account screen and waits for the user to sign in.
   * With [autoTapInstall], taps "Install" using the Android CLI's `android layout`.
   */
  public data class PlayStore(val autoTapInstall: Boolean = true, val timeout: Duration = 5.minutes) : CompanionSource
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
                ?: installFromPlayStore(phone, companionAppId, CompanionSource.PlayStore(source.autoTapInstall), onEvent)
            is CompanionSource.PlayStore -> installFromPlayStore(phone, companionAppId, source, onEvent)
          }
        } catch (e: kotlinx.coroutines.CancellationException) {
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
    val result =
      withTimeoutOrNull(options.timeout) {
        if (!hasGoogleAccount(phone)) {
          phone.shell("am start -a android.settings.ADD_ACCOUNT_SETTINGS --esa account_types com.google")
          onEvent(CompanionInstallEvent.UserActionRequired("Sign in to a Google account on ${phone.serialNumber} (the sign-in screen is open)"))
          while (!hasGoogleAccount(phone)) delay(2_000)
          logger.info("Google account detected on ${phone.serialNumber}")
        }
        phone.shell("am start -a android.intent.action.VIEW -d 'market://details?id=$companionAppId'")
        var prompted = false
        while (!phone.isCompanionAppInstalled(companionAppId)) {
          val tapped = options.autoTapInstall && tapButton(phone, setOf("Install", "Update"))
          if (!tapped && !prompted) {
            prompted = true
            onEvent(CompanionInstallEvent.UserActionRequired("Tap Install for $companionAppId in Play Store on ${phone.serialNumber}"))
          }
          delay(if (tapped) 10_000 else 3_000)
        }
        true
      }
    return if (result == true) null else "Timed out after ${options.timeout} waiting for $companionAppId to install from Play Store"
  }

  /** Finds a clickable node whose text is one of [labels] via `android layout` and taps its centre. */
  internal suspend fun tapButton(phone: AdbDevice, labels: Set<String>): Boolean {
    val cli = tools.androidCli ?: return false
    val layout = tools.run(cli.toString(), "layout", "--device=${phone.serialNumber}", timeoutSeconds = 60)
    if (!layout.ok) return false
    val center = findClickable(layout.stdout, labels) ?: return false
    logger.info("Tapping '${center.first}' at ${center.second},${center.third}")
    phone.shell("input tap ${center.second} ${center.third}")
    return true
  }

  public companion object {
    public suspend fun hasGoogleAccount(device: AdbDevice): Boolean = device.shell("dumpsys account").contains("type=com.google}")

    internal fun findClickable(layoutJson: String, labels: Set<String>): Triple<String, Int, Int>? {
      val nodes: JsonArray = runCatching { Json.parseToJsonElement(layoutJson.trim()).jsonArray }.getOrNull() ?: return null
      for (node in nodes.filterIsInstance<JsonObject>()) {
        val text = (node["text"] ?: node["content-desc"])?.jsonPrimitive?.content ?: continue
        val clickable = node["interactions"]?.jsonArray?.any { it.jsonPrimitive.content == "clickable" } == true
        if (!clickable || labels.none { it.equals(text.trim(), ignoreCase = true) }) continue
        val (x, y) = Regex("""\[(\d+),(\d+)]""").find(node["center"]?.jsonPrimitive?.content ?: continue)?.destructured ?: continue
        return Triple(text, x.toInt(), y.toInt())
      }
      // Play Store's Install button is an unlabeled clickable directly above an "Install on phone." caption.
      val parsed = com.example.android.wearable.emulatorpairing.tools.AndroidUi.parseLayout(layoutJson)
      val caption = parsed.firstOrNull { n -> labels.any { n.label?.trim()?.startsWith("$it on", ignoreCase = true) == true } } ?: return null
      val button = parsed.filter { it.clickable && it.label == null && it.y in (caption.y - 200) until caption.y }.maxByOrNull { it.y } ?: return null
      // The button row is split (Install | ▼); tap left of centre to hit Install rather than the dropdown.
      return Triple(caption.label!!, button.x - button.x / 10, button.y)
    }

    internal fun expandApks(paths: List<Path>): List<Path> =
      paths.flatMap { p -> if (p.isDirectory()) p.listDirectoryEntries("*.apk").sortedBy { if (it.name == "base.apk") "" else it.name } else listOf(p) }
        .filter { it.extension == "apk" }
  }
}

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
import com.android.adblib.DeviceSelector
import com.android.adblib.ShellCommandOutputElement
import com.android.adblib.SocketSpec
import com.android.adblib.shellAsLines
import com.android.adblib.shellAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * A thin suspending wrapper around an adblib [AdbSession] for a single device, mirroring the small subset of ddmlib `IDevice`
 * functionality that the Android Studio pairing code relies on.
 */
public class AdbDevice(public val session: AdbSession, public val serialNumber: String) {
  internal val selector: DeviceSelector = DeviceSelector.fromSerialNumber(serialNumber)

  /**
   * Runs [command] and returns trimmed stdout. Like the Studio implementation, failures are swallowed and produce an empty string, since
   * most callers grep the output for a marker.
   */
  public suspend fun shell(command: String): String =
    try {
      session.deviceServices.shellAsText(selector, command).stdout.trim()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      ""
    }

  /** Streams stdout lines of a long-running [command], e.g. `logcat`. */
  public fun shellLines(command: String): Flow<String> =
    session.deviceServices.shellAsLines(selector, command).filterIsInstance<ShellCommandOutputElement.StdoutLine>().map { it.contents }

  public suspend fun getProperty(name: String): String? = shell("getprop $name").takeIf { it.isNotEmpty() }

  public suspend fun abis(): List<String> = getProperty("ro.product.cpu.abilist")?.split(',')?.map { it.trim() }.orEmpty()

  public suspend fun isEmulator(): Boolean = serialNumber.startsWith("emulator-") || getProperty("ro.kernel.qemu") == "1" ||
    getProperty("ro.boot.qemu") == "1"

  public suspend fun isWatch(): Boolean =
    getProperty("ro.build.characteristics")?.contains("watch") == true || shell("pm has-feature android.hardware.type.watch").contains("true")

  /** The AVD name for emulators, or null for physical devices / older images. */
  public suspend fun avdName(): String? = getProperty("ro.boot.qemu.avd_name") ?: getProperty("ro.kernel.qemu.avd_name")

  public suspend fun isBootCompleted(): Boolean = getProperty("sys.boot_completed") == "1" || getProperty("dev.bootcomplete") != null

  /**
   * True while the current user is locked, e.g. after a reboot with a screen lock set. Until it is unlocked, Google Play
   * services and the companion app don't run, so pairing and the Wearable connection can't start.
   */
  public suspend fun isLocked(): Boolean = Regex("""\(current\).*deviceLocked=1""").containsMatchIn(shell("dumpsys trust"))

  /** Unlocks the device if it is locked with PIN [pin]. Returns true if the device ends up unlocked. */
  public suspend fun unlockWithPin(pin: String): Boolean {
    if (!isLocked()) return true
    if (!shell("dumpsys lock_settings | grep -m1 CredentialType").contains("PIN")) return false
    // KEYCODE_MENU (82) opens the PIN bouncer on phone lock screens where `wm dismiss-keyguard` stays on the clock screen.
    shell("input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard; input keyevent 82; sleep 1; input text $pin; input keyevent KEYCODE_ENTER")
    if (waitUnlocked(seconds = 6)) return true
    // Wear OS cold-boot shows `bootup_lockscreen` over the PIN pad (and ignores `input text`); swipe up to reveal the bouncer, then tap digits.
    val (w, h) = Regex("""(\d+)x(\d+)""").find(shell("wm size"))?.destructured?.let { (a, b) -> a.toInt() to b.toInt() } ?: (384 to 384)
    shell("input keyevent KEYCODE_WAKEUP; input swipe ${w / 2} ${h * 4 / 5} ${w / 2} ${h / 6} 300; sleep 1")
    val dump = shell("uiautomator dump /data/local/tmp/wp_lock.xml >/dev/null && cat /data/local/tmp/wp_lock.xml")
    val keys = Regex("""text="(\d)"[^>]*?bounds="\[(\d+),(\d+)]\[(\d+),(\d+)]"""").findAll(dump)
      .associate { m -> val (d, l, t, r, b) = m.destructured; d to "${(l.toInt() + r.toInt()) / 2} ${(t.toInt() + b.toInt()) / 2}" }
    if (pin.all { it.toString() in keys }) {
      shell(pin.toList().joinToString("; ") { "input tap ${keys.getValue(it.toString())}" } + "; input keyevent KEYCODE_ENTER")
    } else {
      shell("input text $pin; input keyevent KEYCODE_ENTER")
    }
    return waitUnlocked(seconds = 8)
  }

  private suspend fun waitUnlocked(seconds: Int = 10): Boolean {
    repeat(seconds) {
      if (!isLocked()) return true
      delay(1_000)
    }
    return false
  }

  public suspend fun uptimeSeconds(): Double = shell("cat /proc/uptime").split(' ').firstOrNull()?.toDoubleOrNull() ?: 0.0

  public suspend fun grantPermission(applicationId: String, permission: String) {
    shell("pm grant $applicationId $permission")
  }

  /** Equivalent of `adb -s <serial> forward tcp:<hostPort> tcp:<devicePort>`. */
  public suspend fun createForward(hostPort: Int, devicePort: Int) {
    session.hostServices.forward(selector, SocketSpec.Tcp(hostPort), SocketSpec.Tcp(devicePort), rebind = true)
  }

  public suspend fun removeForward(hostPort: Int) {
    session.hostServices.killForward(selector, SocketSpec.Tcp(hostPort))
  }

  /** Equivalent of `adb -s <serial> reverse tcp:<devicePort> tcp:<hostPort>`. */
  public suspend fun createReverse(devicePort: Int, hostPort: Int) {
    session.deviceServices.reverseForward(selector, SocketSpec.Tcp(devicePort), SocketSpec.Tcp(hostPort), rebind = true)
  }

  public suspend fun removeReverse(devicePort: Int) {
    session.deviceServices.reverseKillForward(selector, SocketSpec.Tcp(devicePort))
  }

  override fun toString(): String = serialNumber
}

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
package com.example.android.wearable.emulatorpairing

import com.example.android.wearable.emulatorpairing.companion.EMULATOR_PIN
import com.example.android.wearable.emulatorpairing.tools.AndroidTools
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay

// Observations used to make pairing more robust. They read `dumpsys activity service WearableService` (as Android Studio's
// pairing code does), /proc/net/tcp and logcat, and change nothing on the device.

/** The Wearable service's connection-manager dump; useful for `--verbose` diagnostics. */
public suspend fun AdbDevice.connectionServiceDump(): String = shell("dumpsys activity service WearableService ConnectionServiceManager")

/**
 * After Google Play services data is cleared on a watch without a reboot, its default `network` connection config is missing and it
 * won't accept a phone over the emulator transport until it reboots. Returns false when the dump can't be read.
 */
public suspend fun AdbDevice.watchNeedsRebootForEmulatorConnection(): Boolean {
  val dump = connectionServiceDump()
  return dump.contains("Connection Configurations:") && !dump.contains("name=network", ignoreCase = true)
}

/** Whether something on the device listens on TCP [port], or null if /proc/net/tcp can't be read. */
public suspend fun AdbDevice.isListeningOn(port: Int): Boolean? {
  val table = shell("cat /proc/net/tcp /proc/net/tcp6 2>/dev/null")
  if (table.isBlank()) return null
  val hex = ":%04X".format(port)
  // Columns: sl local_address rem_address st ...; st 0A = LISTEN.
  return table.lines().any { line ->
    val cols = line.trim().split(Regex("\\s+"))
    cols.size > 3 && cols[1].endsWith(hex, ignoreCase = true) && cols[3] == "0A"
  }
}

/**
 * If the phone's Wear transport rejected the watch because they belong to different cloud networks, returns the watch's network id
 * from `Wear_Transport: Error: networkid mismatch ... actual networkId is N` in logcat (or -1 if the id wasn't logged).
 */
public suspend fun AdbDevice.networkIdMismatch(): Long? {
  val line = shell("logcat -d -s Wear_Transport:W").lines().lastOrNull { it.contains("networkid mismatch", ignoreCase = true) } ?: return null
  return Regex("""actual\s+networkId\s+is\s+(\d+)""").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: -1
}

/** The watch's cloud network id, if it has one. */
public suspend fun AdbDevice.cloudNetworkId(): Long? =
  Regex("""cloud network id:\s*(\d+)""").find(shell("dumpsys activity service WearableService CloudNodeAdapter"))?.groupValues?.get(1)?.toLongOrNull()

/** Reboots the device and waits until it has booted and Google Play services reports a node id. */
public suspend fun AdbDevice.rebootAndWait(tools: AndroidTools, timeout: Duration = 5.minutes): Boolean {
  tools.run(tools.requireAdb(), "-s", serialNumber, "reboot", timeoutSeconds = 60)
  delay(5_000)
  val booted = waitForCondition(timeout.inWholeMilliseconds, pollMillis = 3_000) { isBootCompleted() }
  if (booted) {
    if (isEmulator() && isLocked()) unlockWithPin(EMULATOR_PIN)
    waitForCondition(60_000, pollMillis = 2_000) { loadNodeID().isNotEmpty() }
  }
  return booted
}

/** Waits up to [timeoutMillis] for the phone's Wear transport to listen on [port]; returns false only if it was observed not listening. */
public suspend fun AdbDevice.awaitListening(port: Int, timeoutMillis: Long = 20_000): Boolean {
  if (isListeningOn(port) == null) return true // Can't tell; don't block.
  return waitForCondition(timeoutMillis, pollMillis = 1_000) { isListeningOn(port) == true }
}

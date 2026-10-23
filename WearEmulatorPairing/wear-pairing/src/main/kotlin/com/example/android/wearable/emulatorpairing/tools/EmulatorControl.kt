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
package com.example.android.wearable.emulatorpairing.tools

import com.android.adblib.AdbSession
import com.android.adblib.DeviceState
import com.example.android.wearable.emulatorpairing.AdbDevice
import com.example.android.wearable.emulatorpairing.PairingLogger
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Starts, stops and wipes emulators. Prefers the Android CLI, falling back to the SDK `emulator` binary when flags are needed. */
public class EmulatorControl(
  private val session: AdbSession,
  private val tools: AndroidTools = AndroidTools(),
  private val logger: PairingLogger = PairingLogger.None,
) {
  /** Starts [avdName] and waits for boot; returns its serial. Uses `android emulator start` when available. */
  public suspend fun start(avdName: String, headless: Boolean = false, timeout: Duration = 5.minutes): AdbDevice? {
    val cli = tools.androidCli
    if (cli != null && !headless) {
      val result = tools.run(cli.toString(), "emulator", "start", avdName, timeoutSeconds = timeout.inWholeSeconds)
      if (!result.ok) logger.warn("android emulator start failed: ${result.stderr.trim()}")
    } else {
      spawnEmulator(listOf("-avd", avdName) + headlessFlags(headless))
    }
    return waitForAvd(avdName, timeout)
  }

  /**
   * Kills the emulator behind [device] and cold-boots its AVD with `-wipe-data`, keeping the same console port (and so the same
   * serial). This is the fix Studio recommends when a watch is still paired with another phone.
   */
  public suspend fun wipeAndRestart(device: AdbDevice, headless: Boolean = true, timeout: Duration = 5.minutes): AdbDevice? {
    val avdName = device.avdName() ?: error("${device.serialNumber} is not an emulator (no AVD name)")
    val port = device.serialNumber.removePrefix("emulator-").toIntOrNull() ?: error("Can't get the console port from ${device.serialNumber}")
    logger.info("Wiping $avdName (${device.serialNumber})")
    tools.run(tools.requireAdb(), "-s", device.serialNumber, "emu", "kill", timeoutSeconds = 30)
    withTimeoutOrNull(60_000) { while (device.serialNumber in allSerials()) delay(1_000) }
    spawnEmulator(listOf("-avd", avdName, "-port", port.toString(), "-wipe-data", "-no-snapshot-load") + headlessFlags(headless))
    return waitForAvd(avdName, timeout)
  }

  private fun headlessFlags(headless: Boolean) = if (headless) listOf("-no-window", "-no-audio", "-no-boot-anim") else emptyList()

  private fun spawnEmulator(args: List<String>) {
    val log = Files.createTempFile("wear-pair-emulator", ".log")
    logger.info("emulator ${args.joinToString(" ")}  (log: $log)")
    // Start in a new session so the emulator outlives this process (and Ctrl-C / killing the CLI doesn't take it down).
    val setsid = listOf("/usr/bin/setsid", "/bin/setsid").firstOrNull { java.io.File(it).canExecute() }
    tools.spawn(listOfNotNull(setsid) + listOf(tools.requireEmulator()) + args, log)
  }

  /** Waits until an online device reports [avdName] as its AVD and has finished booting. */
  private suspend fun waitForAvd(avdName: String, timeout: Duration): AdbDevice? =
    withTimeoutOrNull(timeout) {
      var found: AdbDevice? = null
      while (found == null) {
        found = onlineSerials().map { AdbDevice(session, it) }.firstOrNull { it.avdName() == avdName && it.isBootCompleted() }
        if (found == null) delay(2_000)
      }
      found
    }

  private suspend fun onlineSerials(): Set<String> =
    session.hostServices.devices().filter { it.deviceState == DeviceState.ONLINE }.map { it.serialNumber }.toSet()

  private suspend fun allSerials(): Set<String> = session.hostServices.devices().map { it.serialNumber }.toSet()
}

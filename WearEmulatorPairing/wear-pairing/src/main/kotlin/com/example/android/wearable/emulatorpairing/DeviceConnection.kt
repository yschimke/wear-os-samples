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

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

// Ported from Android Studio `wear-pairing/DeviceConnection.kt`.

private const val REFRESH_CONNECTION_COMMAND =
  "am broadcast -a com.google.android.gms.wearable.EMULATOR --es operation refresh-emulator-connection"
private const val GET_PAIRING_STATUS_COMMAND = "am broadcast -a com.google.android.gms.wearable.EMULATOR --es operation get-pairing-status"
private val LOCAL_NODE_REGEX = "Local:\\[([a-zA-Z0-9_.-]{1,64})]".toRegex()
private val PEER_NODE_REGEX = "Peer:\\[([^\\[\\],]+),(true|false),(true|false)]".toRegex()
private val VALID_NODE_ID = Regex("^[a-zA-Z0-9_.-]{1,64}$")

public data class PairingStatus(val nodeId: String?, val connected: Boolean, val enabled: Boolean)

public data class PairingStatusResult(val localNodeId: String?, val peers: List<PairingStatus>)

/** Parses the output of the `get-pairing-status` broadcast. Visible for testing. */
internal fun parsePairingStatus(broadcastResult: String): PairingStatusResult {
  val lines = broadcastResult.lines()
  val localNodeId = if (lines.size > 1) LOCAL_NODE_REGEX.find(lines[1])?.groupValues?.get(1) else null
  val peers =
    lines
      .drop(2)
      .mapNotNull { PEER_NODE_REGEX.find(it)?.groupValues }
      .filter { it.size >= 4 }
      .map { PairingStatus(if ("null".equals(it[1], ignoreCase = true)) null else it[1], it[2].toBoolean(), it[3].toBoolean()) }
  return PairingStatusResult(localNodeId, peers)
}

private suspend fun AdbDevice.localNodeFromPairingStatus(): String? =
  LOCAL_NODE_REGEX.find(shell(GET_PAIRING_STATUS_COMMAND))?.groupValues?.get(1)

public suspend fun AdbDevice.isPairingStatusAvailable(): Boolean = localNodeFromPairingStatus() != null

public suspend fun AdbDevice.getPairingStatus(): PairingStatusResult = parsePairingStatus(shell(GET_PAIRING_STATUS_COMMAND))

/** The Wearable (Data Layer) node id of this device, or empty if GMS Core hasn't initialised yet. */
public suspend fun AdbDevice.loadNodeID(): String {
  val raw = loadNodeIDRaw()
  return raw.takeIf { it.isEmpty() || VALID_NODE_ID.matches(it) } ?: ""
}

private suspend fun AdbDevice.loadNodeIDRaw(): String {
  if (hasPairingFeature(PairingFeature.GET_PAIRING_STATUS)) {
    localNodeFromPairingStatus()?.let { return it }
  }
  val localIdPattern = "local: "
  return shell("dumpsys activity service WearableService | grep '$localIdPattern'").replace(localIdPattern, "").trim()
}

public suspend fun AdbDevice.loadCloudNetworkID(ignoreNullOutput: Boolean = true): String {
  val pattern = "cloud network id: "
  return shell("dumpsys activity service WearableService | grep '$pattern'")
    .replace(pattern, "")
    .let {
      // The watch may report a "null" cloud ID until the adb bridge is established and a phone connects to it.
      if (ignoreNullOutput) it.replace("null", "") else it
    }
    .trim()
}

public suspend fun AdbDevice.isCompanionAppInstalled(companionAppId: String): Boolean {
  if (!PACKAGE_NAME_REGEX.matches(companionAppId)) return false
  return shell("dumpsys package $companionAppId | grep versionName").contains("versionName=")
}

/** Asks GMS Core to reconnect its emulator transport, or restarts GMS Core on older versions. */
public suspend fun AdbDevice.refreshEmulatorConnection(logger: PairingLogger = PairingLogger.None) {
  if (hasPairingFeature(PairingFeature.REFRESH_EMULATOR_CONNECTION)) {
    shell(REFRESH_CONNECTION_COMMAND)
  } else {
    restartGmsCore(logger)
  }
}

/** True when the phone reports the watch as an enabled + connected peer (or, on older GMS Core, the watch sees the phone node). */
public suspend fun checkDevicesPaired(phone: AdbDevice, watch: AdbDevice): Boolean {
  if (phone.hasPairingFeature(PairingFeature.GET_PAIRING_STATUS)) {
    val watchNodeId = watch.loadNodeID()
    val (localNodeId, peers) = phone.getPairingStatus()
    if (localNodeId != null) {
      return peers.any { watchNodeId.equals(it.nodeId, ignoreCase = true) && it.enabled && it.connected }
    }
  }
  val phoneNodeId = phone.loadNodeID()
  if (phoneNodeId.isNotEmpty()) {
    return watch.shell("dumpsys activity service WearableService | grep 'connection to peer node: $phoneNodeId'").isNotBlank()
  }
  return false
}

/** True if the watch believes it is already paired with a *different* phone, in which case it needs a factory reset / wipe data. */
public suspend fun checkWearMayNeedFactoryReset(phone: AdbDevice, watch: AdbDevice): Boolean {
  if (watch.hasPairingFeature(PairingFeature.GET_PAIRING_STATUS)) {
    val (watchNodeId, peers) = watch.getPairingStatus()
    if (watchNodeId != null) {
      val peer = peers.firstOrNull()?.nodeId
      return !peer.isNullOrEmpty() && !peer.equals(phone.loadNodeID(), ignoreCase = true)
    }
  }
  val phoneCloudID = phone.loadCloudNetworkID()
  val wearCloudID = watch.loadCloudNetworkID()
  return wearCloudID.isNotEmpty() && phoneCloudID != wearCloudID
}

private suspend fun AdbDevice.restartGmsCore(logger: PairingLogger) {
  val uptime = uptimeSeconds()
  // Killing GMS Core during cold boot hangs booting for a while, so skip it.
  if (uptime > 120.0) {
    logger.warn("[$serialNumber] Killing Google Play Services")
    shell("am force-stop $GMSCORE_APP_ID")
  } else {
    logger.warn("[$serialNumber] Skip killing Google Play Services (uptime = $uptime)")
  }
  logger.info("[$serialNumber] Waiting for Google Play Services to restart")
  val started =
    withTimeoutOrNull(30_000) {
      while (loadNodeID().isEmpty()) {
        shell("am broadcast -a $GMSCORE_APP_ID.INITIALIZE")
        delay(1_000)
      }
      true
    }
  logger.info("[$serialNumber] Google Play Services ${if (started == true) "started" else "never started"}")
}

internal suspend fun waitForCondition(timeoutMillis: Long, pollMillis: Long = 1_000, condition: suspend () -> Boolean): Boolean =
  withTimeoutOrNull(timeoutMillis) {
    while (!condition()) delay(pollMillis)
    true
  } == true

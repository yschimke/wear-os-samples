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

import com.android.adblib.AdbSession
import com.android.adblib.DeviceState
import com.android.adblib.SocketSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** A device as seen by [PairDiscovery]. */
public data class WearableNode(
  val serial: String,
  val avdName: String?,
  val isWatch: Boolean,
  /** Wearable (Data Layer) node id; empty if GMS Core hasn't initialised. */
  val nodeId: String,
  /** Peers this device reports via `get-pairing-status` (empty if unsupported / no pairing). */
  val peers: List<PairingStatus>,
) {
  val label: String = if (avdName != null) "$serial ($avdName)" else serial
}

public enum class PairState {
  /** Both sides see each other and the connection is up. */
  CONNECTED,
  /** Paired, but the connection is down (e.g. the adb bridge is missing after an emulator restart: run `wear-pair bridge`). */
  PAIRED_DISCONNECTED,
  /** adb forward/reverse is set up but the devices aren't paired yet. */
  BRIDGED_ONLY,
}

public data class PairInfo(
  val phone: WearableNode,
  val watch: WearableNode,
  val state: PairState,
  /** Host port of the adb bridge (`forward` on the phone / `reverse` on the watch), if present. */
  val hostPort: Int?,
)

/** A watch whose pairing status names a phone node that isn't one of the online devices. */
public data class ForeignPairing(val watch: WearableNode, val peerNodeId: String, val connected: Boolean)

public data class PairingReport(
  val pairs: List<PairInfo>,
  val foreign: List<ForeignPairing>,
  val unpairedPhones: List<WearableNode>,
  val unpairedWatches: List<WearableNode>,
)

/**
 * Lists which online phones and watches are paired with each other, by combining:
 * - each device's Wearable node id and `get-pairing-status` peers, and
 * - the adb bridges (phone `forward tcp:N tcp:5601` + watch `reverse tcp:5601 tcp:N`).
 */
public class PairDiscovery(private val session: AdbSession) {

  public suspend fun discover(): PairingReport = coroutineScope {
    val serials = session.hostServices.devices().filter { it.deviceState == DeviceState.ONLINE }.map { it.serialNumber }
    val nodes = serials.map { serial -> async { node(AdbDevice(session, serial)) } }.awaitAll()
    val (watches, phones) = nodes.partition { it.isWatch }

    val wearablePort = SocketSpec.Tcp(WEARABLE_EMULATOR_PORT).toQueryString()
    val phoneBridgesDeferred = async {
      tryOrEmpty { session.hostServices.listForward().toList() }
        .filter { it.remote.toQueryString() == wearablePort }
        .groupBy({ it.deviceSerial }, { (it.local as? SocketSpec.Tcp)?.port ?: -1 })
    }
    val watchBridges: Map<String, List<Int>> =
      watches
        .map { w ->
          async {
            w.serial to
              tryOrEmpty { session.deviceServices.reverseListForward(AdbDevice(session, w.serial).selector).toList() }
                .filter { it.remote.toQueryString() == wearablePort }
                .mapNotNull { (it.local as? SocketSpec.Tcp)?.port }
          }
        }
        .awaitAll()
        .toMap()
    val phoneBridges: Map<String, List<Int>> = phoneBridgesDeferred.await()

    val pairs = mutableListOf<PairInfo>()
    for (phone in phones) {
      for (watch in watches) {
        val phoneSees = watch.nodeId.takeIf { it.isNotEmpty() }?.let { id -> phone.peers.firstOrNull { id.equals(it.nodeId, ignoreCase = true) } }
        val watchSees = phone.nodeId.takeIf { it.isNotEmpty() }?.let { id -> watch.peers.firstOrNull { id.equals(it.nodeId, ignoreCase = true) } }
        val port = phoneBridges[phone.serial].orEmpty().firstOrNull { it in watchBridges[watch.serial].orEmpty() }
        val state =
          when {
            (phoneSees ?: watchSees)?.let { it.connected && it.enabled } == true -> PairState.CONNECTED
            phoneSees != null || watchSees != null -> PairState.PAIRED_DISCONNECTED
            port != null -> PairState.BRIDGED_ONLY
            else -> null
          }
        if (state != null) pairs += PairInfo(phone, watch, state, port)
      }
    }

    val phoneIds = phones.map { it.nodeId.lowercase() }.toSet()
    val foreign =
      watches.flatMap { w ->
        w.peers.filter { p -> !p.nodeId.isNullOrEmpty() && p.nodeId.lowercase() !in phoneIds }.map { ForeignPairing(w, it.nodeId!!, it.connected) }
      }
    val pairedSerials = pairs.flatMap { listOf(it.phone.serial, it.watch.serial) }.toSet()
    PairingReport(
      pairs = pairs,
      foreign = foreign,
      unpairedPhones = phones.filter { it.serial !in pairedSerials },
      unpairedWatches = watches.filter { it.serial !in pairedSerials && foreign.none { f -> f.watch.serial == it.serial } },
    )
  }

  private suspend fun node(device: AdbDevice): WearableNode = coroutineScope {
    val statusDeferred = async { device.getPairingStatus() }
    val avdNameDeferred = async { device.avdName() }
    val isWatchDeferred = async { device.isWatch() }
    val status = statusDeferred.await()
    val nodeId = status.localNodeId ?: device.loadNodeID()
    WearableNode(device.serialNumber, avdNameDeferred.await(), isWatchDeferred.await(), nodeId, status.peers)
  }

  private suspend fun <T> tryOrEmpty(block: suspend () -> List<T>): List<T> =
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      emptyList()
    }
}

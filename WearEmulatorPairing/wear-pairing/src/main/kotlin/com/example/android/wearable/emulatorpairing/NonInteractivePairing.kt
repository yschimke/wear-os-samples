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

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** States reported by the companion app's `EmulatorActivity` in logcat as `[EMULATOR_PAIRING:<STATE>]`. */
public enum class CompanionPairingState {
  UNKNOWN,
  INTERNAL_ERROR,

  // Below correspond to states reported by the companion app and must not be renamed.
  STARTED,
  CONSENT,
  PAIRING,
  SUCCESS,
  FAILURE,
  CANCELLED;

  public val hasFinished: Boolean
    get() = this == INTERNAL_ERROR || this == SUCCESS || this == FAILURE || this == CANCELLED
}

private val STATE_LOG_PATTERN = "\\[EMULATOR_PAIRING:([^]]+)]".toRegex()
private val SAFE_ARG = Regex("^[a-zA-Z0-9_.\\- ]{1,128}$")

internal fun parseCompanionPairingState(line: String): CompanionPairingState? =
  STATE_LOG_PATTERN.find(line)?.groupValues?.get(1)?.let { runCatching { CompanionPairingState.valueOf(it) }.getOrDefault(CompanionPairingState.UNKNOWN) }

/**
 * Non-interactive pairing, ported from Android Studio `NonInteractivePairing.kt`.
 *
 * Tails the phone's logcat, launches `<companion>/.EmulatorActivity` once logcat is attached (so no state line is missed), and emits the
 * states the companion app logs. The flow completes after a terminal state ([CompanionPairingState.hasFinished]).
 */
public fun AdbDevice.companionEmulatorPairing(companionAppId: String, watchName: String, watchNodeId: String): Flow<CompanionPairingState> {
  require(PACKAGE_NAME_REGEX.matches(companionAppId)) { "Invalid companion app id: $companionAppId" }
  require(SAFE_ARG.matches(watchName)) { "Unsafe watch name: $watchName" }
  require(watchNodeId.isEmpty() || SAFE_ARG.matches(watchNodeId)) { "Unsafe watch node id: $watchNodeId" }
  val phone = this

  return channelFlow {
      val logcatAttached = CompletableDeferred<Unit>()
      val activityRequested = AtomicBoolean(false)

      launch {
        // Start after the first logcat line arrives; fall back after a short delay in case logcat is quiet.
        withTimeoutOrNull(2_000) { logcatAttached.await() }
        activityRequested.set(true)
        phone.shell(
          "am start -n $companionAppId/.EmulatorActivity --es emulator-name \"$watchName\" --es emulator-id \"$watchNodeId\""
        )
      }

      try {
        phone.shellLines("logcat -T 1").collect { line ->
          logcatAttached.complete(Unit)
          // Ignore lines emitted before we launched the activity, e.g. a stale SUCCESS from a previous run.
          if (activityRequested.get()) {
            parseCompanionPairingState(line)?.let { send(it) }
          }
        }
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        send(CompanionPairingState.INTERNAL_ERROR)
      }
    }
    .transformWhile {
      emit(it)
      !it.hasFinished
    }
}

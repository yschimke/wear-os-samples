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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParsingTest {
  @Test
  fun parsesPairingStatus() {
    val output =
      """
      Broadcasting: Intent { act=com.google.android.gms.wearable.EMULATOR flg=0x400000 (has extras) }
      Local:[a1b2c3d4]
      Peer:[e5f6a7b8,true,true]
      Peer:[null,false,true]
      Broadcast completed: result=0
      """
        .trimIndent()

    val result = parsePairingStatus(output)
    assertEquals("a1b2c3d4", result.localNodeId)
    assertEquals(listOf(PairingStatus("e5f6a7b8", connected = true, enabled = true), PairingStatus(null, false, true)), result.peers)
  }

  @Test
  fun missingLocalNode() {
    assertNull(parsePairingStatus("Broadcasting: Intent {}\nBroadcast completed: result=0").localNodeId)
  }

  @Test
  fun parsesCompanionStates() {
    assertEquals(CompanionPairingState.CONSENT, parseCompanionPairingState("10-22 16:00:00.000 I Companion: [EMULATOR_PAIRING:CONSENT]"))
    assertEquals(CompanionPairingState.UNKNOWN, parseCompanionPairingState("[EMULATOR_PAIRING:SOMETHING_NEW]"))
    assertNull(parseCompanionPairingState("unrelated log line"))
  }

  @Test
  fun sanitizesNames() {
    assertEquals("Wear_OS_Large_Round_API_34", sanitizeName("Wear_OS_Large_Round_API_34"))
    assertEquals("a_b__c", sanitizeName("a\"b;\$c"))
  }
}

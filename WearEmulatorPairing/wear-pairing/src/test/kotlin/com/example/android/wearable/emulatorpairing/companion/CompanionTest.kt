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

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CompanionTest {
  @Test
  fun singlePropertyTestAccount() {
    val file = Files.createTempFile("ta", ".properties")
    file.writeText("testaccount=someone.test@gmail.com:pa:ss=word\n")
    val account = TestAccount.load(file, env = emptyMap())!!
    assertEquals("someone.test@gmail.com", account.email)
    assertEquals("pa:ss=word", account.password)
    assertFalse(account.toString().contains("pa:ss"))
  }

  @Test
  fun sanitizesVerificationMethods() {
    assertEquals("Get a verification code by email", GoogleSignIn.sanitizeMethod("Get a verification code at y•••@ex••••.••"))
    assertEquals("Get a verification code by text message", GoogleSignIn.sanitizeMethod("Get a verification code at •••• ••• ••12"))
    assertEquals("Use another phone or computer to finish signing in", GoogleSignIn.sanitizeMethod("Use another phone or computer to finish signing in"))
  }

  @Test
  fun parsesLayoutNodes() {
    val nodes =
      com.example.android.wearable.emulatorpairing.tools.AndroidUi.parseLayout(
        """[{"interactions":["clickable","focusable","password"],"state":["focused"],"center":"[539,707]"},
           {"text":"NEXT","interactions":["clickable"],"center":"[911,1333]"},
           {"interactions":["clickable"],"center":"[539,829]","resource-id":"identifierId"}]"""
      )
    assertEquals(3, nodes.size)
    assert(nodes[0].isPassword && nodes[0].focused)
    assert(nodes[1].labelIs("next"))
    assertEquals("identifierId", nodes[2].resourceId)
  }

  @Test
  fun envOverridesFile() {
    val file = Files.createTempFile("ta", ".properties")
    file.writeText("testaccount=file@gmail.com:x\n")
    assertEquals("env@gmail.com", TestAccount.load(file, mapOf("WEAR_PAIR_TESTACCOUNT" to "env@gmail.com:y"))!!.email)
  }

  @Test
  fun notConfigured() {
    assertNull(TestAccount.load(Files.createTempFile("ta", ".properties"), emptyMap()))
  }

  @Test
  fun findsClickableInstallButton() {
    val layout =
      """[{"text":"Pixel Watch","center":"[540,300]"},
         {"text":"Install","interactions":["clickable","focusable"],"center":"[800,950]"},
         {"text":"Sign in","interactions":["clickable"],"center":"[539,1581]"}]"""
    assertEquals(Triple("Install", 800, 950), CompanionInstaller.findClickable(layout, setOf("Install", "Update")))
    assertNull(CompanionInstaller.findClickable(layout, setOf("Open")))
  }

  @Test
  fun findsUnlabeledPlayStoreInstallButton() {
    val layout =
      """[{"interactions":["clickable","focusable"],"center":"[540,1185]"},
         {"text":"Install on phone.","center":"[920,1268]"},
         {"interactions":["clickable","focusable"],"center":"[540,1872]"}]"""
    val found = CompanionInstaller.findClickable(layout, setOf("Install", "Update"))!!
    assertEquals(1185, found.third)
  }
}

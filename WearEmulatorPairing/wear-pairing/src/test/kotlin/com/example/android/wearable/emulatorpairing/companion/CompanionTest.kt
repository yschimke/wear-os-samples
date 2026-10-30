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

import com.example.android.wearable.emulatorpairing.tools.AndroidUi
import com.example.android.wearable.emulatorpairing.tools.findClickable
import com.example.android.wearable.emulatorpairing.tools.hasScreenText
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
      AndroidUi.parseLayout(
        """[{"interactions":["clickable","focusable","password"],"state":["focused"],"center":"[539,707]"},
           {"text":"NEXT","interactions":["clickable"],"center":"[911,1333]"},
           {"interactions":["clickable"],"center":"[539,829]","resource-id":"android:id/identifierId"}]"""
      )
    assertEquals(3, nodes.size)
    assertTrue(nodes[0].isPassword && nodes[0].focused)
    assertTrue(nodes[1].labelIs("next"))
    assertEquals("identifierId", nodes[2].resourceId)
  }

  @Test
  fun matchesPerWordWebViewScreenTextAndComposeChildButtons() {
    val nodes =
      AndroidUi.parseLayout(
        """[{"text":"Sign","center":"[200,400]"},
           {"text":"in","center":"[310,400]"},
           {"text":"with","center":"[410,400]"},
           {"text":"ease","center":"[520,400]"},
           {"interactions":["clickable","focusable"],"center":"[180,1950]"},
           {"text":"Skip","center":"[180,1950]"}]"""
      )
    assertTrue(nodes.hasScreenText("Sign in with ease"))
    assertEquals(180 to 1950, nodes.findClickable("SKIP", "Skip")?.let { it.x to it.y })
  }

  @Test
  fun parsesUiAutomatorXmlAndStripsResourceIdPackage() {
    val xml =
      """UI hierchary dumped to: /data/local/tmp/wp_ui.xml
         <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
         <hierarchy rotation="0">
           <node text="System UI isn&apos;t responding" resource-id="android:id/alertTitle" clickable="false" bounds="[100,1050][980,1150]" />
           <node text="Wait" resource-id="android:id/aerr_wait" clickable="true" focusable="true" bounds="[100,1300][980,1420]" />
           <node text="" resource-id="" clickable="true" bounds="[64,1135][1017,1261]">
             <node text="Install" resource-id="" clickable="false" bounds="[432,1171][534,1224]" />
           </node>
         </hierarchy>"""
    val nodes = AndroidUi.parseUiAutomatorXml(xml)
    assertEquals("aerr_wait", nodes.firstOrNull { it.labelIs("Wait") }?.resourceId)
    assertEquals(483 to 1197, nodes.findClickable("Install")?.let { it.x to it.y })
  }

  @Test
  fun escapesSingleQuotesAndSpacesForShellInput() {
    assertEquals("'pa'\\''ss%sword'", AndroidUi.escapeForShellInput("pa'ss word"))
  }

  @Test
  fun extractsDevicePromptAndFiltersVerificationMethods() {
    val splitNodes =
      AndroidUi.parseLayout(
        """[{"text":"2-Step","center":"[200,300]"},{"text":"Verification","center":"[450,300]"},
           {"text":"Check","center":"[180,500]"},{"text":"your","center":"[290,500]"},{"text":"Pixel","center":"[410,500]"},{"text":"9.","center":"[510,500]"},
           {"text":"42","center":"[540,900]"}]"""
      )
    assertEquals("Pixel 9" to "42", GoogleSignIn.extractDevicePrompt(splitNodes))

    val methodNodes =
      AndroidUi.parseLayout(
        """[{"text":"Verify it's you","interactions":["clickable"],"center":"[540,300]"},
           {"text":"Tap Yes on your phone or tablet","interactions":["clickable"],"center":"[540,700]"},
           {"content-desc":"Get a verification code at y•••@gmail.com","interactions":["clickable"],"center":"[540,900]"},
           {"text":"Try another way","interactions":["clickable"],"center":"[220,1400]"},
           {"text":"Learn more about 2-Step Verification","interactions":["clickable"],"center":"[540,1600]"}]"""
      )
    assertEquals(2, GoogleSignIn.extractVerificationMethods(methodNodes).size)
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
  fun findsComposeSplitPlayStoreInstallButton() {
    val layout =
      """[{"interactions":["clickable","focusable"],"center":"[540,1198]"},
         {"text":"Install on phone.","center":"[899,1281]"},
         {"content-desc":"Install","center":"[483,1197]"},
         {"interactions":["clickable","focusable"],"center":"[954,1197]"},
         {"text":"Install","center":"[483,1197]"},
         {"content-desc":"Install on more devices","center":"[954,1197]"}]"""
    assertEquals(Triple("Install", 483, 1197), CompanionInstaller.findClickable(layout, setOf("Install", "Update")))
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

  @Test
  fun classifiesPlayStoreStatesWithoutTappingCancel() {
    val downloading =
      AndroidUi.parseLayout(
        """[{"text":"Google Pixel Watch","center":"[599,405]"},
           {"content-desc":"35%","center":"[160,647]"},
           {"text":"Verified by Play Protect","center":"[420,647]"},
           {"text":"Cancel","interactions":["clickable"],"center":"[293,800]"},
           {"text":"Open","center":"[786,800]"}]"""
      )
    assertIs<PlayStoreAction.InProgress>(CompanionInstaller.classifyPlayStoreScreen(downloading, autoTapInstall = true))

    val staleSignIn =
      AndroidUi.parseLayout("""[{"text":"Sign in","interactions":["clickable"],"center":"[540,1200]"}]""")
    assertEquals(PlayStoreAction.RefreshSignedInPlayStore, CompanionInstaller.classifyPlayStoreScreen(staleSignIn, autoTapInstall = true))

    val skipPayment =
      AndroidUi.parseLayout(
        """[{"text":"Complete account setup","center":"[540,500]"},
           {"text":"Add payment method","center":"[540,900]"},
           {"text":"Skip","interactions":["clickable"],"center":"[250,1800]"}]"""
      )
    val tap = assertIs<PlayStoreAction.Tap>(CompanionInstaller.classifyPlayStoreScreen(skipPayment, autoTapInstall = true))
    assertEquals("skip-payment", tap.reason)
  }
}

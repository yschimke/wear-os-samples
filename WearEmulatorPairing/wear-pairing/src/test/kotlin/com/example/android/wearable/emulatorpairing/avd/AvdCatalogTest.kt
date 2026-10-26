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
package com.example.android.wearable.emulatorpairing.avd

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvdCatalogTest {
  private fun installFakeImage(sdk: Path, platform: String, tagDir: String, abi: String, apiProp: String? = null, tagId: String = tagDir, withImg: Boolean = true) {
    val dir = sdk.resolve("system-images/$platform/$tagDir/$abi")
    Files.createDirectories(dir)
    if (withImg) dir.resolve("system.img").writeText("img")
    val lines = buildList {
      if (apiProp != null) add("AndroidVersion.ApiLevel=$apiProp")
      add("SystemImage.TagId=$tagId")
      add("SystemImage.Abi=$abi")
    }
    dir.resolve("source.properties").writeText(lines.joinToString("\n"))
  }

  @Test
  fun sortsDecimalApisPrefersSignedAndSkipsPartialAndChina() {
    val sdk = Files.createTempDirectory("sdk")
    val avdHome = Files.createTempDirectory("avd")
    val abi = hostAbi()
    installFakeImage(sdk, "android-36", "android-wear-signed", abi, apiProp = "36", tagId = "android-wear")
    installFakeImage(sdk, "android-36.1", "android-wear-signed", abi, apiProp = "36.1", tagId = "android-wear")
    installFakeImage(sdk, "android-37.0", "android-wear", abi, apiProp = "37.0", tagId = "android-wear")
    installFakeImage(sdk, "android-37.0", "android-wear-signed", abi, apiProp = "37.0", tagId = "android-wear")
    installFakeImage(sdk, "android-37.0", "android-wear-cn", abi, apiProp = "37.0", tagId = "android-wear-cn")
    installFakeImage(sdk, "android-", "android-wear-signed", abi, apiProp = "36.1", tagId = "android-wear") // malformed platform dir
    installFakeImage(sdk, "android-35", "android-wear", abi, withImg = false) // partial install

    val catalog = AvdCatalog(SdkLocations(sdk, avdHome))
    assertEquals(
      listOf(
        "system-images;android-37.0;android-wear-signed;$abi",
        "system-images;android-37.0;android-wear;$abi",
        "system-images;android-36.1;android-wear-signed;$abi",
        "system-images;android-36;android-wear-signed;$abi",
      ),
      catalog.suitableWatchImages().map { it.packageId },
    )
  }

  @Test
  fun createsAndAssessesPhoneAndWatchAvds() {
    val sdk = Files.createTempDirectory("sdk")
    val avdHome = Files.createTempDirectory("avd")
    val abi = hostAbi()
    installFakeImage(sdk, "android-37.0", "google_apis_playstore", abi, apiProp = "37.0")
    installFakeImage(sdk, "android-37.0", "android-wear-signed", abi, apiProp = "37.0", tagId = "android-wear")
    installFakeImage(sdk, "android-36", "android-wear-signed", abi, apiProp = "36", tagId = "android-wear")

    val catalog = AvdCatalog(SdkLocations(sdk, avdHome))
    val phone = catalog.createAvd("Test_Phone", catalog.suitablePhoneImages().first())
    assertTrue(phone.configDir.resolve("config.ini").readText().contains("disk.dataPartition.size=8G"))
    assertTrue(phone.playStoreEnabled)
    assertTrue(catalog.assessPhone(phone).studioAccepts)

    val watch37 = catalog.createAvd("Test_Watch37", catalog.suitableWatchImages().first())
    assertTrue(watch37.configDir.resolve("config.ini").readText().contains("disk.dataPartition.size=2G"))
    assertEquals(emptyList(), catalog.assessWatch(watch37).problems)

    val watch36 = catalog.createAvd("Test_Watch36", catalog.suitableWatchImages().last())
    val assessment36 = catalog.assessWatch(watch36)
    assertTrue(assessment36.usable && assessment36.studioAccepts)
    assertEquals(listOf(Severity.WARNING), assessment36.problems.map { it.severity })
  }
}

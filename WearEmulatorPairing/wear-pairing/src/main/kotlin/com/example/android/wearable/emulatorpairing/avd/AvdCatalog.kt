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
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

/** Where AVDs and the SDK live, resolved the same way the emulator / Studio do. */
public data class SdkLocations(val sdkRoot: Path?, val avdHome: Path) {
  public companion object {
    public fun detect(env: Map<String, String> = System.getenv()): SdkLocations {
      val home = Paths.get(System.getProperty("user.home"))
      val avdHome =
        env["ANDROID_AVD_HOME"]?.let { Paths.get(it) }
          ?: env["ANDROID_USER_HOME"]?.let { Paths.get(it, "avd") }
          ?: env["ANDROID_EMULATOR_HOME"]?.let { Paths.get(it, "avd") }
          ?: home.resolve(".android/avd")
      val sdkRoot =
        listOfNotNull(env["ANDROID_HOME"], env["ANDROID_SDK_ROOT"])
          .map { Paths.get(it) }
          .plus(listOf(home.resolve("Android/Sdk"), home.resolve("Library/Android/sdk")))
          .firstOrNull { it.isDirectory() }
      return SdkLocations(sdkRoot, avdHome)
    }
  }
}

/** An installed system image under `<sdk>/system-images/<platform>/<tag>/<abi>`. */
public data class SystemImage(val path: Path, val platform: String, val tagIds: List<String>, val abi: String, val api: Int?) {
  /** The `sdkmanager` package id, e.g. `system-images;android-36;google_apis_playstore;x86_64`. */
  val packageId: String = "system-images;$platform;${path.parent.name};$abi"
  val isWear: Boolean = isWearTag(tagIds)
  val hasPlayStore: Boolean = hasPlayStoreTag(tagIds, path.toString())
}

public data class Avd(
  val name: String,
  val displayName: String,
  val configDir: Path,
  val target: String?,
  val api: Int?,
  val tagIds: List<String>,
  val abi: String?,
  /** `PlayStore.enabled` from config.ini. This is the only thing Studio's `AvdInfo.hasPlayStore()` looks at. */
  val playStoreEnabled: Boolean,
  val imageSysdir: String?,
  val image: SystemImage?,
) {
  val isWear: Boolean = isWearTag(tagIds)
  /** Whether the system image itself ships Google Play, regardless of the config flag. */
  val imageHasPlayStore: Boolean = image?.hasPlayStore ?: hasPlayStoreTag(tagIds, imageSysdir.orEmpty())
}

public enum class Severity {
  /** Pairing will not work. */
  ERROR,
  /** Android Studio's pairing assistant rejects it, but this tool can still pair it. */
  STUDIO_ONLY,
  /** Might cause trouble. */
  WARNING,
}

public data class Problem(val severity: Severity, val message: String, val fix: String? = null)

public data class AvdAssessment(val avd: Avd, val problems: List<Problem>) {
  val usable: Boolean = problems.none { it.severity == Severity.ERROR }
  val studioAccepts: Boolean = problems.none { it.severity != Severity.WARNING }
}

/** Minimum API levels enforced by Studio's `DeviceListStep.isDisabled()`. */
public const val MIN_PHONE_API: Int = 30
public const val MIN_WATCH_API: Int = 28

public class AvdCatalog(public val locations: SdkLocations = SdkLocations.detect()) {

  public fun systemImages(): List<SystemImage> {
    val root = locations.sdkRoot?.resolve("system-images") ?: return emptyList()
    if (!root.isDirectory()) return emptyList()
    return root.listDirectoryEntries().filter { it.isDirectory() }.flatMap { platform ->
      platform.listDirectoryEntries().filter { it.isDirectory() }.flatMap { tag ->
        tag.listDirectoryEntries().filter { it.isDirectory() && it.resolve("system.img").exists() } // skip partial installs
          .map { abiDir -> readSystemImage(platform.name, tag.name, abiDir) }
      }
    }
  }

  public fun avds(): List<Avd> {
    val home = locations.avdHome
    if (!home.isDirectory()) return emptyList()
    val images = systemImages().associateBy { it.path.normalize() }
    return home.listDirectoryEntries("*.ini").filter { it.isRegularFile() }.mapNotNull { ini -> readAvd(ini, images) }.sortedBy { it.name }
  }

  public fun assessPhone(avd: Avd): AvdAssessment {
    val problems = mutableListOf<Problem>()
    if (avd.isWear) problems += Problem(Severity.ERROR, "This is a Wear OS image, not a phone")
    avd.api?.takeIf { it < MIN_PHONE_API }?.let {
      problems += Problem(Severity.ERROR, "API $it is too old: phone emulators must be API $MIN_PHONE_API or newer", recommendPhoneImageFix())
    }
    if (!avd.imageHasPlayStore) {
      problems +=
        Problem(
          Severity.ERROR,
          "Image '${avd.tagIds.joinToString()}' doesn't include Google Play, so the companion app (Pixel Watch / Wear OS) can't be " +
            "installed from Play and Studio hides this AVD (a local --companion-apk still works)",
          recommendPhoneImageFix(),
        )
    } else if (!avd.playStoreEnabled) {
      problems +=
        Problem(
          Severity.STUDIO_ONLY,
          "The image has Google Play, but config.ini says PlayStore.enabled=no, so Studio's pairing assistant greys it out " +
            "(\"requires Play\"). AVDs created by avdmanager or copied often have this.",
          "wear-pair enable-playstore ${avd.name}   (sets PlayStore.enabled=yes; run with the emulator stopped)",
        )
    }
    imageMissingProblem(avd)?.let { problems += it }
    return AvdAssessment(avd, problems)
  }

  public fun assessWatch(avd: Avd): AvdAssessment {
    val problems = mutableListOf<Problem>()
    if (!avd.isWear) problems += Problem(Severity.ERROR, "Not a Wear OS image")
    avd.api?.takeIf { it < MIN_WATCH_API }?.let {
      problems += Problem(Severity.ERROR, "API $it is too old: Wear OS emulators must be API $MIN_WATCH_API or newer", recommendWatchImageFix())
    }
    imageMissingProblem(avd)?.let { problems += it }
    return AvdAssessment(avd, problems)
  }

  /** Installed images that would make a usable phone, newest first. */
  public fun suitablePhoneImages(): List<SystemImage> =
    systemImages().filter { !it.isWear && it.hasPlayStore && (it.api ?: 0) >= MIN_PHONE_API && it.abi == hostAbi() }.sortedByDescending { it.api }

  /**
   * Creates (or replaces) an AVD named [name] from [image], writing the same files the AVD manager would. Phone AVDs on Play images
   * get `PlayStore.enabled=yes` so Studio's pairing assistant accepts them too.
   */
  public fun createAvd(name: String, image: SystemImage): Avd {
    require(Regex("^[A-Za-z0-9._-]+$").matches(name)) { "Invalid AVD name '$name'" }
    val sdk = locations.sdkRoot ?: error("Android SDK not found")
    val dir = locations.avdHome.resolve("$name.avd")
    dir.toFile().deleteRecursively()
    Files.createDirectories(dir)
    val wear = image.isWear
    val (w, h, density) = if (wear) Triple(384, 384, 320) else Triple(1080, 2400, 420)
    val sysdir = sdk.relativize(image.path).toString().trimEnd('/') + "/"
    locations.avdHome.resolve("$name.ini").writeLines(listOf("avd.ini.encoding=UTF-8", "path=$dir", "path.rel=avd/$name.avd", "target=${image.platform}"))
    dir.resolve("config.ini").writeLines(
      listOf(
        "avd.ini.displayname=${name.replace('_', ' ')}",
        "abi.type=${image.abi}",
        "hw.cpu.arch=${if (image.abi.startsWith("arm64")) "arm64" else "x86_64"}",
        "hw.cpu.ncore=4",
        "hw.gpu.enabled=yes",
        "hw.gpu.mode=auto",
        "hw.keyboard=yes",
        "hw.lcd.density=$density",
        "hw.lcd.height=$h",
        "hw.lcd.width=$w",
        "hw.ramSize=${if (wear) 2048 else 4096}",
        // Phones need room for Play Store installs; watches don't.
        "disk.dataPartition.size=${if (wear) "2G" else "8G"}",
        "image.sysdir.1=$sysdir",
        "tag.id=${image.tagIds.first()}",
        "tag.ids=${image.tagIds.joinToString(",")}",
        "target=${image.platform}",
        "PlayStore.enabled=${if (image.hasPlayStore) "yes" else "no"}",
      ) + if (wear) listOf("hw.lcd.circular=true") else emptyList()
    )
    return avds().first { it.name == name }
  }

  public fun suitableWatchImages(): List<SystemImage> =
    systemImages().filter { it.isWear && (it.api ?: 0) >= MIN_WATCH_API && it.abi == hostAbi() }.sortedByDescending { it.api }

  /** Sets `PlayStore.enabled=yes` in the AVD's config.ini. Only allowed when the image actually ships Google Play. */
  public fun enablePlayStore(avd: Avd) {
    require(avd.imageHasPlayStore) { "${avd.name} uses an image without Google Play (${avd.tagIds.joinToString()}); create a new AVD instead" }
    val config = avd.configDir.resolve("config.ini")
    val lines = config.readLines()
    val updated =
      if (lines.any { it.startsWith("PlayStore.enabled=") }) lines.map { if (it.startsWith("PlayStore.enabled=")) "PlayStore.enabled=yes" else it }
      else lines + "PlayStore.enabled=yes"
    config.writeLines(updated)
  }

  public fun recommendPhoneImageFix(): String {
    val installed = suitablePhoneImages().firstOrNull()
    val pkg = installed?.packageId ?: "system-images;android-36;google_apis_playstore;${hostAbi()}"
    val install = if (installed == null) "sdkmanager \"$pkg\" && " else ""
    return "Create a phone AVD from a Google Play image (API $MIN_PHONE_API+): ${install}avdmanager create avd -n Wear_Pair_Phone -k \"$pkg\" " +
      "-d pixel_8, then wear-pair enable-playstore Wear_Pair_Phone"
  }

  public fun recommendWatchImageFix(): String {
    val installed = suitableWatchImages().firstOrNull()
    val pkg = installed?.packageId ?: "system-images;android-36;android-wear-signed;${hostAbi()}"
    val install = if (installed == null) "sdkmanager \"$pkg\" && " else ""
    return "${install}avdmanager create avd -n Wear_Pair_Watch -k \"$pkg\" -d wearos_large_round"
  }

  private fun imageMissingProblem(avd: Avd): Problem? =
    if (locations.sdkRoot != null && avd.imageSysdir != null && avd.image == null) {
      val pkg = avd.imageSysdir.trim('/').split('/').let { if (it.size >= 4) it.joinToString(";") else avd.imageSysdir }
      Problem(Severity.ERROR, "System image ${avd.imageSysdir} is not installed", "sdkmanager \"$pkg\"")
    } else null

  private fun readSystemImage(platform: String, tag: String, abiDir: Path): SystemImage {
    val props = readProperties(abiDir.resolve("source.properties"))
    val tagIds = props["SystemImage.TagId"]?.split(',')?.map { it.trim() } ?: listOf(tag)
    val api = props["AndroidVersion.ApiLevel"]?.toIntOrNull() ?: parseApi(platform)
    return SystemImage(abiDir.normalize(), platform, tagIds, props["SystemImage.Abi"] ?: abiDir.name, api)
  }

  private fun readAvd(ini: Path, images: Map<Path, SystemImage>): Avd? {
    val outer = readProperties(ini)
    val name = ini.fileName.toString().removeSuffix(".ini")
    val dir = outer["path"]?.let { Paths.get(it) }?.takeIf { it.isDirectory() } ?: ini.resolveSibling("$name.avd").takeIf { it.isDirectory() }
    ?: return null
    val config = readProperties(dir.resolve("config.ini"))
    val target = config["target"] ?: outer["target"]
    val sysdir = config["image.sysdir.1"]
    val image = sysdir?.let { s -> locations.sdkRoot?.resolve(s)?.normalize()?.let { images[it] } }
    val tagIds = config["tag.ids"]?.split(',')?.map { it.trim() } ?: listOfNotNull(config["tag.id"])
    val enabled = config["PlayStore.enabled"]
    return Avd(
      name = name,
      displayName = config["avd.ini.displayname"] ?: name.replace('_', ' '),
      configDir = dir,
      target = target,
      api = image?.api ?: target?.let { parseApi(it) },
      tagIds = tagIds,
      abi = config["abi.type"],
      playStoreEnabled = enabled.equals("true", ignoreCase = true) || enabled.equals("yes", ignoreCase = true),
      imageSysdir = sysdir,
      image = image,
    )
  }
}

internal fun isWearTag(tagIds: List<String>): Boolean = tagIds.any { it.startsWith("android-wear") || it == "wear" }

internal fun hasPlayStoreTag(tagIds: List<String>, path: String): Boolean =
  // Wear images ship Play Store too, but the companion only needs to be installed on the phone.
  tagIds.any { it.contains("playstore") } || path.contains("playstore")

internal fun parseApi(target: String): Int? = Regex("""android-(\d+)""").find(target)?.groupValues?.get(1)?.toIntOrNull()

public fun hostAbi(): String =
  when (System.getProperty("os.arch")) {
    "aarch64", "arm64" -> "arm64-v8a"
    else -> "x86_64"
  }

private fun readProperties(file: Path): Map<String, String> =
  if (!file.exists()) emptyMap()
  else
    Files.readAllLines(file).mapNotNull { line ->
      val idx = line.indexOf('=')
      if (idx <= 0 || line.startsWith("#")) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
    }.toMap()

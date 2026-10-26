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

/** Ported from Android Studio `wear-pairing/PairingFeature.kt`. */
public enum class PairingFeature(public val minVersion: Int) {
  /** Phone: companion app supports `.EmulatorActivity` for non-interactive pairing. */
  COMPANION_EMULATOR_ACTIVITY(773385010),

  /** Phone/watch: GMS Core supports the `refresh-emulator-connection` broadcast. */
  REFRESH_EMULATOR_CONNECTION(213013000),

  /** Phone: GMS Core supports multiple watches paired to one phone. */
  MULTI_WATCH_SINGLE_PHONE_PAIRING(213204000),

  /** Watch: GMS Core supports connecting via `adb reverse`. */
  REVERSE_PORT_FORWARD(210915000),

  /** Phone/watch: GMS Core supports the `get-pairing-status` broadcast. */
  GET_PAIRING_STATUS(214412000),

  /** Phone: older companions need the user to tap "Skip and finish" after manual pairing. */
  COMPANION_SKIP_AND_FINISH_FIXED(773393865),

  /** Watch: GMS Core 26.x+ (Wear OS 7) completes copying a Google account from the paired phone on emulators. */
  WATCH_ACCOUNT_COPY(260000000),
}

public enum class CompanionSupport {
  SUPPORTED,
  INCOMPATIBLE_ABI,
  INCOMPATIBLE_COMPANION_ID,
}

public const val GMSCORE_APP_ID: String = "com.google.android.gms"
public const val PIXEL_COMPANION_APP_ID: String = "com.google.android.apps.wear.companion"
public const val OEM_COMPANION_FALLBACK_APP_ID: String = "com.google.android.wearable.app"

private val VERSION_CODE_PATTERN = "versionCode=(\\d+)".toRegex()
private const val OEM_COMPANION_SETTING_KEY = "cw.oem_companion_package"
private const val OEM_COMPANION_PROPERTY_KEY = "ro.oem.companion_package"
private val availableCompanionAppIds = setOf(PIXEL_COMPANION_APP_ID, OEM_COMPANION_FALLBACK_APP_ID)

/** Android package-name grammar; also guarantees no shell metacharacters. */
internal val PACKAGE_NAME_REGEX = Regex("""[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+""")

public suspend fun AdbDevice.getAppVersionCode(appId: String?): Int? =
  if (appId == null || !PACKAGE_NAME_REGEX.matches(appId)) null
  else VERSION_CODE_PATTERN.find(shell("dumpsys package $appId | grep versionCode | head -n1"))?.groupValues?.get(1)?.toInt()

/** Which companion app the watch expects on the phone (Pixel Watch app, an OEM app, or the legacy Wear OS app). */
public suspend fun AdbDevice.getCompanionAppIdForWatch(logger: PairingLogger = PairingLogger.None): String {
  val settingValue = shell("settings get secure $OEM_COMPANION_SETTING_KEY")
  if (settingValue.isNotBlank() && settingValue != "null") {
    if (PACKAGE_NAME_REGEX.matches(settingValue)) return settingValue
    logger.warn("Ignoring malformed $OEM_COMPANION_SETTING_KEY from watch: '$settingValue'")
  }
  val propertyValue = getProperty(OEM_COMPANION_PROPERTY_KEY)
  if (!propertyValue.isNullOrBlank() && propertyValue != "null") {
    if (PACKAGE_NAME_REGEX.matches(propertyValue)) return propertyValue
    logger.warn("Ignoring malformed $OEM_COMPANION_PROPERTY_KEY from watch: '$propertyValue'")
  }
  return OEM_COMPANION_FALLBACK_APP_ID
}

public suspend fun AdbDevice.supportsCompanionAppId(companionAppId: String): CompanionSupport {
  if (companionAppId !in availableCompanionAppIds) return CompanionSupport.INCOMPATIBLE_COMPANION_ID
  if (companionAppId == OEM_COMPANION_FALLBACK_APP_ID && abis().singleOrNull() == "x86_64") return CompanionSupport.INCOMPATIBLE_ABI
  return CompanionSupport.SUPPORTED
}

public suspend fun AdbDevice.hasPairingFeature(pairingFeature: PairingFeature, companionAppId: String? = null): Boolean =
  when (pairingFeature) {
    PairingFeature.COMPANION_EMULATOR_ACTIVITY ->
      companionAppId != OEM_COMPANION_FALLBACK_APP_ID ||
        (getAppVersionCode(companionAppId) ?: 0) >= pairingFeature.minVersion
    PairingFeature.REFRESH_EMULATOR_CONNECTION,
    PairingFeature.GET_PAIRING_STATUS,
    PairingFeature.MULTI_WATCH_SINGLE_PHONE_PAIRING,
    PairingFeature.REVERSE_PORT_FORWARD,
    PairingFeature.WATCH_ACCOUNT_COPY -> (getAppVersionCode(GMSCORE_APP_ID) ?: 0) >= pairingFeature.minVersion
    PairingFeature.COMPANION_SKIP_AND_FINISH_FIXED ->
      companionAppId != OEM_COMPANION_FALLBACK_APP_ID ||
        (getAppVersionCode(OEM_COMPANION_FALLBACK_APP_ID) ?: 0) >= pairingFeature.minVersion
  }

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

import com.android.adblib.DeviceState
import com.example.android.wearable.emulatorpairing.AdbDevice
import com.example.android.wearable.emulatorpairing.PairingLogger
import com.example.android.wearable.emulatorpairing.getPairingStatus
import com.example.android.wearable.emulatorpairing.loadNodeID
import com.example.android.wearable.emulatorpairing.tools.AndroidTools
import com.example.android.wearable.emulatorpairing.tools.AndroidUi
import com.example.android.wearable.emulatorpairing.tools.UiNode
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/**
 * A Google account to sign in on an emulator, for installing the companion app from Play Store. Use a dedicated test account
 * without 2-step verification; otherwise the user is asked to finish the challenge on the emulator screen.
 */
public class TestAccount(
  public val email: String,
  /** Inline password, from `testaccount=email:password`. */
  internal val password: String? = null,
  /** Shell command printing the password, e.g. `pass show android/test-account`. */
  public val passwordCommand: String? = null,
) {
  init {
    require(password != null || passwordCommand != null) { "Configure a password for $email (testaccount=email:password or password.command)" }
  }

  override fun toString(): String = "TestAccount($email)"

  public companion object {
    public val CONFIG_DIR: Path = Paths.get(System.getProperty("user.home"), ".config/wear-pair")
    public val DEFAULT_CONFIG: Path = CONFIG_DIR.resolve("test-account.properties")

    /**
     * Loads from [file] (default `~/.config/wear-pair/test-account.properties`). Simplest form is one line:
     * ```
     * testaccount=your.testaccount@gmail.com:password123
     * ```
     * Env overrides: `WEAR_PAIR_TESTACCOUNT` (email:password), or `WEAR_PAIR_TEST_ACCOUNT_EMAIL` + `WEAR_PAIR_TEST_ACCOUNT_PASSWORD_COMMAND`.
     * Returns null when not configured.
     */
    public fun load(file: Path = DEFAULT_CONFIG, env: Map<String, String> = System.getenv()): TestAccount? {
      val props = Properties()
      if (file.exists()) file.inputStream().use { props.load(it) }
      fun value(key: String, envKey: String) = env[envKey]?.takeIf { it.isNotBlank() } ?: props.getProperty(key)?.takeIf { it.isNotBlank() }

      val combined = value("testaccount", "WEAR_PAIR_TESTACCOUNT")
      // Emails can't contain ':', so split on the first one; the password may contain ':'.
      val (combinedEmail, combinedPassword) = combined?.split(':', limit = 2)?.let { it[0].trim() to it.getOrNull(1) } ?: (null to null)
      val email = combinedEmail ?: value("email", "WEAR_PAIR_TEST_ACCOUNT_EMAIL") ?: return null
      return TestAccount(email, combinedPassword?.takeIf { it.isNotEmpty() }, value("password.command", "WEAR_PAIR_TEST_ACCOUNT_PASSWORD_COMMAND"))
    }
  }
}


/**
 * How the sign-in talks to the person running it when Google asks for extra verification. The default implementation only
 * reports messages; the person then finishes on the emulator screen.
 */
public interface SignInHelper {
  public fun notify(message: String)

  /** Pick one of [options]; return its index, or null to let the person handle it on the emulator screen. */
  public suspend fun choose(title: String, options: List<String>): Int? = null

  /** Ask for a verification code; return null to let the person type it on the emulator screen. */
  public suspend fun askCode(prompt: String): String? = null

  public companion object {
    public fun notifyOnly(block: (String) -> Unit): SignInHelper = object : SignInHelper {
      override fun notify(message: String) = block(message)
    }
  }
}

/** Screen-lock PIN set on emulators that have none (account copy to a watch requires a screen lock). */
public const val EMULATOR_PIN: String = "1234"

/** How long the person has to complete a verification challenge. */
public val USER_VERIFICATION_WINDOW: Duration = 20.minutes

/**
 * Signs a Google account in on an emulator by driving the standard "Add account" screens, using only public tooling:
 * `am start -a android.settings.ADD_ACCOUNT_SETTINGS`, the Android CLI's `android layout`, and `adb shell input`.
 *
 * Each loop iteration reads the screen and takes one action: skip the phone-number step, enter email, enter password, accept terms,
 * retry an expired session. Some accounts are asked to verify a new device ("Verify it's you"); then the person is told what to do
 * and given [USER_VERIFICATION_WINDOW], choosing a method and entering the code through [SignInHelper] (works with headless
 * emulators) or directly on the emulator screen.
 */
public class GoogleSignIn(private val tools: AndroidTools = AndroidTools(), private val logger: PairingLogger = PairingLogger.None) {

  /** Returns null on success, otherwise a reason. */
  public suspend fun signIn(
    device: AdbDevice,
    account: TestAccount,
    helper: SignInHelper = SignInHelper.notifyOnly { logger.warn(it) },
    timeout: Duration = 5.minutes,
  ): String? {
    if (hasAccount(device, account.email)) return null
    val ui = AndroidUi(tools, device)
    if (!ui.available) return "The Android CLI (`android`) is needed to drive the sign-in screens; sign in manually instead"
    if (device.isWatch()) return signInWatch(device, account, ui, helper)
    val password = account.password ?: readSecret(account.passwordCommand!!)
    if (password.contains('\'')) return "Passwords containing a single quote can't be typed via adb; sign in manually"

    device.shell("am start -a android.settings.ADD_ACCOUNT_SETTINGS --esa account_types com.google")
    var deadline = TimeSource.Monotonic.markNow() + timeout
    var typedEmail = false
    var typedPassword = false
    var verificationStarted = false
    var chosenMethod = false
    var lastCodeScreen = ""
    var lastDevicePrompt = ""
    var handedOff = false
    var lastScreen = ""
    while (deadline.hasNotPassedNow()) {
      if (hasAccount(device, account.email)) {
        logger.info("${account.email} is signed in on ${device.serialNumber}")
        if (verificationStarted) helper.notify("Verification complete — thanks, continuing automatically.")
        return null
      }
      val nodes = ui.nodes()
      val screen = nodes.mapNotNull { it.label }.joinToString("|")
      fun clickable(vararg labels: String): UiNode? = nodes.firstOrNull { it.clickable && it.labelIs(*labels) }
      fun hasText(fragment: String) = nodes.any { it.label?.contains(fragment, ignoreCase = true) == true }

      val emailField = nodes.firstOrNull { it.resourceId == "identifierId" }
      val passwordField = nodes.firstOrNull { it.isPassword }
      val isChallenge =
        hasText("Verify it") || hasText("2-Step Verification") || hasText("Choose how you want to sign in") || hasText("verification code")
      when {
        nodes.isEmpty() || hasText("Checking info") || hasText("Just a sec") -> Unit // loading

        // "X isn't responding" (slow first boot): keep waiting rather than killing the app.
        nodes.any { it.resourceId == "aerr_wait" } -> {
          logger.info("Dismissing an app-not-responding dialog (Wait)")
          ui.tap(nodes.first { it.resourceId == "aerr_wait" })
        }

        hasText("Sign in with ease") && clickable("SKIP", "Skip") != null -> {
          logger.info("Skipping the phone-number step")
          ui.tap(clickable("SKIP", "Skip")!!)
        }

        emailField != null && emailField.text?.trim()?.equals(account.email, ignoreCase = true) != true -> {
          logger.info("Entering email")
          ui.tap(emailField)
          if (!emailField.text.isNullOrBlank()) ui.clearFocusedField()
          ui.type(account.email)
          ui.enter() // Don't tap NEXT: the keyboard moves it.
          typedEmail = true
        }

        emailField != null -> ui.enter() // Correct email already entered; submit.

        hasText("Wrong password") -> return "Google rejected the password for ${account.email}"

        passwordField != null && !typedPassword -> {
          logger.info("Entering password")
          ui.tap(passwordField)
          ui.type(password)
          ui.enter()
          typedPassword = true
        }

        isChallenge -> {
          if (!verificationStarted) {
            verificationStarted = true
            deadline = TimeSource.Monotonic.markNow() + USER_VERIFICATION_WINDOW
            helper.notify(verificationBriefing(device))
          }
          val codeField = nodes.firstOrNull { it.label == null && it.clickable && it.resourceId != null } ?: nodes.firstOrNull { it.focused && it.label == null && it.clickable }
          val methods = nodes.filter { it.clickable && it.contentDesc != null && it.contentDesc != "Try another way" }
          // "Check your <device>": approve the notification on another signed-in device and tap the number shown here.
          val promptDevice = nodes.firstNotNullOfOrNull { n -> n.label?.let { Regex("^Check your (.+)$").find(it.trim())?.groupValues?.get(1) } }
          val matchNumber = nodes.firstNotNullOfOrNull { n -> n.label?.trim()?.takeIf { Regex("^\\d{1,3}$").matches(it) } }
          when {
            promptDevice != null && "$promptDevice/$matchNumber" == lastDevicePrompt -> Unit // waiting for approval
            promptDevice != null -> {
              lastDevicePrompt = "$promptDevice/$matchNumber"
              helper.notify(
                "Google sent a sign-in prompt to your $promptDevice. On it, tap Yes on the Google notification" +
                  (matchNumber?.let { ", then tap the number $it" } ?: "") +
                  ". No notification? Choose \"Try another way\" on the emulator, or re-run with --companion-apk instead."
              )
            }
            // A code entry screen: "Enter the code…" with an input field.
            ((hasText("Enter") && hasText("code")) || hasText("recovery email")) && codeField != null && screen != lastCodeScreen -> {
              lastCodeScreen = screen
              val askingEmail = hasText("recovery email") && !hasText("code")
              val code = helper.askCode(if (askingEmail) "Enter the account's recovery email address (or press Enter to type it on the emulator):" else "Enter the verification code Google just sent (or press Enter to type it on the emulator):")
              if (!code.isNullOrBlank() && askingEmail && !code.contains('\'')) {
                ui.tap(codeField)
                ui.type(code.trim())
                ui.enter()
              } else if (!code.isNullOrBlank() && code.all { it.isLetterOrDigit() || it == '-' || it == ' ' }) {
                ui.tap(codeField)
                ui.type(code.replace(" ", "").replace("-", ""))
                ui.enter()
              }
            }
            // The method picker.
            !chosenMethod && methods.isNotEmpty() -> {
              chosenMethod = true
              val options = methods.map { sanitizeMethod(it.label!!) }
              val index = helper.choose("How do you want to verify? (or press Enter to choose on the emulator)", options)
              if (index != null && index in methods.indices) {
                ui.tap(methods[index])
                if (options[index].contains("another phone", ignoreCase = true) || options[index].contains("another device", ignoreCase = true)) {
                  helper.notify("Approve the sign-in on your other device (phone or computer signed in to this account).")
                }
              }
            }
          }
        }

        clickable("TRY AGAIN", "Try again") != null -> {
          logger.info("Session expired; retrying")
          typedEmail = false
          typedPassword = false
          chosenMethod = false
          ui.tap(clickable("TRY AGAIN", "Try again")!!)
        }

        // Terms of service and Google services screens.
        clickable("I agree", "I AGREE", "Agree", "AGREE", "Accept", "ACCEPT") != null -> ui.tap(clickable("I agree", "I AGREE", "Agree", "AGREE", "Accept", "ACCEPT")!!)
        clickable("MORE", "More") != null -> ui.tap(clickable("MORE", "More")!!) // scrolls to reveal ACCEPT
        clickable("Not now", "NOT NOW", "No thanks", "NO THANKS", "Skip", "SKIP", "Don't turn on", "Cancel") != null &&
          (hasText("backup") || hasText("Back up") || hasText("recovery") || hasText("phone number") || hasText("payment")) ->
          ui.tap(clickable("Not now", "NOT NOW", "No thanks", "NO THANKS", "Skip", "SKIP", "Don't turn on", "Cancel")!!)

        else ->
          if (!handedOff && screen.isNotEmpty() && screen == lastScreen) {
            handedOff = true
            deadline = maxOf(deadline, TimeSource.Monotonic.markNow() + USER_VERIFICATION_WINDOW)
            val what = nodes.firstOrNull { it.label != null && !it.clickable }?.label?.let { sanitizeMethod(it) } ?: "an unrecognised screen"
            helper.notify(
              "Sign-in needs your help on ${device.serialNumber}: it's stuck at \"$what\". Please finish it on the emulator screen " +
                "within ${USER_VERIFICATION_WINDOW.inWholeMinutes} minutes; this will continue automatically once the account appears."
            )
          }
      }
      lastScreen = screen
      delay(if (typedEmail || typedPassword) 3_000 else 2_000)
    }
    return if (verificationStarted) "Verification wasn't completed within ${USER_VERIFICATION_WINDOW.inWholeMinutes} minutes for ${account.email}"
    else "Timed out after $timeout signing in ${account.email} on ${device.serialNumber}"
  }

  private suspend fun verificationBriefing(device: AdbDevice): String {
    val headless = device.isEmulator() && tools.androidCli != null
    return buildString {
      appendLine("Google wants to verify this sign-in (common for accounts with extra security, on a new emulator).")
      appendLine("In the next ${USER_VERIFICATION_WINDOW.inWholeMinutes} minutes you'll need to:")
      appendLine("  1. Choose a verification method below (email code, text code, or approve on another device).")
      appendLine("  2. Enter the code here when it arrives, or approve the prompt on your other device.")
      append("You can also do this directly on the emulator screen")
      if (headless) append(" (if it was started without a window, restart it with one: android emulator start ${device.avdName() ?: "<avd>"})")
      append(". Pairing continues automatically afterwards.")
    }
  }

  /**
   * Wear OS can't sign in on its own: Settings > Accounts > Add Google Account copies the account from the paired phone.
   * This drives that flow end to end on emulators: it sets a screen lock (PIN [EMULATOR_PIN]) on emulators without one (the copy
   * fails silently without it), taps "Open on phone" on the watch, then on the phone picks the account ("Continue as …") and
   * enters the PIN at "Verify it's you".
   *
   * Tested working with a Wear OS 7 (API 37) signed watch image; older watch images (Play services 24.x/25.x) fail during the
   * transfer.
   */
  private suspend fun signInWatch(watch: AdbDevice, account: TestAccount, ui: AndroidUi, helper: SignInHelper): String? {
    val notReady =
      "A watch can't sign in by itself: Wear OS copies the account from its paired phone. First pair it " +
        "(`wear-pair pair ...`), sign the phone in (`wear-pair sign-in <phone>`), then run `wear-pair sign-in ${watch.serialNumber}`."
    val watchPeers = watch.getPairingStatus().peers.filter { it.connected && it.enabled }.mapNotNull { it.nodeId?.lowercase() }
    if (watchPeers.isEmpty()) return notReady
    val phone =
      watch.session.hostServices.devices().filter { it.deviceState == DeviceState.ONLINE && it.serialNumber != watch.serialNumber }
        .map { AdbDevice(watch.session, it.serialNumber) }
        .firstOrNull { p -> p.loadNodeID().lowercase() in watchPeers } ?: return notReady
    if (!hasAccount(phone, account.email)) return "${phone.serialNumber} isn't signed in to ${account.email}; run `wear-pair sign-in ${phone.serialNumber}` first"
    for (d in listOf(phone, watch)) {
      if (d.isEmulator() && d.shell("dumpsys lock_settings | grep -m1 CredentialType").contains("NONE")) {
        d.shell("locksettings set-pin $EMULATOR_PIN")
        helper.notify("Set screen-lock PIN $EMULATOR_PIN on ${d.serialNumber} (needed to copy an account to the watch)")
      }
    }
    for (d in listOf(phone, watch)) {
      if (d.isEmulator() && !d.unlockWithPin(EMULATOR_PIN)) return "${d.serialNumber} is locked; unlock it and try again"
    }
    val phoneUi = AndroidUi(tools, phone)

    // Emulated watches report "charging", and Wear OS then shows a full-screen charging UI over everything.
    if (watch.isEmulator()) watch.shell("dumpsys battery unplug")
    watch.shell("svc power stayon true; input keyevent 224; wm dismiss-keyguard")
    watch.shell("am start -a android.settings.ADD_ACCOUNT_SETTINGS --esa account_types com.google")
    var openedOnPhone = false
    val deadline = TimeSource.Monotonic.markNow() + USER_VERIFICATION_WINDOW
    while (deadline.hasNotPassedNow()) {
      if (hasAccount(watch, account.email)) {
        logger.info("${account.email} is on ${watch.serialNumber}")
        if (watch.isEmulator()) watch.shell("dumpsys battery reset; svc power stayon false")
        return null
      }
      val nodes = ui.nodes()
      val add = nodes.firstOrNull { it.clickable && it.labelIs("Add Google Account", "Add account") }
      val openOnPhone = nodes.firstOrNull { it.resourceId == "top_container" || (it.clickable && it.label?.contains("Open on phone", ignoreCase = true) == true) }
      when {
        !openedOnPhone && add != null -> ui.tap(add)
        !openedOnPhone && openOnPhone != null -> {
          ui.tap(openOnPhone)
          openedOnPhone = true
          logger.info("Opened the account copy on ${phone.serialNumber}")
        }
      }
      if (openedOnPhone) {
        val p = phoneUi.nodes()
        val pick = p.firstOrNull { it.label?.startsWith("Continue as") == true } ?: p.firstOrNull { it.label?.trim() == account.email && it.clickable }
        when {
          p.any { it.label?.contains("Verify it", ignoreCase = true) == true || it.label?.contains("Unlock to continue", ignoreCase = true) == true } -> {
            logger.info("Entering the phone's PIN")
            phoneUi.type(EMULATOR_PIN)
            phoneUi.enter()
          }
          pick != null -> {
            logger.info("Choosing ${account.email} on ${phone.serialNumber}")
            phoneUi.tap(pick)
          }
          p.any { it.resourceId == "aerr_wait" } -> phoneUi.tap(p.first { it.resourceId == "aerr_wait" })
        }
      }
      delay(3_000)
    }
    return "${account.email} wasn't copied to ${watch.serialNumber} within ${USER_VERIFICATION_WINDOW.inWholeMinutes} minutes. " +
      "Account copy needs a recent watch image (tested: Wear OS 7 / API 37 signed); older images fail during the transfer."
  }

  private suspend fun hasAccount(device: AdbDevice, email: String) = device.shell("dumpsys account").contains("name=$email,")

  private suspend fun readSecret(command: String): String {
    val result = tools.run(listOf("/bin/sh", "-c", command), timeoutSeconds = 120)
    if (!result.ok) error("password.command failed (exit ${result.exitCode})")
    return result.stdout.trim()
  }

  internal companion object {
    /** Drops masked contact details (e.g. "y•••@ex••••.••", "•••• 12") so they aren't printed or logged. */
    fun sanitizeMethod(label: String): String {
      val masked = label.split(' ').any { '•' in it }
      return when {
        !masked -> label
        label.contains("code", ignoreCase = true) && label.contains('@') -> "Get a verification code by email"
        label.contains("code", ignoreCase = true) -> "Get a verification code by text message"
        else -> label.split(' ').filterNot { '•' in it }.joinToString(" ").trim()
      }
    }
  }
}

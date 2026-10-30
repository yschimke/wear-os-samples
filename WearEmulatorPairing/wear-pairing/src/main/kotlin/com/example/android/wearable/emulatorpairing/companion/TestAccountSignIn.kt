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
import com.example.android.wearable.emulatorpairing.tools.findClickable
import com.example.android.wearable.emulatorpairing.tools.hasScreenText
import com.example.android.wearable.emulatorpairing.tools.screenText
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
 * `am start -a android.settings.ADD_ACCOUNT_SETTINGS`, the Android CLI's `android layout` (or `uiautomator dump`), and `adb shell input`.
 *
 * Each loop iteration reads the screen and takes one action: dismiss ANR dialogs, skip the phone-number step, enter email, enter
 * password, accept terms, finish Google Services / contacts setup, or retry an expired session. Some accounts are asked to verify a new
 * device ("Verify it's you"); then the person is told what to do and given [USER_VERIFICATION_WINDOW], choosing a method and entering
 * the code through [SignInHelper] (works with headless emulators) or directly on the emulator screen.
 */
public class GoogleSignIn(private val tools: AndroidTools = AndroidTools(), private val logger: PairingLogger = PairingLogger.None) {

  /** Returns null on success, otherwise a reason. */
  public suspend fun signIn(
    device: AdbDevice,
    account: TestAccount,
    helper: SignInHelper = SignInHelper.notifyOnly { logger.warn(it) },
    timeout: Duration = 5.minutes,
  ): String? {
    if (device.isEmulator() && device.isLocked()) device.unlockWithPin(EMULATOR_PIN)
    if (hasAccount(device, account.email)) return null
    val ui = AndroidUi(tools, device)
    if (device.isWatch()) return signInWatch(device, account, ui, helper)
    val password = account.password ?: readSecret(account.passwordCommand!!)

    device.shell("am start -a android.settings.ADD_ACCOUNT_SETTINGS --esa account_types com.google")
    delay(2_000)
    var deadline = TimeSource.Monotonic.markNow() + timeout
    var accountAddedAt: TimeSource.Monotonic.ValueTimeMark? = null
    var emailAttempts = 0
    var passwordAttempts = 0
    var justSubmittedPassword = false
    var verificationStarted = false
    var chosenMethod = false
    var lastCodeScreen = ""
    var lastDevicePrompt = ""
    var handedOff = false
    var lastScreen = ""
    while (deadline.hasNotPassedNow()) {
      val signedIn = hasAccount(device, account.email)
      if (signedIn && accountAddedAt == null) {
        accountAddedAt = TimeSource.Monotonic.markNow()
        logger.info("${account.email} is signed in on ${device.serialNumber}")
        if (verificationStarted) helper.notify("Verification complete — thanks, continuing automatically.")
      }

      val nodes = ui.nodes()
      val screen = nodes.mapNotNull { it.label }.joinToString("|")
      fun clickable(vararg labels: String): UiNode? = nodes.findClickable(*labels)
      fun hasText(fragment: String): Boolean = nodes.hasScreenText(fragment)

      if (signedIn && (!isPostSignInWizard(nodes) || accountAddedAt!!.elapsedNow() > 25.seconds)) {
        return null
      }

      val anrWait = nodes.firstOrNull { it.resourceId == "aerr_wait" } ?: if (hasText("isn't responding")) clickable("Wait") else null
      val emailField =
        nodes.firstOrNull { it.resourceId == "identifierId" }
          ?: if (!signedIn && hasText("Email or phone") && !hasText("password")) nodes.firstOrNull { it.clickable && it.focused && it.label == null } else null
      val passwordField = nodes.firstOrNull { it.isPassword }
      val accountNotFound =
        hasText("Couldn't find your Google Account") || hasText("Couldn't find this account") || hasText("Enter a valid email")
      val isChallenge =
        !signedIn &&
          (hasText("Verify it") ||
            hasText("2-Step Verification") ||
            hasText("Choose how you want to sign in") ||
            hasText("verification code") ||
            hasText("Check your ") ||
            hasText("Tap Yes on the notification"))

      when {
        // Always dismiss "X isn't responding" first (even over loading or password screens) so it can't steal input focus.
        anrWait != null -> {
          logger.info("Dismissing an app-not-responding dialog (Wait)")
          justSubmittedPassword = false
          ui.tap(anrWait)
        }

        nodes.isEmpty() || hasText("Checking info") || hasText("Just a sec") || hasText("Signing in") || hasText("One moment") -> {
          justSubmittedPassword = false
        }

        hasText("Sign in with ease") && clickable("SKIP", "Skip") != null -> {
          logger.info("Skipping the phone-number step")
          ui.tap(clickable("SKIP", "Skip")!!)
        }

        accountNotFound && emailField != null -> {
          if (emailField.text?.trim()?.equals(account.email, ignoreCase = true) == true && emailAttempts >= 2) {
            return "Google couldn't find the account ${account.email}"
          }
          logger.info("Re-entering email (previous input didn't match ${account.email})")
          ui.focusAndType(emailField, account.email, clearFirst = true)
          ui.enter()
          emailAttempts++
        }

        emailField != null && emailField.text?.trim()?.equals(account.email, ignoreCase = true) != true -> {
          logger.info("Entering email")
          ui.focusAndType(emailField, account.email, clearFirst = !emailField.text.isNullOrBlank() || emailAttempts > 0)
          ui.enter() // Don't tap NEXT first: the soft keyboard moves it.
          emailAttempts++
        }

        emailField != null -> {
          val next = clickable("NEXT", "Next")
          if (emailAttempts > 1 && next != null) ui.tap(next) else ui.enter()
          emailAttempts++
        }

        hasText("Wrong password") -> return "Google rejected the password for ${account.email}"

        passwordField != null && justSubmittedPassword -> {
          // Allow one extra poll for WebView navigation before concluding the password submission was dropped.
          justSubmittedPassword = false
        }

        passwordField != null && passwordAttempts < 3 -> {
          logger.info("Entering password" + if (passwordAttempts > 0) " (attempt ${passwordAttempts + 1})" else "")
          ui.focusAndType(passwordField, password, clearFirst = passwordAttempts > 0)
          val next = clickable("NEXT", "Next")
          if (passwordAttempts > 0 && next != null) ui.tap(next) else ui.enter()
          passwordAttempts++
          justSubmittedPassword = true
        }

        isChallenge -> {
          if (!verificationStarted) {
            verificationStarted = true
            deadline = TimeSource.Monotonic.markNow() + USER_VERIFICATION_WINDOW
            helper.notify(verificationBriefing(device))
          }
          val codeField =
            nodes.firstOrNull { it.label == null && it.clickable && it.resourceId != null }
              ?: nodes.firstOrNull { it.focused && it.label == null && it.clickable }
          val methods = extractVerificationMethods(nodes)
          val devicePrompt = extractDevicePrompt(nodes)
          when {
            devicePrompt != null && "${devicePrompt.first}/${devicePrompt.second}" == lastDevicePrompt -> Unit
            devicePrompt != null -> {
              val (promptDevice, matchNumber) = devicePrompt
              lastDevicePrompt = "$promptDevice/$matchNumber"
              helper.notify(
                "Google sent a sign-in prompt to your $promptDevice. On it, tap Yes on the Google notification" +
                  (matchNumber?.let { ", then tap the number $it" } ?: "") +
                  ". No notification? Choose \"Try another way\" on the emulator, or re-run with --companion-apk instead."
              )
            }
            // A code entry screen: "Enter the code…" or recovery email confirmation.
            ((hasText("Enter") && hasText("code")) || hasText("recovery email")) && codeField != null && screen != lastCodeScreen -> {
              lastCodeScreen = screen
              val askingEmail = hasText("recovery email") && !hasText("code")
              val code =
                helper.askCode(
                  if (askingEmail) "Enter the account's recovery email address (or press Enter to type it on the emulator):"
                  else "Enter the verification code Google just sent (or press Enter to type it on the emulator):"
                )
              if (!code.isNullOrBlank() && askingEmail) {
                ui.focusAndType(codeField, code.trim(), clearFirst = true)
                ui.enter()
              } else if (!code.isNullOrBlank() && code.all { it.isLetterOrDigit() || it == '-' || it == ' ' }) {
                ui.focusAndType(codeField, code.replace(" ", "").replace("-", ""), clearFirst = true)
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
                if (options[index].contains("another phone", ignoreCase = true) || options[index].contains("another device", ignoreCase = true) ||
                  options[index].contains("Tap Yes", ignoreCase = true)
                ) {
                  helper.notify("Approve the sign-in on your other device (phone or computer signed in to this account).")
                }
              }
            }
          }
        }

        clickable("TRY AGAIN", "Try again") != null -> {
          logger.info("Session expired; retrying")
          emailAttempts = 0
          passwordAttempts = 0
          justSubmittedPassword = false
          chosenMethod = false
          ui.tap(clickable("TRY AGAIN", "Try again")!!)
        }

        // Regional / DMA personalization & service-linking choice screens.
        (hasText("Choose your settings") || hasText("Before you continue") || hasText("Link your Google services")) &&
          clickable("Reject all", "Keep current choices", "Confirm", "Save and continue", "Continue") != null ->
          ui.tap(clickable("Reject all", "Keep current choices", "Confirm", "Save and continue", "Continue")!!)

        // Optional post-sign-in prompts (contacts backup, device backup, recovery phone, passkey, payment).
        clickable("Not now", "NOT NOW", "No thanks", "NO THANKS", "Skip", "SKIP", "Don't turn on", "DON'T TURN ON", "Cancel") != null &&
          (hasText("backup") ||
            hasText("Back up") ||
            hasText("recovery") ||
            hasText("phone number") ||
            hasText("payment") ||
            hasText("contacts") ||
            hasText("sync") ||
            hasText("passkey") ||
            hasText("Simplify your sign-in")) ->
          ui.tap(clickable("Not now", "NOT NOW", "No thanks", "NO THANKS", "Skip", "SKIP", "Don't turn on", "DON'T TURN ON", "Cancel")!!)

        // Terms of service and Google Services screens ("More" scrolls to reveal "Accept").
        clickable("I agree", "I AGREE", "Agree", "AGREE", "Accept", "ACCEPT") != null ->
          ui.tap(clickable("I agree", "I AGREE", "Agree", "AGREE", "Accept", "ACCEPT")!!)
        clickable("MORE", "More") != null -> ui.tap(clickable("MORE", "More")!!)
        signedIn && clickable("Done", "DONE", "Close", "Got it") != null -> ui.tap(clickable("Done", "DONE", "Close", "Got it")!!)

        !signedIn ->
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
      delay(if (justSubmittedPassword || emailAttempts == 1) 3_000 else 2_000)
    }
    if (hasAccount(device, account.email)) return null
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
    try {
      watch.shell("am start -a android.settings.ADD_ACCOUNT_SETTINGS --esa account_types com.google")
      var openedOnPhone = false
      val deadline = TimeSource.Monotonic.markNow() + USER_VERIFICATION_WINDOW
      while (deadline.hasNotPassedNow()) {
        if (hasAccount(watch, account.email)) {
          logger.info("${account.email} is on ${watch.serialNumber}")
          return null
        }
        val nodes = ui.nodes()
        val add = nodes.findClickable("Add Google Account", "Add account")
        val openOnPhone =
          nodes.firstOrNull { it.resourceId == "top_container" }
            ?: nodes.firstOrNull { it.clickable && it.label?.contains("Open on phone", ignoreCase = true) == true }
            ?: nodes.findClickable("Open on phone")
        when {
          nodes.any { it.resourceId == "aerr_wait" } -> ui.tap(nodes.first { it.resourceId == "aerr_wait" })
          !openedOnPhone && add != null -> ui.tap(add)
          !openedOnPhone && openOnPhone != null -> {
            ui.tap(openOnPhone)
            openedOnPhone = true
            logger.info("Opened the account copy on ${phone.serialNumber}")
          }
        }
        if (openedOnPhone) {
          val p = phoneUi.nodes()
          val pick =
            p.firstOrNull { it.label?.startsWith("Continue as", ignoreCase = true) == true }
              ?: p.firstOrNull { it.label?.trim().equals(account.email, ignoreCase = true) && it.clickable }
              ?: p.findClickable(account.email, "Continue", "Copy")
          when {
            p.any { it.resourceId == "aerr_wait" } -> phoneUi.tap(p.first { it.resourceId == "aerr_wait" })
            p.hasScreenText("Verify it") || p.hasScreenText("Unlock to continue") || p.hasScreenText("Confirm your PIN") || p.hasScreenText("Enter your PIN") -> {
              logger.info("Entering the phone's PIN")
              phoneUi.type(EMULATOR_PIN)
              phoneUi.enter()
            }
            pick != null -> {
              logger.info("Choosing ${account.email} on ${phone.serialNumber}")
              phoneUi.tap(pick)
            }
          }
        }
        delay(3_000)
      }
      return "${account.email} wasn't copied to ${watch.serialNumber} within ${USER_VERIFICATION_WINDOW.inWholeMinutes} minutes. " +
        "Account copy needs a recent watch image (tested: Wear OS 7 / API 37 signed); older images fail during the transfer."
    } finally {
      withContext(NonCancellable) {
        if (watch.isEmulator()) watch.shell("dumpsys battery reset; svc power stayon false")
      }
    }
  }

  private suspend fun hasAccount(device: AdbDevice, email: String) =
    device.shell("dumpsys account").contains("name=$email,", ignoreCase = true)

  private suspend fun readSecret(command: String): String {
    val result = tools.run(listOf("/bin/sh", "-c", command), timeoutSeconds = 120)
    if (!result.ok) error("password.command failed (exit ${result.exitCode})")
    return result.stdout.trim()
  }

  internal companion object {
    private val CHROME_LABELS =
      setOf("try another way", "more ways to sign in", "help", "learn more", "privacy", "terms", "back", "next", "cancel", "resend it")

    /** True while post-sign-in wizard steps (Google Services, contacts sync, backup, passkey, consent) are still on screen. */
    internal fun isPostSignInWizard(nodes: List<UiNode>): Boolean =
      nodes.any { it.resourceId == "aerr_wait" } ||
        nodes.hasScreenText("Google Services") ||
        nodes.hasScreenText("Terms of Service") ||
        nodes.hasScreenText("Never lose your contacts") ||
        nodes.hasScreenText("Back up") ||
        nodes.hasScreenText("Simplify your sign-in") ||
        nodes.hasScreenText("Choose your settings") ||
        nodes.hasScreenText("Before you continue") ||
        nodes.hasScreenText("Checking info") ||
        nodes.hasScreenText("Just a sec") ||
        nodes.findClickable("I agree", "Agree", "Accept", "More") != null

    /** Extracts `(deviceName, matchNumber)` from a Google device-approval prompt, even when WebView splits words across nodes. */
    internal fun extractDevicePrompt(nodes: List<UiNode>): Pair<String, String?>? {
      val combined = nodes.screenText()
      val device =
        nodes.firstNotNullOfOrNull { n -> n.label?.let { Regex("""^Check your (.+?)[.!]*$""", RegexOption.IGNORE_CASE).find(it.trim())?.groupValues?.get(1) } }
          ?: Regex("""Check your ([A-Za-z0-9][A-Za-z0-9 _\-]{0,35}?)(?:[.!,]| Google| Tap| Open| to | and |$)""", RegexOption.IGNORE_CASE)
            .find(combined)
            ?.groupValues
            ?.get(1)
            ?.trim()
          ?: if (nodes.hasScreenText("Tap Yes") || nodes.hasScreenText("Google sent a notification")) "phone or other signed-in device" else null
      if (device == null) return null
      val matchNumber =
        Regex("""\btap (?:the number )?(\d{1,3})\b""", RegexOption.IGNORE_CASE).find(combined)?.groupValues?.get(1)
          ?: nodes.firstNotNullOfOrNull { n -> n.label?.trim()?.takeIf { Regex("""^\d{1,3}$""").matches(it) && n.y > 120 } }
      return device to matchNumber
    }

    /** Returns selectable verification method rows, excluding navigation/help links. */
    internal fun extractVerificationMethods(nodes: List<UiNode>): List<UiNode> =
      nodes.filter { node ->
        val label = node.label?.trim() ?: return@filter false
        node.clickable &&
          label.length >= 6 &&
          label.lowercase() !in CHROME_LABELS &&
          !label.startsWith("Learn more", ignoreCase = true) &&
          !label.equals("Verify it's you", ignoreCase = true) &&
          !label.equals("2-Step Verification", ignoreCase = true)
      }

    /** Drops masked contact details (e.g. "y•••@ex••••.••", "•••• 12") so they aren't printed or logged. */
    fun sanitizeMethod(label: String): String {
      val masked = label.split(' ').any { '•' in it || '*' in it }
      return when {
        !masked -> label
        label.contains("code", ignoreCase = true) && label.contains('@') -> "Get a verification code by email"
        label.contains("code", ignoreCase = true) -> "Get a verification code by text message"
        else -> label.split(' ').filterNot { '•' in it || '*' in it }.joinToString(" ").trim()
      }
    }
  }
}

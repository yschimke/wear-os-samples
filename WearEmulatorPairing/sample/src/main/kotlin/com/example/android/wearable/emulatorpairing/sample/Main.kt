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
package com.example.android.wearable.emulatorpairing.sample

import com.android.adblib.AdbSession
import com.android.adblib.AdbSessionHost
import com.android.adblib.DeviceState
import com.example.android.wearable.emulatorpairing.AdbDevice
import com.example.android.wearable.emulatorpairing.GMSCORE_APP_ID
import com.example.android.wearable.emulatorpairing.PairDiscovery
import com.example.android.wearable.emulatorpairing.PairingFeature
import com.example.android.wearable.emulatorpairing.PairingLogger
import com.example.android.wearable.emulatorpairing.PairingOptions
import com.example.android.wearable.emulatorpairing.PairingProgress
import com.example.android.wearable.emulatorpairing.PairingResult
import com.example.android.wearable.emulatorpairing.WearPairing
import com.example.android.wearable.emulatorpairing.avd.AvdCatalog
import com.example.android.wearable.emulatorpairing.avd.Severity
import com.example.android.wearable.emulatorpairing.avd.hostAbi
import com.example.android.wearable.emulatorpairing.companion.SignInHelper
import com.example.android.wearable.emulatorpairing.checkWearMayNeedFactoryReset
import com.example.android.wearable.emulatorpairing.connectionServiceDump
import com.example.android.wearable.emulatorpairing.companion.CompanionInstallEvent
import com.example.android.wearable.emulatorpairing.companion.CompanionInstaller
import com.example.android.wearable.emulatorpairing.companion.CompanionSource
import com.example.android.wearable.emulatorpairing.companion.TestAccount
import com.example.android.wearable.emulatorpairing.companion.GoogleSignIn
import com.example.android.wearable.emulatorpairing.companion.describe
import com.example.android.wearable.emulatorpairing.getAppVersionCode
import com.example.android.wearable.emulatorpairing.getCompanionAppIdForWatch
import com.example.android.wearable.emulatorpairing.hasPairingFeature
import com.example.android.wearable.emulatorpairing.isCompanionAppInstalled
import com.example.android.wearable.emulatorpairing.isPairingStatusAvailable
import com.example.android.wearable.emulatorpairing.supportsCompanionAppId
import com.example.android.wearable.emulatorpairing.tools.AndroidTools
import com.example.android.wearable.emulatorpairing.tools.EmulatorControl
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

private const val USAGE =
  """usage: wear-pair <command> [args] [flags]

Discover
  avds                          AVDs on this machine: which can pair, why not, and how to fix
  list                          online devices (phone/watch)
  paired                        which online phones and watches are paired / bridged
  doctor  [phone] [watch]       live checks with fixes (companion app, Play Store, account, GMS Core, ...)

Pair
  create                        create + boot a new phone and watch AVD from installed images, then pair them
  pair    [phone] [watch]       pair (auto-detects one phone + one watch)
  status  [phone] [watch]       is the pair connected?
  bridge  [phone] [watch]       re-create the adb bridge for an already-paired pair (after an emulator restart)
  keep    [phone] [watch]       re-bridge automatically whenever both come back online (Ctrl-C to stop)
  disconnect [phone] [watch]    remove the adb bridge only; `bridge` reconnects
  unpair  [phone] [watch]       fully unpair: bridge, phone-side pairing (companion app data) and the
                                watch's pairing state (resets its Google Play services data; --keep-watch to skip,
                                --wipe-watch to wipe the whole watch instead). On physical devices only the
                                bridge is removed unless --force

Setup
  start <avd> [--headless]      start an emulator (Android CLI) and wait for boot
  enable-playstore <avd>        set PlayStore.enabled=yes for an AVD whose image has Google Play
  install-companion [phone]     install the companion app ahead of time (needs a source flag). Uses the online
                                watch's companion, or --package <id> (default: Pixel Watch app)
  sign-in [serial]              sign the configured Google test account in on a phone ahead of time; on a watch,
                                copy it from the paired phone

Companion source flags (pair / install-companion; tried in this order)
  --companion-apk <file|dir>    local APK, or a directory of split APKs (repeatable)
  --companion-from <serial>     copy the app from another device that has it
  --test-account                sign in ~/.config/wear-pair/test-account.properties, then install from Play Store
  --play                        open sign-in / Play Store on the phone and tap Install (Android CLI)
Other flags
  --phone-avd <name> --watch-avd <name>   names for `create` (default Wear_Pair_Phone / Wear_Pair_Watch)
  --wipe-watch                  if the watch is paired with another phone, wipe and cold-boot it
  --accept-terms                accept the companion app's Terms of Service with all optional boxes unchecked
                                (a legal agreement for the signed-in account: only with the owner's consent)
  --window                      show the window when (re)starting emulators
  --verbose                     doctor/paired: include the Wearable connection-manager dump per device
Environment
  ANDROID_PHONE_SERIAL, ANDROID_WATCH_SERIAL   default serials when not passed
  NO_COLOR                      disable coloured output"""

private class Args(argv: Array<String>) {
  val positional = mutableListOf<String>()
  val flags = mutableSetOf<String>()
  val multi = mutableMapOf<String, MutableList<String>>()

  init {
    var i = 0
    while (i < argv.size) {
      val a = argv[i]
      when {
        a in setOf("--companion-apk", "--companion-from", "--phone-avd", "--watch-avd", "--package") -> multi.getOrPut(a) { mutableListOf() } += argv.getOrElse(++i) { usage("$a needs a value") }
        a.startsWith("--") -> flags += a
        else -> positional += a
      }
      i++
    }
  }
}

private fun usage(message: String? = null): Nothing {
  message?.let { System.err.println("error: $it\n") }
  System.err.println(USAGE)
  exitProcess(2)
}

fun main(argv: Array<String>): Unit = runBlocking {
  val args = Args(argv)
  val command = args.positional.firstOrNull() ?: usage()
  val rest = args.positional.drop(1)
  val tools = AndroidTools()
  val catalog = AvdCatalog(tools.locations)

  // Offline commands
  when (command) {
    "avds" -> return@runBlocking printAvds(catalog)
    "enable-playstore" -> {
      val avd = catalog.avds().firstOrNull { it.name == rest.firstOrNull() } ?: usage("unknown AVD '${rest.firstOrNull()}'; see `wear-pair avds`")
      catalog.enablePlayStore(avd)
      println("Set PlayStore.enabled=yes for ${avd.name} (restart the emulator if it's running)")
      return@runBlocking
    }
    "help", "--help", "-h" -> usage()
  }

  AdbSessionHost().use { host ->
    AdbSession.create(host).use { session ->
      val sources = companionSources(args)
      val options =
        PairingOptions(
          companionSources = sources,
          wipeWatchIfPairedElsewhere = "--wipe-watch" in args.flags,
          acceptCompanionTerms = "--accept-terms" in args.flags,
          headless = "--window" !in args.flags,
          signInHelper = ConsoleSignInHelper,
        )
      val pairing = WearPairing(session, options, PairingLogger.Stderr, tools)

      when (command) {
        "list" ->
          session.hostServices.devices().forEach {
            val d = pairing.device(it.serialNumber)
            val kind = if (it.deviceState != DeviceState.ONLINE) "-" else if (d.isWatch()) "watch" else "phone"
            println("${it.serialNumber}\t${it.deviceState}\t$kind\t${if (it.deviceState == DeviceState.ONLINE) d.avdName().orEmpty() else ""}")
          }
        "paired" -> {
          printPaired(PairDiscovery(session))
          if ("--verbose" in args.flags) printConnectionDumps(onlineDevices(session, pairing))
        }
        "create" -> {
          val phoneImage = catalog.suitablePhoneImages().firstOrNull()
            ?: usage("no installed Google Play phone image (API 30+). Install one: android sdk install \"system-images;android-36;google_apis_playstore;${hostAbi()}\"")
          val watchImage = catalog.suitableWatchImages().firstOrNull()
            ?: usage("no installed Wear OS image. Install one: android sdk install \"system-images;android-36;android-wear-signed;${hostAbi()}\"")
          val phoneName = args.multi["--phone-avd"]?.last() ?: "Wear_Pair_Phone"
          val watchName = args.multi["--watch-avd"]?.last() ?: "Wear_Pair_Watch"
          catalog.createAvd(phoneName, phoneImage)
          catalog.createAvd(watchName, watchImage)
          println("Created $phoneName (${phoneImage.packageId}) and $watchName (${watchImage.packageId})")
          val control = EmulatorControl(session, tools, PairingLogger.Stderr)
          val headless = "--window" !in args.flags
          val phone = control.start(phoneName, headless) ?: run { System.err.println("$phoneName did not boot"); exitProcess(1) }
          val watch = control.start(watchName, headless) ?: run { System.err.println("$watchName did not boot"); exitProcess(1) }
          println("phone=${phone.serialNumber} watch=${watch.serialNumber}")
          pair(WearPairing(session, options, PairingLogger.Stderr, tools), phone, watch)
        }
        "start" -> {
          val avd = rest.firstOrNull() ?: usage("start needs an AVD name")
          val device = EmulatorControl(session, tools, PairingLogger.Stderr).start(avd, headless = "--headless" in args.flags)
          println(device?.let { "$avd is running as ${it.serialNumber}" } ?: "Timed out starting $avd")
        }
        "install-companion" -> {
          if (sources.isEmpty()) usage("install-companion needs a source: --companion-apk, --companion-from, --test-account or --play")
          val phone = rest.firstOrNull()?.let { pairing.device(it) } ?: singleOnline(session, pairing, watch = false)
          val watch = onlineDevices(session, pairing).filter { it.isWatch() }.singleOrNull()
          val appId = args.multi["--package"]?.last() ?: watch?.getCompanionAppIdForWatch() ?: com.example.android.wearable.emulatorpairing.PIXEL_COMPANION_APP_ID
          val ok = CompanionInstaller(tools, PairingLogger.Stderr, ConsoleSignInHelper).install(phone, appId, sources, ::printCompanionEvent)
          println(if (ok) "$appId installed on ${phone.serialNumber}" else "Could not install $appId")
          if (!ok) exitProcess(1)
        }
        "sign-in" -> {
          val target = rest.firstOrNull()?.let { pairing.device(it) } ?: singleOnline(session, pairing, watch = false)
          val account = TestAccount.load() ?: usage("no test account configured; see config/test-account.properties.example")
          val failure = GoogleSignIn(tools, PairingLogger.Stderr).signIn(target, account, ConsoleSignInHelper)
          println(failure?.let { "Sign-in failed: $it" } ?: "${account.email} is signed in on ${target.serialNumber}")
          if (failure != null) exitProcess(1)
        }
        else -> {
          val (phone, watch) = if (rest.size >= 2) pairing.device(rest[0]) to pairing.device(rest[1]) else detect(session, pairing)
          println("phone=${phone.serialNumber} watch=${watch.serialNumber}")
          when (command) {
            "pair" -> pair(pairing, phone, watch)
            "doctor" -> {
              doctor(catalog, phone, watch, sources)
              if ("--verbose" in args.flags) printConnectionDumps(listOf(phone, watch))
            }
            "status" -> println(if (pairing.isPaired(phone, watch)) "paired" else "not paired")
            "bridge" -> println("bridged on host port ${pairing.bridge(phone, watch)}; paired=${pairing.isPaired(phone, watch)}")
            "disconnect" -> pairing.disconnect(phone, watch).also { println("disconnected (run `wear-pair bridge` to reconnect)") }
            "unpair" -> {
              val steps = pairing.unpair(phone, watch, resetWatch = "--keep-watch" !in args.flags && "--wipe-watch" !in args.flags, force = "--force" in args.flags)
              steps.forEach { println("  - $it") }
              if ("--wipe-watch" in args.flags) {
                val wiped = EmulatorControl(session, tools, PairingLogger.Stderr).wipeAndRestart(watch, headless = "--window" !in args.flags)
                println(wiped?.let { "  - Wiped and restarted ${it.serialNumber}" } ?: "  - Wiping ${watch.serialNumber} failed")
              }
              println("unpaired")
            }
            "keep" -> pairing.keepBridged(phone, watch) { paired -> println("re-bridged, paired=$paired") }
            else -> usage("unknown command '$command'")
          }
        }
      }
    }
  }
}

private fun companionSources(args: Args): List<CompanionSource> = buildList {
  args.multi["--companion-apk"]?.let { add(CompanionSource.LocalApks(it.map { p -> Paths.get(p) })) }
  args.multi["--companion-from"]?.forEach { add(CompanionSource.CopyFromDevice(it)) }
  if ("--test-account" in args.flags) {
    val account = TestAccount.load() ?: usage("--test-account: nothing configured in ${TestAccount.DEFAULT_CONFIG}; see config/test-account.properties.example")
    add(CompanionSource.TestAccountThenPlayStore(account))
  }
  if ("--play" in args.flags) add(CompanionSource.PlayStore())
}

private suspend fun onlineDevices(session: AdbSession, pairing: WearPairing): List<AdbDevice> =
  session.hostServices.devices().filter { it.deviceState == DeviceState.ONLINE }.map { pairing.device(it.serialNumber) }

private suspend fun singleOnline(session: AdbSession, pairing: WearPairing, watch: Boolean): AdbDevice {
  val matches = onlineDevices(session, pairing).filter { it.isWatch() == watch }
  if (matches.size != 1) usage("expected exactly one ${if (watch) "watch" else "phone"} online, found ${matches.joinToString()}; pass a serial")
  return matches.single()
}

private suspend fun detect(session: AdbSession, pairing: WearPairing): Pair<AdbDevice, AdbDevice> {
  val envPhone = System.getenv("ANDROID_PHONE_SERIAL")?.takeIf { it.isNotBlank() }
  val envWatch = System.getenv("ANDROID_WATCH_SERIAL")?.takeIf { it.isNotBlank() }
  if (envPhone != null && envWatch != null) return pairing.device(envPhone) to pairing.device(envWatch)
  val online = onlineDevices(session, pairing)
  val (detectedWatches, detectedPhones) = online.partition { it.isWatch() }
  val watches = envWatch?.let { listOf(pairing.device(it)) } ?: detectedWatches
  val phones = envPhone?.let { listOf(pairing.device(it)) } ?: detectedPhones
  if (watches.size != 1 || phones.size != 1) {
    System.err.println("Expected exactly one phone and one watch online, found phones=$phones watches=$watches. Pass serials explicitly.")
    System.err.println("Tip: `wear-pair avds` lists AVDs that can pair; `wear-pair start <avd>` starts one.")
    exitProcess(2)
  }
  return phones.single() to watches.single()
}

private suspend fun pair(pairing: WearPairing, phone: AdbDevice, watch: AdbDevice) {
  val result =
    pairing.pair(phone, watch) { progress ->
      when (progress) {
        is PairingProgress.Companion -> printCompanionEvent(progress.event)
        is PairingProgress.CompanionState -> println("companion (attempt ${progress.attempt}): ${progress.state}")
        is PairingProgress.ManualPairingRequired -> {
          println("Automatic pairing didn't complete. On the phone:")
          progress.instructions.forEachIndexed { i, s -> println("  ${i + 1}. $s") }
        }
        is PairingProgress.RebootingWatch -> println("Rebooting ${progress.serial} so its emulator transport accepts a phone…")
        is PairingProgress.WipingWatch -> println("Watch ${progress.serial} is paired with another phone: wiping and cold-booting it…")
        is PairingProgress.RepairingGmsCore -> println("Waiting for Google Play services on ${progress.serial} to initialise…")
        else -> println(progress)
      }
    }
  when (result) {
    is PairingResult.Paired -> {
      println("Paired (host port ${result.hostPort}, companion ${result.companionAppId})")
      if (result.needsSkipAndFinish) println("If the companion app is still on a setup screen, tap 'Skip and finish'.")
    }
    is PairingResult.Failure -> {
      System.err.println("Pairing failed: ${result.message}")
      if (result.fixes.isNotEmpty()) System.err.println("Fixes:")
      result.fixes.forEach { System.err.println("  - $it") }
      exitProcess(1)
    }
  }
}

private fun printCompanionEvent(event: CompanionInstallEvent) {
  when (event) {
    is CompanionInstallEvent.Trying -> println("Installing companion: ${event.source.describe()}")
    is CompanionInstallEvent.UserActionRequired -> println("ACTION NEEDED: ${event.instructions}")
    is CompanionInstallEvent.SourceFailed -> println("  ${event.source.describe()} failed: ${event.reason}")
  }
}

private fun printAvds(catalog: AvdCatalog) {
  val avds = catalog.avds()
  if (avds.isEmpty()) println("No AVDs in ${catalog.locations.avdHome}")
  for ((title, list) in listOf("Phones" to avds.filter { !it.isWear }.map(catalog::assessPhone), "Watches" to avds.filter { it.isWear }.map(catalog::assessWatch))) {
    println("$title:")
    for (a in list) {
      val verdict = when {
        a.studioAccepts -> "OK"
        a.usable -> "OK for wear-pair (Studio would hide it)"
        else -> "CAN'T PAIR"
      }
      println("  ${a.avd.name}  [API ${a.avd.api ?: "?"}, ${a.avd.tagIds.joinToString()}, ${a.avd.abi}]  $verdict")
      for (p in a.problems) {
        println("      ${if (p.severity == Severity.ERROR) Marks.bad else Marks.warn} ${p.message}")
        p.fix?.let { println("        fix: $it") }
      }
    }
    if (list.isEmpty()) println("  (none)")
  }
  println("\nInstalled images that can make a phone (Google Play): ${catalog.suitablePhoneImages().joinToString { it.packageId }.ifEmpty { "none" }}")
  println("Installed images that can make a watch: ${catalog.suitableWatchImages().joinToString { it.packageId }.ifEmpty { "none" }}")
}

private fun printPaired(discovery: PairDiscovery) = runBlocking {
  val report = discovery.discover()
  if (report.pairs.isEmpty()) println("No paired or bridged phone/watch pairs among online devices.")
  for (p in report.pairs) {
    println("${p.phone.label}  <->  ${p.watch.label}   ${p.state}${p.hostPort?.let { "   (bridge tcp:$it)" } ?: "   (no adb bridge)"}")
  }
  for (f in report.foreign) println("${f.watch.label} is paired with a phone that isn't online (node ${f.peerNodeId}, connected=${f.connected})")
  if (report.unpairedPhones.isNotEmpty()) println("Unpaired phones: ${report.unpairedPhones.joinToString { it.label }}")
  if (report.unpairedWatches.isNotEmpty()) println("Unpaired watches: ${report.unpairedWatches.joinToString { it.label }}")
}

private suspend fun doctor(catalog: AvdCatalog, phone: AdbDevice, watch: AdbDevice, sources: List<CompanionSource>) {
  var problems = 0
  fun ok(msg: String) = println("  ${Marks.ok} $msg")
  fun bad(msg: String, vararg fixes: String) {
    problems++
    println("  ${Marks.bad} $msg")
    fixes.forEach { println("      fix: $it") }
  }

  println("Phone ${phone.serialNumber} (${phone.avdName() ?: "physical"})")
  val sdk = phone.getProperty("ro.build.version.sdk")?.toIntOrNull() ?: 0
  if (sdk >= 30) ok("API $sdk") else bad("API $sdk < 30", catalog.recommendPhoneImageFix())
  phone.avdName()?.let { name -> catalog.avds().firstOrNull { it.name == name } }?.let { avd ->
    catalog.assessPhone(avd).problems.filter { it.severity == Severity.STUDIO_ONLY }.forEach { println("  ! ${it.message}\n      fix: ${it.fix}") }
  }
  val companionAppId = watch.getCompanionAppIdForWatch()
  val hasPlay = phone.shell("pm path com.android.vending").isNotBlank()
  val hasAccount = CompanionInstaller.hasGoogleAccount(phone)
  if (phone.isCompanionAppInstalled(companionAppId)) {
    ok("companion $companionAppId installed")
  } else {
    val support = phone.supportsCompanionAppId(companionAppId)
    bad(
      "companion $companionAppId (required by the watch) is not installed ($support)",
      *buildList {
        add("--companion-apk <file|dir>  or  --companion-from <serial>")
        if (hasPlay) add(if (hasAccount) "--play (account signed in; will open Play Store and tap Install)" else "--play (you'll be asked to sign in)  or  --test-account")
        else add("no Play Store on this image: ${catalog.recommendPhoneImageFix()}")
      }.toTypedArray(),
    )
  }
  if (hasPlay) ok("Play Store present") else println("  ! no Play Store (only matters if the companion isn't installed)")
  if (hasAccount) ok("Google account signed in") else println("  ! no Google account signed in (needed for Play Store installs; use --test-account or sign in)")
  if (phone.hasPairingFeature(PairingFeature.MULTI_WATCH_SINGLE_PHONE_PAIRING)) ok("Google Play services ${phone.getAppVersionCode(GMSCORE_APP_ID)}")
  else bad("Google Play services too old (${phone.getAppVersionCode(GMSCORE_APP_ID)})", catalog.recommendPhoneImageFix())
  if (phone.isPairingStatusAvailable()) ok("Wearable pairing status available")
  else println("  ! Wearable service not reporting pairing status yet (normal until the companion is installed; pair will nudge GMS Core)")

  println("Watch ${watch.serialNumber} (${watch.avdName() ?: "physical"})")
  val wsdk = watch.getProperty("ro.build.version.sdk")?.toIntOrNull() ?: 0
  if (wsdk >= 28) ok("API $wsdk") else bad("API $wsdk < 28", catalog.recommendWatchImageFix())
  if (watch.hasPairingFeature(PairingFeature.REVERSE_PORT_FORWARD)) ok("Google Play services ${watch.getAppVersionCode(GMSCORE_APP_ID)}")
  else bad("Google Play services too old (${watch.getAppVersionCode(GMSCORE_APP_ID)})", catalog.recommendWatchImageFix())
  if (checkWearMayNeedFactoryReset(phone, watch)) bad("watch is paired with a different phone", "wear-pair pair --wipe-watch")
  else ok("not paired with another phone")

  if (sources.isNotEmpty()) println("Companion sources: ${sources.joinToString { it.describe() }}")
  TestAccount.load()?.let { println("Test account configured: ${it.email}") }
  println(if (problems == 0) "\nNo blocking problems." else "\n$problems blocking problem(s).")
}

/** Prompts in the terminal when Google asks to verify a sign-in; falls back to on-screen instructions without a console. */
private object ConsoleSignInHelper : SignInHelper {
  /** A terminal, or WEAR_PAIR_INTERACTIVE=1 to read answers from stdin (e.g. when driven by an agent). */
  private val interactive = System.console() != null || System.getenv("WEAR_PAIR_INTERACTIVE") == "1"

  override fun notify(message: String) = println("\n>>> $message\n")

  override suspend fun choose(title: String, options: List<String>): Int? {
    if (!interactive) return null
    println(title)
    options.forEachIndexed { i, o -> println("  ${i + 1}) $o") }
    println("> ")
    val line = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { readlnOrNull() }?.trim().orEmpty()
    return line.toIntOrNull()?.minus(1)?.takeIf { it in options.indices }
  }

  override suspend fun askCode(prompt: String): String? {
    if (!interactive) return null
    println("$prompt ")
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { readlnOrNull() }?.trim()?.takeIf { it.isNotEmpty() }
  }
}

/** Coloured status marks when stdout is a terminal and NO_COLOR isn't set (https://no-color.org). */
private object Marks {
  private val color = System.getenv("NO_COLOR").isNullOrEmpty() && (System.console() != null || System.getenv("FORCE_COLOR").let { it != null && it != "0" })
  private fun c(code: String, s: String) = if (color) "\u001b[${code}m$s\u001b[0m" else s
  val ok = c("32", "✓")
  val bad = c("31", "✗")
  val warn = c("33", "!")
}

private suspend fun printConnectionDumps(devices: List<AdbDevice>) {
  for (d in devices) {
    println("\n--- ConnectionServiceManager (${d.serialNumber}) ---")
    println(d.connectionServiceDump().lines().joinToString("\n") { "    $it" })
  }
}

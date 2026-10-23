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
package com.example.android.wearable.emulatorpairing.tools

import com.example.android.wearable.emulatorpairing.avd.SdkLocations
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.isExecutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String) {
  val ok: Boolean
    get() = exitCode == 0
}

/**
 * External tools: the Android CLI (`android`), `adb` and `emulator`. adb device I/O normally goes through adblib; these are
 * used for things adblib doesn't do (installing APKs, pulling files) and for AVD management.
 */
public class AndroidTools(public val locations: SdkLocations = SdkLocations.detect(), env: Map<String, String> = System.getenv()) {
  /** The Android CLI (https://developer.android.com/tools/agents), if installed. */
  public val androidCli: Path? =
    findOnPath("android", env)?.takeIf { !it.toString().contains("/tools/android") } // not the legacy SDK `tools/android`
      ?: Paths.get(System.getProperty("user.home"), ".local/bin/android").takeIf { it.isExecutable() }

  public val adb: Path? = locations.sdkRoot?.resolve("platform-tools/adb")?.takeIf { it.isExecutable() } ?: findOnPath("adb", env)

  public val emulator: Path? = locations.sdkRoot?.resolve("emulator/emulator")?.takeIf { it.isExecutable() } ?: findOnPath("emulator", env)

  public suspend fun run(vararg command: String, timeoutSeconds: Long = 600): ProcessResult = run(command.toList(), timeoutSeconds)

  public suspend fun run(command: List<String>, timeoutSeconds: Long = 600): ProcessResult =
    withContext(Dispatchers.IO) {
      val process = ProcessBuilder(command).redirectInput(ProcessBuilder.Redirect.from(java.io.File("/dev/null"))).start()
      val out = async { process.inputStream.bufferedReader().readText() }
      val err = async { process.errorStream.bufferedReader().readText() }
      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        ProcessResult(-1, out.get(), "Timed out after ${timeoutSeconds}s: ${command.joinToString(" ")}\n${err.get()}")
      } else {
        ProcessResult(process.exitValue(), out.get(), err.get())
      }
    }

  /** Starts a long-running process (e.g. the emulator) without waiting for it. */
  public fun spawn(command: List<String>, log: Path? = null): Process {
    val builder = ProcessBuilder(command).redirectErrorStream(true)
    if (log != null) builder.redirectOutput(log.toFile()) else builder.redirectOutput(ProcessBuilder.Redirect.DISCARD)
    return builder.start()
  }

  public fun requireAdb(): String = (adb ?: error("adb not found; install platform-tools (android sdk install platform-tools)")).toString()

  public fun requireEmulator(): String = (emulator ?: error("emulator not found; install it (android sdk install emulator)")).toString()

  private fun <T> async(block: () -> T): java.util.concurrent.Future<T> =
    java.util.concurrent.CompletableFuture.supplyAsync(block)

  private companion object {
    fun findOnPath(name: String, env: Map<String, String>): Path? =
      env["PATH"].orEmpty().split(java.io.File.pathSeparator).filter { it.isNotEmpty() }.map { Paths.get(it, name) }.firstOrNull { it.isExecutable() }
  }
}

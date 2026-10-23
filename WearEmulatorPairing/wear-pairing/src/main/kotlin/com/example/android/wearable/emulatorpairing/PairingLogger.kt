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

/** Minimal logging hook, so the library doesn't force a logging framework on callers. */
public fun interface PairingLogger {
  public fun log(level: Level, message: String)

  public fun info(message: String): Unit = log(Level.INFO, message)

  public fun warn(message: String): Unit = log(Level.WARN, message)

  public enum class Level {
    DEBUG,
    INFO,
    WARN,
  }

  public companion object {
    public val None: PairingLogger = PairingLogger { _, _ -> }
    public val Stderr: PairingLogger = PairingLogger { level, message -> System.err.println("[$level] $message") }
  }
}

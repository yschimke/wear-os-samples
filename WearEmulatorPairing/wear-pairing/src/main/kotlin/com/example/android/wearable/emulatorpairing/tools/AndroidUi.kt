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

import com.example.android.wearable.emulatorpairing.AdbDevice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** One element from `android layout` (flat JSON list). */
public data class UiNode(
  val text: String?,
  val contentDesc: String?,
  val resourceId: String?,
  val interactions: Set<String>,
  val state: Set<String>,
  val x: Int,
  val y: Int,
) {
  val label: String? = text ?: contentDesc
  val clickable: Boolean = "clickable" in interactions
  val isPassword: Boolean = "password" in interactions
  val focused: Boolean = "focused" in state

  public fun labelIs(vararg labels: String): Boolean = label?.trim()?.let { l -> labels.any { it.equals(l, ignoreCase = true) } } == true
}

/** Screen inspection + input on top of the Android CLI's `android layout` and `adb shell input`. */
public class AndroidUi(private val tools: AndroidTools, private val device: AdbDevice) {
  public val available: Boolean = tools.androidCli != null

  public suspend fun nodes(): List<UiNode> {
    val cli = tools.androidCli ?: return emptyList()
    val result = tools.run(cli.toString(), "layout", "--device=${device.serialNumber}", timeoutSeconds = 60)
    return if (result.ok) parseLayout(result.stdout) else emptyList()
  }

  public suspend fun tap(node: UiNode) {
    device.shell("input tap ${node.x} ${node.y}")
  }

  /** Types [text] into the focused field. Not for text containing a single quote. */
  public suspend fun type(text: String) {
    require(!text.contains('\'')) { "Can't type text containing a single quote via adb" }
    device.shell("input text '${text.replace(" ", "%s")}'")
  }

  public suspend fun enter() {
    device.shell("input keyevent 66")
  }

  public suspend fun clearFocusedField(maxChars: Int = 80) {
    device.shell("input keyevent 123") // MOVE_END
    device.shell("input keyevent " + List(maxChars) { "67" }.joinToString(" ")) // DEL
  }

  public companion object {
    public fun parseLayout(json: String): List<UiNode> {
      val array = runCatching { Json.parseToJsonElement(json.trim()).jsonArray }.getOrNull() ?: return emptyList()
      return array.filterIsInstance<JsonObject>().mapNotNull { node ->
        val (x, y) = Regex("""\[(\d+),(\d+)]""").find(node["center"]?.jsonPrimitive?.content ?: return@mapNotNull null)?.destructured
          ?: return@mapNotNull null
        UiNode(
          text = node["text"]?.jsonPrimitive?.content,
          contentDesc = node["content-desc"]?.jsonPrimitive?.content,
          resourceId = node["resource-id"]?.jsonPrimitive?.content,
          interactions = node["interactions"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty(),
          state = node["state"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty(),
          x = x.toInt(),
          y = y.toInt(),
        )
      }
    }
  }
}

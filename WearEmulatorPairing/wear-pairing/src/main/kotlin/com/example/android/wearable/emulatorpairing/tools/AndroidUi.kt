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
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.Element
import org.xml.sax.InputSource

/** Screen rectangle `[left,top][right,bottom]`. */
public data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
  public val width: Int
    get() = right - left

  public val height: Int
    get() = bottom - top

  public fun contains(x: Int, y: Int): Boolean = x in left..right && y in top..bottom
}

/** One element from `android layout` (flat JSON list) or `uiautomator dump` XML. */
public data class UiNode(
  val text: String?,
  val contentDesc: String?,
  val resourceId: String?,
  val interactions: Set<String>,
  val state: Set<String>,
  val x: Int,
  val y: Int,
  val bounds: Bounds? = null,
) {
  val label: String? = text?.takeIf { it.isNotEmpty() } ?: contentDesc?.takeIf { it.isNotEmpty() }
  val clickable: Boolean = "clickable" in interactions
  val checkable: Boolean = "checkable" in interactions
  val checked: Boolean = "checked" in state
  val isPassword: Boolean = "password" in interactions
  val focused: Boolean = "focused" in state

  public fun labelIs(vararg labels: String): Boolean =
    listOfNotNull(text, contentDesc).map { it.trim() }.any { l -> l.isNotEmpty() && labels.any { it.equals(l, ignoreCase = true) } }
}

/** Concatenated visible text across all nodes, for matching multi-word phrases split across WebView nodes. */
public fun List<UiNode>.screenText(): String = mapNotNull { it.label?.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

/** True when [fragment] appears in any individual node label or across adjacent nodes on screen. */
public fun List<UiNode>.hasScreenText(fragment: String): Boolean =
  any { it.label?.contains(fragment, ignoreCase = true) == true } || screenText().contains(fragment, ignoreCase = true)

/**
 * Finds a button matching one of [labels]. Handles both classic Views (where the labeled node itself is `clickable`) and
 * Compose / WebView layouts (where an outer `clickable` container has no text and its child carries the label at the visual button
 * position).
 */
public fun List<UiNode>.findClickable(vararg labels: String): UiNode? {
  firstOrNull { it.clickable && it.labelIs(*labels) }?.let { return it }
  val clickables = filter { it.clickable }
  return filter { !it.clickable && it.labelIs(*labels) }.firstOrNull { child ->
    clickables.any { parent ->
      (parent.bounds != null && parent.bounds.height <= 400 && parent.bounds.contains(child.x, child.y)) ||
        (parent.label == null && abs(parent.y - child.y) <= 70 && abs(parent.x - child.x) <= 460)
    }
  }
}

/** Screen inspection + input on top of the Android CLI's `android layout` (falling back to `uiautomator dump`) and `adb shell input`. */
public class AndroidUi(private val tools: AndroidTools, private val device: AdbDevice) {
  public val available: Boolean = true

  public suspend fun nodes(): List<UiNode> {
    tools.androidCli?.let { cli ->
      val result = tools.run(cli.toString(), "layout", "--device=${device.serialNumber}", timeoutSeconds = 45)
      if (result.ok) {
        val parsed = parseLayout(result.stdout)
        if (parsed.isNotEmpty()) return parsed
      }
    }
    val xml = device.shell("uiautomator dump /data/local/tmp/wp_ui.xml >/dev/null 2>&1 && cat /data/local/tmp/wp_ui.xml")
    return parseUiAutomatorXml(xml)
  }

  public suspend fun tap(node: UiNode) {
    tap(node.x, node.y)
  }

  public suspend fun tap(x: Int, y: Int) {
    device.shell("input tap $x $y")
  }

  /** Types [text] into the focused field, escaping spaces and single quotes for `/system/bin/sh`. */
  public suspend fun type(text: String) {
    device.shell("input text ${escapeForShellInput(text)}")
  }

  /** Focuses [node] (waiting briefly for WebView / soft-keyboard layout to settle), optionally clears it, and types [text]. */
  public suspend fun focusAndType(node: UiNode, text: String, clearFirst: Boolean = false) {
    if (!node.focused) {
      tap(node)
      delay(600)
    }
    if (clearFirst) {
      clearFocusedField()
      delay(200)
    }
    type(text)
    delay(300)
  }

  public suspend fun enter() {
    device.shell("input keyevent 66")
  }

  public suspend fun clearFocusedField(maxChars: Int = 40) {
    // Ctrl+A + DEL clears native EditText and Chromium WebView inputs in one shot; trailing DELs handle fields that ignore Ctrl+A.
    val dels = List(maxChars) { "67" }.joinToString(" ")
    device.shell("input keycombination 113 29; input keyevent 67; input keyevent 123; input keyevent $dels")
  }

  public companion object {
    private val CENTER_REGEX = Regex("""\[(\d+),(\d+)]""")
    private val BOUNDS_REGEX = Regex("""\[(\d+),(\d+)]\[(\d+),(\d+)]""")
    private val INTERACTION_ATTRS = listOf("clickable", "focusable", "checkable", "scrollable", "long-clickable", "password")
    private val STATE_ATTRS = listOf("focused", "checked", "selected")

    internal fun escapeForShellInput(text: String): String = "'${text.replace(" ", "%s").replace("'", "'\\''")}'"

    internal fun parseBounds(raw: String?): Bounds? {
      val (l, t, r, b) = BOUNDS_REGEX.find(raw ?: return null)?.destructured ?: return null
      return Bounds(l.toInt(), t.toInt(), r.toInt(), b.toInt())
    }

    public fun parseLayout(json: String): List<UiNode> {
      val array = runCatching { Json.parseToJsonElement(json.trim()).jsonArray }.getOrNull() ?: return emptyList()
      return array.filterIsInstance<JsonObject>().mapNotNull { node ->
        val bounds = parseBounds(node["bounds"]?.jsonPrimitive?.content)
        val centerMatch = CENTER_REGEX.find(node["center"]?.jsonPrimitive?.content.orEmpty())
        val x = centerMatch?.groupValues?.get(1)?.toInt() ?: bounds?.let { (it.left + it.right) / 2 } ?: return@mapNotNull null
        val y = centerMatch?.groupValues?.get(2)?.toInt() ?: bounds?.let { (it.top + it.bottom) / 2 } ?: return@mapNotNull null
        UiNode(
          text = node["text"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
          contentDesc = node["content-desc"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
          resourceId = node["resource-id"]?.jsonPrimitive?.content?.substringAfter(":id/")?.takeIf { it.isNotEmpty() },
          interactions = node["interactions"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty(),
          state = node["state"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty(),
          x = x,
          y = y,
          bounds = bounds,
        )
      }
    }

    public fun parseUiAutomatorXml(raw: String): List<UiNode> {
      val start = raw.indexOf("<hierarchy").takeIf { it >= 0 } ?: return emptyList()
      val xml = raw.substring(start)
      val doc =
        runCatching {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
          }
          .getOrNull() ?: return emptyList()
      val list = doc.getElementsByTagName("node")
      return buildList {
        for (i in 0 until list.length) {
          val el = list.item(i) as? Element ?: continue
          val bounds = parseBounds(el.getAttribute("bounds")) ?: continue
          if (bounds.width <= 0 || bounds.height <= 0) continue
          val interactions = INTERACTION_ATTRS.filter { el.getAttribute(it) == "true" }.toSet()
          val state = STATE_ATTRS.filter { el.getAttribute(it) == "true" }.toSet()
          val text = el.getAttribute("text").takeIf { it.isNotEmpty() }
          val desc = el.getAttribute("content-desc").takeIf { it.isNotEmpty() }
          val resId = el.getAttribute("resource-id").substringAfter(":id/").takeIf { it.isNotEmpty() }
          if (text == null && desc == null && resId == null && interactions.isEmpty() && state.isEmpty()) continue
          add(
            UiNode(
              text = text,
              contentDesc = desc,
              resourceId = resId,
              interactions = interactions,
              state = state,
              x = (bounds.left + bounds.right) / 2,
              y = (bounds.top + bounds.bottom) / 2,
              bounds = bounds,
            )
          )
        }
      }
    }
  }
}

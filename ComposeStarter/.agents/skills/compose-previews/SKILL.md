---
name: preview
description: Render Compose @Preview functions and view the generated images. Use this to verify UI changes, iterate on designs, and compare before/after states.
---

# Compose Preview

Render `@Preview` composables to PNG images to verify UI changes visually.

## MCP Tools Available

The compose-preview MCP server exposes these tools:

| Tool | Description |
|------|-------------|
| `listPreviews` | Discover all @Preview functions. Optional `fileName` filter. |
| `renderPreview` | Render a preview. Returns JSON with file path by default. Params: `previewName`, `variantIndex` (0-based), `inline` (true for base64 image). |
| `getPreviewStatus` | Get compilation state, watched previews, last render times. |
| `compareSnapshots` | Compare two historical snapshots of a preview. |
| `listPreviewHistory` | List timestamped snapshots for a preview. |
| `getPreviewSnapshot` | Retrieve a specific historical snapshot image. |
| `watchPreview` | Start watching source files for auto-re-render. |
| `unwatchPreview` | Stop watching. |
| `compileModule` | Compile the module and report errors. |

## Workflow: Iterate on a Design

1. **List previews** to see what's available:
   ```
   Use listPreviews tool, optionally with fileName filter
   ```

2. **Render a preview** to see the current state:
   ```
   Use renderPreview with previewName (and variantIndex for multi-preview)
   The response includes a "file" path — read it to view the image.
   ```

3. **Make your code change** to the composable

4. **Re-render** — the server auto-detects changes, recompiles, and reloads:
   ```
   Use renderPreview again — it auto-compiles
   ```

5. **Compare** before and after if snapshots exist:
   ```
   Use compareSnapshots with previewName, indexA=1, indexB=0
   ```

6. **Check status** to see what's been rendered:
   ```
   Use getPreviewStatus — shows compile state, render times, watched previews
   ```

## Image Output Modes

### File path (default) — preferred for agents
`renderPreview` returns JSON with a `file` field pointing to the rendered PNG on disk:
```json
{"variantIndex": 0, "label": "...", "file": "/absolute/path/to/preview.png"}
```
Read the file to view the image. This avoids transferring large base64 blobs.

### Inline base64 — for embedded display
Pass `inline: true` to get the image as base64 data in the response:
```
renderPreview(previewName: "MyPreview", inline: true)
```
Returns an `ImageContent` block with the PNG data. Use this when you need to embed the image directly (e.g., in a webview) rather than reading a file.

### CLI equivalents
- `--json-files` → file paths (default for agents)
- `--json` → base64 inline

## CLI Alternative

If the MCP server isn't running, use the CLI directly:

```bash
# Render all previews
cli/bin/preview-cli show --project <path> --json-files

# Render specific function
cli/bin/preview-cli show --project <path> --filter <FunctionName> --json-files

# Render single variant
cli/bin/preview-cli show --project <path> --filter <FunctionName> --variant 0 --json-files
```

Read the `file` path from the JSON output to view the rendered PNG.

## Multi-Preview Annotations

Functions can have multiple `@Preview` annotations via multi-preview annotations:
- `@WearPreviewDevices` → Large Round (227×227dp) + Small Round (192×192dp)
- `@WearPreviewFontScales` → 7 font scale variants (0.94x–1.24x)

Use `variantIndex` (0-based) with `renderPreview` to select a specific variant.

## Setting Up the Gradle Plugin

The target Android project needs the `ee.schimke.composepreview.plugin` Gradle plugin applied. This plugin registers a `collectPreviewInfo` task that writes classpath, manifest, resource, and R.jar paths needed by the renderer.

### settings.gradle

```groovy
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/yschimke/precognition")
            credentials {
                username = "yschimke"
                password = "FROM_YURI"
            }
        }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

### app/build.gradle.kts

```kotlin
plugins {
    // ... existing plugins
    id("ee.schimke.composepreview.plugin") version "0.1.0"
}
```

### Requirements

The target app must have these dependencies (typically already present in Compose projects):

```kotlin
testImplementation("org.robolectric:robolectric:4.16.1")
```

### What the plugin does

The plugin registers a `collectPreviewInfo` Gradle task that:
1. Resolves `debugUnitTestRuntimeClasspath` to find all dependency JARs
2. Scans the build directory for compiled class directories (debug + debugUnitTest)
3. Locates the merged AndroidManifest.xml, resource APK, and R.jar
4. Writes all paths to `app/build/preview/project-info.txt`

This file is read by the CLI and MCP server to set up the Robolectric renderer classpath.

## Design Guidance

When creating or iterating on Wear OS designs, refer to the **[Wear UI Guide](./WEAR_UI.md)** for:
- **Material 3 Expressive** principles and best practices.
- Recommended **Compose Material 3** APIs (e.g., `TransformingLazyColumn`, `EdgeButton`).
- Proper **System UI** integration (e.g., `TimeText`, `AppScaffold`).
- **Responsive Layout** strategies for various screen sizes.

## Tips

- First render ~3-5s (harness startup). Subsequent renders ~100-200ms.
- The server uses `--auto-watch` by default — previews auto-update on file changes.
- Resource changes (.xml, .json) are detected and trigger recompilation + re-render.
- Always visually verify after making UI changes. Don't assume the change looks correct.
- When talking about a screen, at minimum show the user the preview also. 
- Maybe show previous versions to compare. And show something before and after you make changes.
- use a border occasionally if you want to highlight something to the user. or a box with a canvas, and draw something on top.

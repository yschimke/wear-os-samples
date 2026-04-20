# Remote Compose Migration Guide & Learnings

This guide summarizes the learnings and steps for adding Remote Compose components and previews to a Wear OS project, based on our experience with the Alarm Tile.

## Setup and Dependencies

### 1. Apply the Plugin
Apply the `ee.schimke.composeai.preview` plugin in your module's `build.gradle.kts`:
```kotlin
plugins {
    id("ee.schimke.composeai.preview") version "0.7.3"
}
```

### 2. Add Dependencies
Add the Remote Compose dependencies. We found that using specific alpha versions worked best:
- `androidx.compose.remote:remote-tooling-preview:1.0.0-alpha07`
- `androidx.compose.remote:remote-creation:1.0.0-alpha07`
- `androidx.compose.remote:remote-creation-compose:1.0.0-alpha07`
- `androidx.wear.compose.remote:remote-material3:1.0.0-alpha02`

### 3. Handle Lint Restrictions
Remote Compose APIs are often marked as restricted. To avoid build failures:
- Suppress the warning at the top of your Kotlin files:
  ```kotlin
  @file:Suppress("RestrictedApiAndroidX")
  ```
- Disable the check in your module's `build.gradle.kts`:
  ```kotlin
  android {
      lint {
          disable += "RestrictedApi"
      }
  }
  ```

### 4. SDK Version Compatibility
Remote Compose may require compiling against a newer SDK (e.g., API 37).
- If you encounter errors about `compileSdk` being too low, update it in `build.gradle.kts`.
- If your SDK is installed as `android-37.0` and AGP looks for `android-37`, you can use the deprecated function as a workaround to specify the exact string:
  ```kotlin
  android {
      compileSdkVersion("android-37.0")
  }
  ```

## Authoring Components

### 1. Target Markers
Always use both `@Composable` and `@RemoteComposable` annotations for your Remote Compose widgets:
```kotlin
@RemoteComposable
@Composable
fun MyWidget() {
    // ...
}
```

### 2. Containers and Backgrounds
If a specific `RemoteCard` component is not available, `RemoteButton` acts as a great container that provides a background and shape, mimicking a card in Wear Material 3.

### 3. Typography
Use `RemoteMaterialTheme.typography` to access Material 3 typography tokens:
```kotlin
RemoteText(
    text = "Heading".rs,
    style = RemoteMaterialTheme.typography.titleMedium
)
```
Note the use of the `.rs` extension on strings to create `RemoteString`.

### 4. Alignment
To center text or components horizontally inside a `RemoteColumn`, use `RemoteAlignment.CenterHorizontally`:
```kotlin
RemoteColumn(
    horizontalAlignment = RemoteAlignment.CenterHorizontally
) {
    // ...
}
```

### 5. Studying Protolayout for Styles
To understand the styles and layouts used in existing tiles (like `titleCard`), you can study the source code of Protolayout Material 3 components. For example, check [Card.kt](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:wear/protolayout/protolayout-material3/src/main/java/androidx/wear/protolayout/material3/Card.kt) to see how `titleCard` is constructed and what defaults it uses.

## Design Rules

### 1. Single Card Layout
Always fit the widget information into a single Card (e.g., using `RemoteButton` as container). Avoid separating information into multiple pills or rows outside the card unless explicitly requested. For example, include dates in the timeline row rather than a separate top pill.

## Previews

### 1. Framing
To avoid previews being jammed into the top-left corner of the canvas, use a filling container with centered alignment in your preview function:
```kotlin
@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun MyWidgetPreview() {
    RemotePreview(profile = RcPlatformProfiles.ANDROIDX) {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize(),
            contentAlignment = RemoteAlignment.Center
        ) {
            MyWidget()
        }
    }
}
```
Ensure that `RemoteModifier.fillMaxSize()` is used on the widget or container to make it take space.

## Remote Material3 Composables

Based on our usage and the API surface we explored, here are the available components in `androidx.wear.compose.remote.material3`:
- `RemoteText`: Used for displaying text with Material 3 typography.
- `RemoteButton`: Used as a clickable container or button. Can be styled with `RemoteButtonColors`.
- `RemoteCircularProgressIndicator`: Used for showing progress (e.g., steps goal).
- `RemoteMaterialTheme`: Used to access typography tokens (and potentially color schemes).

Note: Standard linear layouts like `RemoteBox`, `RemoteColumn`, and `RemoteRow` are available in `androidx.compose.remote.creation.compose.layout`, not in the material3 package.

## Learnings and Gotchas

- **Progress Indicator**: `RemoteCircularProgressIndicator` expects a `RemoteFloat` for the `progress` parameter. Use `.rf` to convert a float literal or calculation (e.g., `0.5f.rf`).
- **Icon Usage**: `RemoteIcon` requires an `ImageVector` or `RemoteImageVector`. It does not accept raw resource names or IDs directly in the constructor. If standard Material icons are not available, consider using a `RemoteText` with a character (like "+") or emoji as a workaround.
- **Text Color**: `RemoteText` uses a `color` parameter (expecting `RemoteColor`) to set text color directly. It does not use `textColor`.
- **Layout Weights**: `weight()` is an extension function on `RemoteModifier` available within `RemoteRowScope` (and likely `RemoteColumnScope`). It works similarly to standard Compose to distribute space proportionally.
- **Visibility in Previews**: If a component appears blank in previews, ensure it has a defined width (e.g., using `fillMaxWidth()` or `weight()`) or its parent container has space. Texts without explicit width or weight might resolve to zero width in some renderer conditions.

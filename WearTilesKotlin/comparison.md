# Tile vs Widget Comparison

This file compares the original Tile designs with the new Remote Compose widgets and critiques the differences.

## Alarm Tile

### Original Tile Preview
![Original Alarm Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.AlarmKt.alarmPreview_Large Round 1.00f.png)

### New Widget Preview
![New Alarm Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.AlarmWidgetKt.AlarmWidgetPreview.png)

### Critique
- **Data**: The original tile shows the specific time "2:58 PM" and the repeat days "Mon—Fri". The widget is simplified to show only "Mon—Fri" and "PM" as requested, losing the specific time.
- **Structure**: The original has a title card for the alarm info and a separate "+" button at the bottom. The widget combines the info into a single card (RemoteButton) and omits the plus button to fit the "single card" rule.
- **Style**: The typography matches well using `RemoteMaterialTheme`. The background color is now dark purple (`0xFF312E5C`) and corners are rounded to `16.rdp`, matching the target design style.

---

## Calendar Tile

### Original Tile Preview
![Original Calendar Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.CalendarKt.calendar1Preview_Large Round 1.00f.png)

### New Widget Preview
![New Calendar Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.CalendarWidgetKt.CalendarWidgetPreview.png)

### Critique
- **Data**: The widget uses the exact text data from the original preview, and includes the date "25 July" in the timeline as requested.
- **Structure**: The original tile has a `ButtonGroup` at the top with the date and an add button, and a `TitleCard` below. The widget follows the "single card" rule by moving the date into the timeline row ("25 July, 6:30-7:30 PM") and fitting everything into a single `RemoteButton` container.
- **Style**: The text is center-aligned in the widget, matching the original tile and the mockup. Colors are dark purple and corners are rounded, matching the target design style.

---

## Goal Tile

### Original Tile Preview
![Original Goal Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.GoalKt.goalPreview_Large Round 1.00f.png)

### New Widget Preview
![New Goal Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.GoalWidgetKt.GoalWidgetPreview.png)

### Critique
- **Data**: The widget uses the exact text data from the original preview ("Steps", "5,168", "of 8,000").
- **Structure**: The original tile has a top title "Steps", a graphic data card with progress indicator and icon, and a bottom button "Track". The widget follows the "single card" rule by fitting all text info into a single card and omitting the graphic and bottom button.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded, matching the target design style.

---

## Media Tile

### Original Tile Preview
![Original Media Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.MediaKt.mediaPreview_Large Round 1.00f.png)

### New Widget Preview
![New Media Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.MediaWidgetKt.MediaWidgetPreview.png)

### Critique
- **Data**: The widget uses the exact text data from the original preview ("Last played", "Metal mix", "Chilled mix").
- **Structure**: The original tile has a title "Last played", a column with two buttons for playlists (with background images), and a bottom button "Browse". The widget follows the "single card" rule by fitting all text info into a single card and omitting the images and bottom button.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded, matching the target design style.

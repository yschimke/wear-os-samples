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

---

## Hike Tile

### Original Tile Preview
![Original Hike Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.HikeKt.hikePreview_Large Round 1.00f.png)

### New Widget Preview
![New Hike Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.HikeWidgetKt.HikeWidgetPreview.png)

### Critique
- **Data**: The widget uses the exact text data from the original preview ("Hike", "10", "Miles").
- **Structure**: The original tile has a title "Hike", a button group with a text data card and an image button, and a bottom button "Start". The widget simplifies this to a single card with text and omits the image button and bottom button to fit the "single card" rule.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded.

---

## Meditation Tile

### Original Tile Preview
![Original Meditation Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.MeditationKt.mindfulnessPreview_Large Round 1.00f.png)

### New Widget Preview
![New Meditation Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.MeditationWidgetKt.MeditationWidgetPreview.png)

### Critique
- **Data**: The widget uses text data derived from the preview ("3 mindful tasks left", "Breath", "Daily mindfulness").
- **Structure**: The original tile has a title, a column with two large buttons (with icons), and a bottom button "Browse". The widget fits all text info into a single card and omits the icons and bottom button to fit the "single card" rule.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded.

---

## News Tile

### Original Tile Preview
![Original News Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.NewsKt.newsPreview_Large Round 1.00f.png)

### New Widget Preview
![New News Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.NewsWidgetKt.NewsWidgetPreview.png)

### Critique
- **Data**: The widget uses the exact text data from the original preview ("Today, 31 July", "Millions still without power as new storm moves across US").
- **Structure**: The original tile has a date title, a large card with headline over a background image, and a bottom button "News". The widget fits the text info into a single card and omits the background image and bottom button to fit the "single card" rule.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded.

---

## Ski Tile

### Original Tile Preview
![Original Ski Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.SkiKt.skiPreview_Large Round 1.00f.png)

### New Widget Preview
![New Ski Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.SkiWidgetKt.SkiWidgetPreview.png)

### Critique
- **Data**: The widget combines value and unit into single lines to fit ("Max Spd: 46.5 mph", "Distance: 21.8 mile").
- **Structure**: The original tile has a title "Latest run" and a button group with two large data cards. The widget fits the text info into a single card following the "single card" rule.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded.

---

## Social Tile

### Original Tile Preview
![Original Social Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.SocialKt.socialPreview6_Large Round 1.00f.png)

### New Widget Preview
![New Social Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.SocialWidgetKt.SocialWidgetPreview.png)

### Critique
- **Data**: The widget lists the initials of contacts joined by spaces ("MS AB WW CD") instead of showing avatars or separate buttons.
- **Structure**: The original tile has a grid of contact buttons (avatars or initials) and a bottom button "More". The widget fits the text info into a single card following the "single card" rule.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded.

---

## Timer Tile

### Original Tile Preview
![Original Timer Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.TimerKt.timer1LayoutPreview_Large Round 1.00f.png)

### New Widget Preview
![New Timer Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.TimerWidgetKt.TimerWidgetPreview.png)

### Critique
- **Data**: The widget shows "Minutes" and quick options "5 10 15" derived from the first layout of the original tile.
- **Structure**: The original tile has a title and a button group of quick timer options, plus a bottom button. The widget fits the text info into a single card following the "single card" rule.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded.

---

## Weather Tile

### Original Tile Preview
![Original Weather Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.WeatherKt.weatherPreview_Large Round 1.00f.png)

### New Widget Preview
![New Weather Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.WeatherWidgetKt.WeatherWidgetPreview.png)

### Critique
- **Data**: The widget shows location "San Francisco", current temp "52°", and high/low "64° / 48°". It omits the emoji icon due to rendering issues in the preview tool.
- **Structure**: The original tile has location, conditions row, and a card with hourly forecast. The widget fits the main conditions into a single card and omits the forecast to fit the "single card" rule.
- **Style**: Text is center-aligned in a vertical column to prevent horizontal cut-off. Colors are dark purple and corners are rounded.

---

## Workout Tile

### Original Tile Preview
![Original Workout Tile](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.golden.WorkoutKt.workoutLayout1Preview_Large Round 1.00f.png)

### New Widget Preview
![New Workout Widget](/usr/local/google/home/yschimke/workspace/wear-os-samples/WearTilesKotlin/app/build/compose-previews/renders/com.example.wear.tiles.widget.WorkoutWidgetKt.WorkoutWidgetPreview.png)

### Critique
- **Data**: The widget shows title "Exercise" and options "Yoga Run Cycle" derived from the first layout of the original tile.
- **Structure**: The original tile has a title and a button group of workout types, plus a bottom button. The widget fits the text info into a single card following the "single card" rule.
- **Style**: Text is center-aligned. Colors are dark purple and corners are rounded.

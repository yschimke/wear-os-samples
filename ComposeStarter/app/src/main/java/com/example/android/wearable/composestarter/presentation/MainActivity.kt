/*
 * Copyright 2025 The Android Open Source Project
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
package com.example.android.wearable.composestarter.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import java.time.LocalTime
import com.example.android.wearable.composestarter.R
import com.example.android.wearable.composestarter.presentation.theme.AppCardDefaults
import com.example.android.wearable.composestarter.presentation.theme.WearAppTheme
import kotlinx.serialization.Serializable

/**
 * Simple "Hello, World" app meant as a starting point for a new project using Compose for Wear OS.
 *
 * Displays a centered [Text] composable and a list built with [TransformingLazyColumn].
 *
 * Use the Wear version of Compose Navigation. You can carry
 * over your knowledge from mobile, and it supports the swipe-to-dismiss gesture (Wear OS's
 * back action). For more information, go here:
 * https://developer.android.com/reference/kotlin/androidx/wear/compose/navigation3/package-summary
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

@Serializable
sealed interface AppKey : NavKey

@Serializable
data object MenuScreen : AppKey

@Serializable
data object ListNavScreen : AppKey

@Composable
fun WearApp() {
    val backStack = rememberNavBackStack(MenuScreen)

    WearAppTheme {
        AppScaffold {
            val entryProvider =
                remember {
                    entryProvider<NavKey> {
                        entry<MenuScreen> {
                            GreetingScreen(
                                "Android",
                                onShowList = { backStack.add(ListNavScreen) }
                            )
                        }
                        entry<ListNavScreen> {
                            ListScreen()
                        }
                    }
                }

            val swipeDismissableSceneStrategy = rememberSwipeDismissableSceneStrategy<NavKey>()

            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider,
                sceneStrategies = listOf(swipeDismissableSceneStrategy)
            )
        }
    }
}

@Composable
fun GreetingScreen(
    greetingName: String,
    onShowList: () -> Unit,
    modifier: Modifier = Modifier,
    timeOfDay: TimeOfDay = rememberTimeOfDay()
) {
    Box(modifier = modifier.fillMaxSize()) {
        Greeting(
            greetingName = greetingName,
            timeOfDay = timeOfDay,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 72.dp)
        )
        EdgeButton(
            onClick = onShowList,
            buttonSize = EdgeButtonSize.Medium,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = stringResource(R.string.show_list),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ListScreen(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState
        /*
         * TransformingLazyColumn takes care of the horizontal and vertical
         * padding for the list and handles scrolling.
         */
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(text = "Header") }
            }
            item {
                TitleCard(
                    title = {
                        Text(
                            stringResource(R.string.example_card_title) // Removed explicit color
                        )
                    },
                    onClick = { },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = AppCardDefaults.cardColors()
                ) {
                    Text(stringResource(R.string.example_card_content))
                }
            }
            item {
                Button(
                    label = {
                        Text(
                            text = stringResource(R.string.example_button_text),
                            modifier = modifier.fillMaxWidth()
                        )
                    },
                    onClick = { },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
            item {
                ButtonGroup(
                    modifier =
                        modifier
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            }.transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            )
                ) {
                    FilledIconButton(
                        onClick = { showDialog = true },
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription =
                                stringResource(
                                    R.string.settings_button_content_description
                                )
                        )
                    }
                    FilledIconButton(
                        onClick = { },
                        shapes = IconButtonDefaults.animatedShapes()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.thumb_up),
                            contentDescription =
                                stringResource(
                                    R.string.thumbs_up_button_content_description
                                )
                        )
                    }
                }
            }
        }
    }

    SampleDialog(
        showDialog = showDialog,
        onDismiss = { showDialog = false },
        onCancel = {},
        onOk = {}
    )
}

@Composable
fun Greeting(
    greetingName: String,
    modifier: Modifier = Modifier,
    timeOfDay: TimeOfDay = rememberTimeOfDay()
) {
    val colorScheme = MaterialTheme.colorScheme
    val heroBrush =
        Brush.linearGradient(
            colors = listOf(colorScheme.primary, colorScheme.tertiary)
        )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        Canvas(modifier = Modifier.size(width = 48.dp, height = 22.dp)) {
            val strokeWidth = 4.dp.toPx()
            val inset = strokeWidth / 2f
            drawArc(
                brush = heroBrush,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size =
                    androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        (size.height - strokeWidth) * 2f
                    ),
                style =
                    androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
            )
        }
        Text(
            text = timeOfDay.label,
            color = colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = greetingName,
            color = colorScheme.primary,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class TimeOfDay(val label: String) {
    Morning("Good morning"),
    Day("Hello"),
    Evening("Good evening")
}

@Composable
fun rememberTimeOfDay(): TimeOfDay =
    remember {
        when (LocalTime.now().hour) {
            in 5..11 -> TimeOfDay.Morning
            in 12..17 -> TimeOfDay.Day
            else -> TimeOfDay.Evening
        }
    }

@Composable
fun SampleDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        visible = showDialog,
        onDismissRequest = onDismiss,
        icon = {},
        title = { Text(text = stringResource(R.string.title)) },
        text = { Text(text = stringResource(R.string.error_long)) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    // Perform confirm action here
                    onOk()
                    onDismiss()
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    // Perform dismiss action here
                    onCancel()
                    onDismiss()
                }
            )
        }
    )
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun GreetingScreenPreview() {
    WearAppTheme {
        AppScaffold {
            GreetingScreen(
                greetingName = "Alex",
                onShowList = {},
                timeOfDay = TimeOfDay.Morning
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun ListScreenPreview() {
    ListScreen()
}

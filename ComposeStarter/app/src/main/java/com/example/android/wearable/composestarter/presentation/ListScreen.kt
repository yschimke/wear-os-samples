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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.android.wearable.composestarter.R
import com.example.android.wearable.composestarter.presentation.theme.AppCardDefaults
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun ListScreen(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }

    /*
     * Specifying the types of items that appear at the start and end of the list ensures that the
     * appropriate padding is used.
     */
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        /*
         * TransformingLazyColumn takes care of the horizontal and vertical
         * padding for the list and handles scrolling.
         * Use workaround from Horologist for padding or wait until fix lands
         */
        contentPadding =
            rememberResponsiveColumnPadding(
                first = ColumnItemType.ListHeader,
                last = ColumnItemType.IconButton
            )
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
                            .transformedHeight(this, transformationSpec),
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
fun ListScreenPreview() {
    ListScreen()
}

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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.android.wearable.composestarter.R
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun GreetingScreen(
    greetingName: String,
    onShowList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberTransformingLazyColumnState()

    /* If you have enough items in your list, use [TransformingLazyColumn] which is an optimized
     * version of LazyColumn for wear devices with some added features. For more information,
     * see d.android.com/wear/compose.
     */
    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onShowList,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text(stringResource(R.string.show_list))
            }
        },
        // The bottom padding value is always ignored when using EdgeButton because this button is
        // always placed at the end of the screen.
        // The `ScreenScaffold` parameter `edgeButtonSpacing` can be used to specify the
        // gap between edgeButton and content.
        contentPadding =
            rememberResponsiveColumnPadding(
                first = ColumnItemType.ListHeader
            )
    ) { contentPadding ->
        // Use workaround from Horologist for padding or wait until fix lands
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding
        ) {
            item { Greeting(greetingName = greetingName, modifier = modifier.fillMaxSize()) }
        }
    }
}

@Composable
fun Greeting(
    greetingName: String,
    modifier: Modifier = Modifier
) {
    ListHeader {
        Text(
            modifier = modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.hello_world, greetingName)
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun GreetingScreenPreview() {
    GreetingScreen("Ashley Ingram", onShowList = {})
}

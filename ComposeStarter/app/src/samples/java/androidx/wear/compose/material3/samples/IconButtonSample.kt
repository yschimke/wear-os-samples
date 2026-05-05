/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.material3.samples

import com.example.android.wearable.composestarter.R as AppR
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.OutlinedIconButton

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun IconButtonSample() {
    IconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun FilledIconButtonSample() {
    FilledIconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun FilledVariantIconButtonSample() {
    FilledIconButton(
        onClick = { /* Do something */ },
        colors = IconButtonDefaults.filledVariantIconButtonColors(),
    ) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun FilledTonalIconButtonSample() {
    FilledTonalIconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun OutlinedIconButtonSample() {
    OutlinedIconButton(onClick = { /* Do something */ }) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun IconButtonWithOnLongClickSample() {
    IconButton(
        onClick = { /* Do something for onClick*/ },
        onLongClick = { /* Do something on long click */ },
        onLongClickLabel = "Long click",
    ) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun IconButtonWithCornerAnimationSample() {
    FilledIconButton(
        onClick = { /* Do something */ },
        shapes = IconButtonDefaults.animatedShapes(),
        colors = IconButtonDefaults.filledIconButtonColors(),
    ) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun IconButtonWithImageSample() {
    val enabled = true
    IconButton(
        onClick = { /* Do something */ },
        shapes = IconButtonDefaults.shapes(),
        enabled = enabled,
    ) {
        Image(
            painter = painterResource(id = AppR.drawable.backgroundimage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                if (enabled) Modifier else Modifier.alpha(IconButtonDefaults.DisabledImageOpacity),
        )
    }
}

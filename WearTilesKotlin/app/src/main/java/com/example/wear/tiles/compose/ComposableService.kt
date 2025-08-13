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
package com.example.wear.tiles.compose

//import android.graphics.Canvas
//import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Text
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.example.wear.tiles.tools.toImageResource
import com.google.android.horologist.tiles.SuspendingTileService
import java.util.UUID

/**
 * Creates a Messaging tile, showing up to 6 contacts, an icon button and an edge button.
 *
 * It extends [SuspendingTileService], a Coroutine-friendly wrapper around
 * [androidx.wear.tiles.TileService], and implements [tileRequest] and [resourcesRequest].
 *
 * The main function, [tileRequest], is triggered when the system calls for a tile. Resources are
 * provided with the [resourcesRequest] method, which is triggered when the tile uses an Image.
 */
class ComposableService : SuspendingTileService() {
    val CanvasId = "circleCanvas"
    val ComposeId = "circleCompose"

    /** This method returns a Tile object, which describes the layout of the Tile. */
    override suspend fun tileRequest(requestParams: TileRequest): Tile {
        val layoutElement = LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                LayoutElementBuilders.Image.Builder()
                    .setWidth(dp(100f))
                    .setHeight(dp(100f))
                    .setResourceId(ComposeId)
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setBorder(
                                ModifiersBuilders.Border.Builder()
                                    .setWidth(dp(2f))
                                    .setColor(
                                        ColorBuilders.ColorProp.Builder(Color.Yellow.toArgb())
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return Tile.Builder()
            .setResourcesVersion(UUID.randomUUID().toString())
            .setTileTimeline(Timeline.fromLayoutElement(layoutElement))
            .build()
    }

    override suspend fun resourcesRequest(requestParams: ResourcesRequest): Resources {

        // Add images to the Resources object, and return
        return Resources.Builder()
            .setVersion(requestParams.version)
            .addIdToImageMapping(CanvasId, circleCanvas().toImageResource())
            .addIdToImageMapping(ComposeId, circleCompose().toImageResource())
            .build()
    }

    private fun circleCanvas(): Bitmap {
        val image = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(image)
        val radius = canvas.width / 2f
        canvas.drawCircle(radius, radius, radius, Paint().apply { setColor(Color.Red.toArgb()) })
        return image
    }

    private suspend fun circleCompose(): Bitmap {
        val renderer = ComposableBitmapRendererImpl(this.application)
        return renderer.renderComposableToBitmap(Size(200f, 200f)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color.Yellow)
                }
                Chip(onClick = {}, label = {
                    Text("Hello from Compose")
                })
            }
        }!!
    }
}

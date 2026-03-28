/*
 * Copyright 2021 The Android Open Source Project
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
@file:OptIn(ExperimentalHorologistApi::class)

package com.example.wear.tiles.messaging

import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

/**
 * Creates a Messaging tile, showing up to 6 contacts, an icon button and an edge button.
 *
 * It extends [SuspendingTileService], a Coroutine-friendly wrapper around
 * [androidx.wear.tiles.TileService], and implements [tileRequest] and [resourcesRequest].
 *
 * The main function, [tileRequest], is triggered when the system calls for a tile. Resources are
 * provided with the [resourcesRequest] method, which is triggered when the tile uses an Image.
 */
class MessagingTileService : Material3TileService() {
    private lateinit var imageLoader: ImageLoader
    private lateinit var contacts: List<Contact>

    override fun onCreate() {
        super.onCreate()

        // For this sample, contacts is a simple static list. In a real application it might come
        // from an injected repository.
        contacts = getMockNetworkContacts()

        // For this sample, make Coil dumb by disabling all caching features.
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context).memoryCache(null).diskCache(null).build()
        }

        imageLoader = SingletonImageLoader.get(this)
    }

    /** This method returns a Tile object, which describes the layout of the Tile. */
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val layoutElement = tileLayout(contacts)

        return Tile.Builder()
            .setTileTimeline(Timeline.fromLayoutElement(layoutElement))
            .build()
    }
}

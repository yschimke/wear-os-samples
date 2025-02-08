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

package com.example.android.wearable.composestarter.presentation.nav3

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.navigation3.NavBackStackProvider
import androidx.navigation3.NavEntry
import androidx.navigation3.NavLocalProvider
import androidx.wear.compose.foundation.HierarchicalFocusCoordinator
import androidx.wear.compose.foundation.SwipeToDismissKeys
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.SwipeToDismissBox

@Composable
fun <T : Any> rememberSaveableMutableStateListOf(vararg elements: T): SnapshotStateList<T> {
    return rememberSaveable(saver = snapshotStateListSaver()) {
        elements.toList().toMutableStateList()
    }
}

private fun <T : Any> snapshotStateListSaver() = listSaver<SnapshotStateList<T>, T>(
    save = { stateList -> stateList.toList() },
    restore = { it.toMutableStateList() },
)

@Composable
fun <T : Any> WearNavDisplay(
    backstack: MutableList<T>,
    modifier: Modifier = Modifier,
    localProviders: List<NavLocalProvider> = emptyList(),
    onBack: () -> Unit = { backstack.removeAt(backstack.size - 1) },
    entryProvider: (key: T) -> NavEntry<T>
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        SwipeToDismissWearNavDisplay(backstack, modifier, localProviders, onBack, entryProvider)
    } else {
        PredictiveWearNavDisplay(backstack, modifier, localProviders, onBack, entryProvider)
    }
}

@Composable
fun <T : Any> SwipeToDismissWearNavDisplay(
    backstack: MutableList<T>,
    modifier: Modifier = Modifier,
    localProviders: List<NavLocalProvider> = emptyList(),
    onBack: () -> Unit = { backstack.removeAt(backstack.size - 1) },
    entryProvider: (key: T) -> NavEntry<T>
) {
    BackHandler(backstack.size > 1, onBack)
    NavBackStackProvider<T>(backstack, entryProvider, localProviders) { entries ->
        val active = entries.last()
        val background = entries.getOrNull(entries.size - 2)

        SwipeToDismissBox(
            onDismissed = onBack,
            state = rememberSwipeToDismissBoxState(),
            modifier = modifier,
            backgroundKey = background?.key ?: SwipeToDismissKeys.Background,
            hasBackground = background != null,
            contentKey = active.key,
        ) { isBackground ->
            val child = if (isBackground) requireNotNull(background) else active
            HierarchicalFocusCoordinator(requiresFocus = { !isBackground }) {
                child.content.invoke(child.key)
            }
        }
    }
}

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

import androidx.activity.compose.PredictiveBackHandler
import androidx.collection.mutableObjectFloatMapOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.NavBackStackProvider
import androidx.navigation3.NavEntry
import androidx.navigation3.NavLocalProvider
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

@Composable
fun <T : Any> PredictiveWearNavDisplay(
    backstack: MutableList<T>,
    modifier: Modifier = Modifier,
    localProviders: List<NavLocalProvider> = emptyList(),
    onBack: () -> Unit = { backstack.removeAt(backstack.size - 1) },
    entryProvider: (key: T) -> NavEntry<T>
) {
    NavBackStackProvider<T>(backstack, entryProvider, localProviders) { entries ->
        val current = entries.last()
        val previous = entries.getOrNull(entries.size - 2)

        val scrimColor = LocalSwipeToDismissBackgroundScrimColor.current
        var progress by remember { mutableFloatStateOf(0f) }
        var inPredictiveBack by remember { mutableStateOf(false) }
        val transitionState = remember { SeekableTransitionState(current) }

        PredictiveBackHandler(backstack.size > 1) { backEvent ->
            inPredictiveBack = true
            progress = 0f
            try {
                backEvent.collect { progress = it.progress }
                Animatable(progress).animateTo(1f, TRANSITION_ANIMATION_SPEC) { progress = value }
                inPredictiveBack = false
                onBack()
            } catch (e: CancellationException) {
                inPredictiveBack = false
            }
        }
        val zIndices = remember { mutableObjectFloatMapOf<T>() }
        val transition = rememberTransition(transitionState, label = "entry")

        if (inPredictiveBack && previous != null) {
            LaunchedEffect(progress) { transitionState.seekTo(progress, previous) }
        } else {
            LaunchedEffect(current) {
                if (transitionState.currentState != current) {
                    transitionState.animateTo(current)
                } else {
                    animate(transitionState.fraction, 0f, animationSpec = TRANSITION_ANIMATION_SPEC) {
                            value,
                            _ ->
                        this@LaunchedEffect.launch {
                            if (value > 0) {
                                // Seek the original transition back to the currentState
                                transitionState.seekTo(value)
                            }
                            if (value == 0f) {
                                // Once we animate to the start, we need to snap to the right state.
                                transitionState.snapTo(current)
                            }
                        }
                    }
                }
            }
        }

        // TODO switch to transition on the whole stack
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation3/navigation3/src/androidMain/kotlin/androidx/navigation3/SinglePaneNavDisplay.android.kt;l=169
//        val isPop = isPop(transition.currentState, newStack)
        // No mapping for this
        val wearNavigator_isPop_value = false

        transition.AnimatedContent(
            modifier,
            transitionSpec = {
                val initialZIndex = zIndices.getOrPut(initialState.key) { 0f }
                val targetZIndex =
                    when {
                        targetState.key == initialState.key -> initialZIndex
                        wearNavigator_isPop_value || inPredictiveBack ->
                            initialZIndex - 1f // Going to the previous page, so zIndex - 1
                        else -> initialZIndex + 1f // Going to the next page, so zIndex + 1
                    }.also { zIndices[targetState.key] = it }

                ContentTransform(
                    targetContentEnter =
                        if (wearNavigator_isPop_value || inPredictiveBack) POP_ENTER_TRANSITION
                        else ENTER_TRANSITION,
                    initialContentExit =
                        if (wearNavigator_isPop_value || inPredictiveBack) POP_EXIT_TRANSITION
                        else EXIT_TRANSITION,
                    targetContentZIndex = targetZIndex,
                    sizeTransform = null
                )
            },
            contentAlignment = Alignment.Center,
            contentKey = { it.key }
        ) { thisEntry ->
            // In some specific cases, such as popping your back stack or changing your
            // start destination, AnimatedContent can contain an entry that is no longer
            // part of visible entries since it was cleared from the back stack and is not
            // animating.
            val currentEntry: NavEntry<T>? =
                if (wearNavigator_isPop_value || inPredictiveBack) {
                    // We have to do this because the previous entry might not show up in backStack
                    thisEntry
                } else {
                    backstack.lastOrNull { entry -> thisEntry.key == entry }?.let {
                        entryProvider(it)
                    }
                }

            if (currentEntry != null) {
                Box(
                    modifier =
                        Modifier.background(
                            scrimColor,
                            CircleShape
                        )
                            .fillMaxSize()
                ) {
                    currentEntry.content.invoke(currentEntry.key)
                    if (currentEntry.key != current.key) {
                        Box(
                            modifier =
                                Modifier.clickable(
                                    enabled = false,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    // Ignore taps on previous backstack entries
                                }
                                    .fillMaxSize()
                        )
                    }
                }
            }
        }
        LaunchedEffect(transition.currentState, transition.targetState) {
            if (transition.currentState == transition.targetState) {
//                backstack.forEach { entry -> wearNavigator.onTransitionComplete(entry) }
                zIndices.forEach { key, _ ->
                    if (key != transition.targetState.key) zIndices.remove(key)
                }
            }
        }
    }
}

private fun <T : Any> isPop(oldBackStack: List<T>, newBackStack: List<T>): Boolean {
    // entire stack replaced
    if (oldBackStack.first() != newBackStack.first()) return false
    // navigated
    if (newBackStack.size > oldBackStack.size) return false

    val divergingIndex =
        newBackStack.indices.firstOrNull { index -> newBackStack[index] != oldBackStack[index] }
    // if newBackStack never diverged from oldBackStack, then it is a clean subset of the oldStack
    // and is a pop
    return divergingIndex == null
}

private val ENTER_TRANSITION =
    slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = spring(0.8f, 300f)) +
        scaleIn(initialScale = 0.8f, animationSpec = spring(1f, 500f)) +
        fadeIn(animationSpec = spring(1f, 1500f))
private val EXIT_TRANSITION =
    scaleOut(targetScale = 0.85f, animationSpec = spring(1f, 150f)) +
        slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = spring(0.8f, 200f)) +
        fadeOut(targetAlpha = 0.6f, animationSpec = spring(1f, 1400f))
private val POP_ENTER_TRANSITION =
    scaleIn(initialScale = 0.8f, animationSpec = tween(easing = LinearEasing)) +
        slideInHorizontally(
            initialOffsetX = { -it / 2 },
            animationSpec = tween(easing = LinearEasing)
        ) +
        fadeIn(initialAlpha = 0.5f, animationSpec = tween(easing = LinearEasing))
private val POP_EXIT_TRANSITION =
    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(easing = LinearEasing)) +
        scaleOut(targetScale = 0.8f, animationSpec = tween(easing = LinearEasing))

private val TRANSITION_ANIMATION_SPEC =
    spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)

/*
 * Copyright 2025 - 2026 PICO. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pico.spatial.sample.spatialaudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.event.AudioEvents
import com.pico.spatial.sample.spatialaudio.WINDOW_ID
import com.pico.spatial.sample.spatialaudio.WINDOW_TAG
import com.pico.spatial.sample.spatialaudio.ecs.FlyTrajectorySystem
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.dsl.registerSystem
import com.pico.spatial.ui.foundation.dsl.unregisterSystem
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator

@Composable
fun ImmersiveScene(viewModel: AudioControlViewModel = AudioControlViewModel.instance) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val spatialNavigator = LocalSpatialNavigator.current
    val rootEntity = remember { Entity() }

    DisposableEffect(lifecycleOwner) {
        registerSystem<FlyTrajectorySystem>()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                spatialNavigator.closeWindowContainer(id = WINDOW_ID, tag = WINDOW_TAG)
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                spatialNavigator.openWindowContainer(id = WINDOW_ID, tag = WINDOW_TAG)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.unsubscribeAllAudioEvents()
            unregisterSystem<FlyTrajectorySystem>()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SpatialView(
        initial = { content, _ ->
            content.addEntity(rootEntity)
            viewModel.loadScene(rootEntity)
            content.subscribeAudioEvents(viewModel)
        }
    )
}

private fun SpatialViewContent.subscribeAudioEvents(viewModel: AudioControlViewModel) {
    viewModel.subscribeAudioEvent(this, AudioEvents.PlaybackStarted::class.java)
    viewModel.subscribeAudioEvent(this, AudioEvents.PlaybackPaused::class.java)
    viewModel.subscribeAudioEvent(this, AudioEvents.PlaybackCompleted::class.java)
}

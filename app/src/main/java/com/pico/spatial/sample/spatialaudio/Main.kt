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
package com.pico.spatial.sample.spatialaudio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pico.spatial.sample.spatialaudio.data.assetBundle
import com.pico.spatial.sample.spatialaudio.ui.AudioControlViewModel
import com.pico.spatial.sample.spatialaudio.ui.ControlPanel
import com.pico.spatial.sample.spatialaudio.ui.ImmersiveScene
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.platform.resize.ContainerResizeType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultStage { LifecycleResetScope { PicoTheme { ImmersiveScene() } } }

        WindowContainer(
            id = WINDOW_ID,
            form = Form.Planar,
            defaultSize = WindowContainerSize(550.dp, 380.dp),
            resizeType = ContainerResizeType.ContentSize,
            enableMaterialBackground = true,
        ) {
            LifecycleResetScope {
                PicoTheme {
                    ControlPanel(Modifier.windowConstraints(width = 550.dp, height = 380.dp))
                }
            }
        }

        MainScope().launch { assetBundle.await() }
    }

@Composable
private fun LifecycleResetScope(content: @Composable () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var restartKey by remember { mutableIntStateOf(0) }
    var hasPaused by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    hasPaused = true
                    AudioControlViewModel.instance.cleanUp()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (hasPaused) {
                        hasPaused = false
                        restartKey += 1
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            AudioControlViewModel.instance.cleanUp()
        }
    }

    key(restartKey) { content() }
}

const val WINDOW_ID = "control_panel"
const val WINDOW_TAG = "the_unique_control_panel"

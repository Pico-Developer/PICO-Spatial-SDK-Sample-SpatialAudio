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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.sample.spatialaudio.R
import com.pico.spatial.ui.design.Switch
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.foundation.vibrant.Vibrant
import com.pico.spatial.ui.foundation.vibrant.withVibrant
import com.pico.spatial.ui.graphics.Vibrant
import com.pico.spatial.ui.platform.Material
import com.pico.spatial.ui.platform.containers.closeStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ControlPanel(
    modifier: Modifier,
    viewModel: AudioControlViewModel = AudioControlViewModel.instance
) {
    val coroutine = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAmbientAudioEnabled by viewModel.isAmbientAudioEnabled.collectAsState()
    val isBirdAudioEnabled by viewModel.isBirdAudioEnabled.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_DESTROY) {
                coroutine.launch(Dispatchers.Main.immediate) { closeStage() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
    ) {
        CustomText(
            modifier = Modifier.height(120.dp).padding(start = 36.dp, top = 36.dp),
            text = stringResource(R.string.playback_settings),
            fontWeight = FontWeight(700),
            fontSize = 36.sp,
            lineHeight = 40.sp,
            vibrant = Vibrant.Darker,
        )
        AudioItemCard(
            semanticsText = "enable_ambient_audio_switch",
            text = stringResource(R.string.enable_ambient_audio),
            isEnabled = isAmbientAudioEnabled,
            vibrant = Vibrant.Darkest,
            onToggleEnable = { enable -> viewModel.toggleAmbientAudio(enable) }
        )
        Spacer(modifier = Modifier.height(15.dp))
        AudioItemCard(
            semanticsText = "enable_bird_audio_switch",
            text = stringResource(R.string.enable_bird_audio),
            isEnabled = isBirdAudioEnabled,
            vibrant = Vibrant.Darkest,
            onToggleEnable = { enable -> viewModel.toggleBirdAudio(enable) }
        )
    }
}

@Composable
fun AudioItemCard(
    semanticsText: String,
    text: String,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(12.dp),
    isEnabled: Boolean,
    onToggleEnable: (enabled: Boolean) -> Unit,
    vibrant: Vibrant
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(90.dp)
                .padding(horizontal = 36.dp)
                .clip(roundedCornerShape)
                .backgroundMaterial(enable = true, style = Material.Regular)
                .background(color = Color.Vibrant.withVibrant(Vibrant.Light)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CustomText(
            modifier = Modifier.height(32.dp).padding(start = 24.dp),
            text = text,
            fontWeight = FontWeight(500),
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.3.sp,
            vibrant = vibrant,
        )
        Switch(
            modifier =
                Modifier.padding(end = 16.dp).semantics { contentDescription = semanticsText },
            checked = isEnabled,
            onCheckedChange = { checked -> onToggleEnable(checked) }
        )
    }
}

@Composable
fun CustomText(
    modifier: Modifier = Modifier,
    text: String,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
    vibrant: Vibrant,
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        color = Color.Vibrant.withVibrant(vibrant),
    )
}

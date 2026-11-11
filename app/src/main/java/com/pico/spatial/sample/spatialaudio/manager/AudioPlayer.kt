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
package com.pico.spatial.sample.spatialaudio.manager

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.audio.AudioPlayerController
import com.pico.spatial.sample.spatialaudio.data.AMBIENT_AUDIO_NAME
import com.pico.spatial.sample.spatialaudio.data.AudioResourceStore
import com.pico.spatial.sample.spatialaudio.data.BIRD_AUDIO_NAME_PATH_LIST
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioPlayer(
    private val coroutineScope: CoroutineScope,
    private val audioResourceStore: AudioResourceStore
) {
    // Controller for each audio
    private val controllerMap: MutableMap<String, AudioPlayerController?> = mutableMapOf()
    // Settings for random-order bird audio loop
    private val enabledBirdAudio = mutableSetOf<String>()
    private var isLoopRunning = false

    /** Prepares audio resources and set up playback controllers for bird entity. */
    suspend fun prepareBirdAudio(bird: Entity) {
        BIRD_AUDIO_NAME_PATH_LIST.forEach { (audioName, _) ->
            audioResourceStore.getAudioResource(audioName)?.let { res ->
                val controller = bird.prepareAudio(res)
                controllerMap[audioName] = controller
                enabledBirdAudio.add(audioName)
            }
        }
        toggleBirdAudio(INITIAL_BIRD_AUDIO_ENABLED)
    }

    /** Prepares audio resources and set up playback controllers for ambient environment entity. */
    suspend fun prepareAmbientAudio(ambient: Entity) {
        audioResourceStore.getAudioResource(AMBIENT_AUDIO_NAME)?.let { res ->
            val controller = ambient.prepareAudio(res)
            controllerMap[AMBIENT_AUDIO_NAME] = controller
        }
        toggleAmbientAudio(INITIAL_AMBIENT_AUDIO_ENABLED)
    }

    fun toggleAmbientAudio(enabled: Boolean) {
        if (enabled) {
            playAudio(AMBIENT_AUDIO_NAME, true, 0.3f)
        } else {
            stopAudio(true)
        }
    }

    fun toggleBirdAudio(enabled: Boolean) {
        // If there's no audio or the loop is running, do nothing
        if (enabledBirdAudio.isEmpty() || enabled == isLoopRunning) return
        isLoopRunning = enabled
        if (enabled) {
            startBirdLoop()
        } else {
            stopAudio(false)
        }
    }

    private fun startBirdLoop() {
        coroutineScope.launch {
            while (isLoopRunning) {
                val randomAudio = enabledBirdAudio.random()
                playAudio(randomAudio, false)
                delay(Random.nextLong(BIRD_AUDIO_INTERVAL_MIN, BIRD_AUDIO_INTERVAL_MAX))
            }
        }
    }

    private fun playAudio(audioName: String, setLoop: Boolean = true, volume: Float = 1f) {
        controllerMap[audioName]?.let { controller ->
            controller.setVolume(volume)
            controller.setLoop(setLoop)
            controller.play()
        }
    }

    private fun stopAudio(isAmbient: Boolean) {
        if (isAmbient) {
            controllerMap[AMBIENT_AUDIO_NAME]?.stop()
        } else {
            enabledBirdAudio.forEach { controllerMap[it]?.stop() }
        }
    }

    fun cleanUp() {
        isLoopRunning = false
        controllerMap.forEach { (_, controller) ->
            controller?.stop()
            controller?.close()
        }
        controllerMap.clear()
        enabledBirdAudio.clear()
        audioResourceStore.cleanUp()
    }

    companion object {
        const val BIRD_AUDIO_INTERVAL_MIN = 1500L // in ms
        const val BIRD_AUDIO_INTERVAL_MAX = 2000L // in ms
        const val INITIAL_AMBIENT_AUDIO_ENABLED = false
        const val INITIAL_BIRD_AUDIO_ENABLED = true
    }
}

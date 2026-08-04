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

import android.util.Log
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.AmbientAudioComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ObjectAudioComponent
import com.pico.spatial.core.ecs.audio.Directivity
import com.pico.spatial.core.ecs.audio.DistanceAttenuationMode
import com.pico.spatial.core.ecs.event.Event
import com.pico.spatial.core.ecs.resource.AnimationResource
import com.pico.spatial.sample.spatialaudio.data.AudioResourceStore
import com.pico.spatial.sample.spatialaudio.data.assetBundle
import com.pico.spatial.sample.spatialaudio.ecs.FlyTrajectoryComponent
import com.pico.spatial.sample.spatialaudio.manager.AudioPlayer
import com.pico.spatial.sample.spatialaudio.manager.EventManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared audio controller used by both ImmersiveScene (3D) and ControlPanel (2D panel).
 *
 * Uses a singleton via [Companion.instance] because DefaultStage and WindowContainer run in
 * separate Compose trees and cannot share a instance through the normal ViewModelStore mechanism.
 */
class AudioControlViewModel private constructor() {
    private val audioResourceStore = AudioResourceStore()
    private val audioPlayer =
        AudioPlayer(CoroutineScope(SupervisorJob() + Dispatchers.Main), audioResourceStore)
    private val eventManager = EventManager()
    private var birdAnimationResource: AnimationResource? = null
    private var isSceneLoaded = false

    private val _isAmbientAudioEnabled = MutableStateFlow(AudioPlayer.INITIAL_AMBIENT_AUDIO_ENABLED)
    val isAmbientAudioEnabled: StateFlow<Boolean> = _isAmbientAudioEnabled.asStateFlow()

    private val _isBirdAudioEnabled = MutableStateFlow(AudioPlayer.INITIAL_BIRD_AUDIO_ENABLED)
    val isBirdAudioEnabled: StateFlow<Boolean> = _isBirdAudioEnabled.asStateFlow()

    init {
        eventManager.audioEventCallback = { event -> Log.d(TAG, "Audio Event: $event") }
    }

    fun toggleAmbientAudio(enable: Boolean) {
        Log.i(TAG, "toggleAmbientAudio: enable=$enable")
        _isAmbientAudioEnabled.value = enable
        audioPlayer.toggleAmbientAudio(enable)
    }

    fun toggleBirdAudio(enable: Boolean) {
        Log.i(TAG, "toggleBirdAudio: enable=$enable")
        _isBirdAudioEnabled.value = enable
        audioPlayer.toggleBirdAudio(enable)
    }

    fun loadScene(rootEntity: Entity) {
        Log.i(TAG, "loadScene: start, isSceneLoaded=$isSceneLoaded")
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                audioResourceStore.preloadAudio()
                Log.i(TAG, "loadScene: audio resources preloaded")
                val model =
                    withContext(Dispatchers.IO) { assetBundle.await().loadModel(SCENE_NAME) }
                Log.i(TAG, "loadScene: model loaded, model=$model")
                if (!isSceneLoaded) {
                    rootEntity.addChild(model)
                }
                prepareAudioNode(rootEntity)
                isSceneLoaded = true
            } catch (e: Exception) {
                Log.e(TAG, "loadScene: failed", e)
            }
        }
    }

    private suspend fun prepareAudioNode(rootEntity: Entity) {
        Log.i(TAG, "prepareAudioNode: start")
        // Set up ambient audio component
        val envEntity = rootEntity.findEntity(NODE_ENVIRONMENT)
        if (envEntity == null) {
            Log.e(TAG, "prepareAudioNode: environment entity '$NODE_ENVIRONMENT' not found!")
        } else {
            Log.i(
                TAG,
                "prepareAudioNode: found environment entity '$NODE_ENVIRONMENT', setting AmbientAudioComponent"
            )
            envEntity.components.set(AmbientAudioComponent(1.0f))
            audioPlayer.prepareAmbientAudio(envEntity)
        }
        // Set up object audio component
        val birdEntity = rootEntity.findEntity(NODE_BIRD)
        if (birdEntity == null) {
            Log.e(TAG, "prepareAudioNode: bird entity '$NODE_BIRD' not found!")
        } else {
            Log.i(
                TAG,
                "prepareAudioNode: found bird entity '$NODE_BIRD', setting FlyTrajectoryComponent + ObjectAudioComponent"
            )
            birdEntity.components.apply {
                set(FlyTrajectoryComponent(isEnabled = true))
                set(
                    ObjectAudioComponent(
                        volume = 1f,
                        directivity = Directivity(0.5f, 2.0f),
                        distanceAttenuationMode = DistanceAttenuationMode.INVERSE_SQUARED,
                        reverbVolume = 1f
                    )
                )
            }
            audioPlayer.prepareBirdAudio(birdEntity)
        }
        playBirdAnimation(rootEntity)
        Log.i(TAG, "prepareAudioNode: done")
    }

    private fun playBirdAnimation(rootEntity: Entity) {
        val smEntityArray = rootEntity.findSkinnedMeshEntity(true)
        if (smEntityArray.isEmpty()) {
            Log.e(TAG, "Fail to find skinned mesh for bird!")
        } else {
            Log.d(TAG, "Successfully found ${smEntityArray.size} skinned mesh for bird!")
            for (entity in smEntityArray) {
                birdAnimationResource = entity.getAnimationResources().firstOrNull()
                birdAnimationResource?.use { entity.playAnimation(it) }
            }
        }
    }

    fun subscribeAudioEvent(content: SpatialViewContent, event: Class<out Event>) {
        eventManager.subscribeAudioEvent(content, event)
    }

    fun unsubscribeAllAudioEvents() {
        eventManager.unsubscribeAllAudioEvents()
    }

    fun cleanUp() {
        birdAnimationResource?.close()
        birdAnimationResource = null
        audioPlayer.cleanUp()
        eventManager.unsubscribeAllAudioEvents()

        // Reset states
        _isAmbientAudioEnabled.value = AudioPlayer.INITIAL_AMBIENT_AUDIO_ENABLED
        _isBirdAudioEnabled.value = AudioPlayer.INITIAL_BIRD_AUDIO_ENABLED
        isSceneLoaded = false
    }

    companion object {
        private const val TAG = "AudioControlViewModel"
        private const val SCENE_NAME = "SpatialAudioScene"
        private const val NODE_BIRD = "pico_bird"
        private const val NODE_ENVIRONMENT = "SM_Sphere"

        val instance: AudioControlViewModel by lazy { AudioControlViewModel() }
    }
}

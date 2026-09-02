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
package com.pico.spatial.sample.spatialaudio.data

import com.pico.spatial.core.ecs.LoadType
import com.pico.spatial.core.ecs.resource.AudioResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

const val BIRD_AUDIO_NAME_1 = "bird001"
private const val BIRD_AUDIO_PATH_1 = "asset://audio/$BIRD_AUDIO_NAME_1.wav"
const val BIRD_AUDIO_NAME_2 = "bird002"
private const val BIRD_AUDIO_PATH_2 = "asset://audio/$BIRD_AUDIO_NAME_2.wav"
const val BIRD_AUDIO_NAME_3 = "bird003"
private const val BIRD_AUDIO_PATH_3 = "asset://audio/$BIRD_AUDIO_NAME_3.wav"
const val BIRD_AUDIO_NAME_4 = "bird004"
private const val BIRD_AUDIO_PATH_4 = "asset://audio/$BIRD_AUDIO_NAME_4.wav"
const val AMBIENT_AUDIO_NAME = "jungle_birds_florals"
private const val AMBIENT_AUDIO_PATH = "asset://audio/$AMBIENT_AUDIO_NAME.wav"

// You can add or delete bird audio names and paths here
val BIRD_AUDIO_NAME_PATH_LIST =
    listOf(
        BIRD_AUDIO_NAME_1 to BIRD_AUDIO_PATH_1,
        BIRD_AUDIO_NAME_2 to BIRD_AUDIO_PATH_2,
        BIRD_AUDIO_NAME_3 to BIRD_AUDIO_PATH_3,
        BIRD_AUDIO_NAME_4 to BIRD_AUDIO_PATH_4
    )

private val AUDIO_NAME_TO_PATH: Map<String, String> = buildMap {
    BIRD_AUDIO_NAME_PATH_LIST.forEach { (name, path) -> put(name, path) }
    put(AMBIENT_AUDIO_NAME, AMBIENT_AUDIO_PATH)
}

/**
 * Owns and keeps audio resource lifecycle (load/cache/close).
 *
 * Prefer instantiating and owning it in a ViewModel (or DI scope) instead of using a global
 * singleton with mutable state.
 */
class AudioResourceStore {
    private val audioResourceMap = mutableMapOf<String, AudioResource>()

    suspend fun preloadAudio() = coroutineScope {
        AUDIO_NAME_TO_PATH.filterKeys { it !in audioResourceMap }
            .map { (name, path) ->
                async(Dispatchers.IO) {
                    name to
                        AudioResource.load(
                            name = name,
                            path = path,
                            loadType = LoadType.FROM_ASSETS
                        )
                }
            }
            .awaitAll()
            .forEach { (name, res) -> audioResourceMap[name] = res }
    }

    suspend fun getAudioResource(name: String): AudioResource? {
        audioResourceMap[name]?.let {
            return it
        }
        val path = AUDIO_NAME_TO_PATH[name] ?: return null
        val res =
            withContext(Dispatchers.IO) {
                AudioResource.load(name = name, path = path, loadType = LoadType.FROM_ASSETS)
            }
        audioResourceMap[name] = res
        return res
    }

    fun cleanUp() {
        audioResourceMap.values.forEach { it.close() }
        audioResourceMap.clear()
    }
}

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
package com.pico.spatial.sample.spatialaudio.ecs

import com.pico.spatial.core.ecs.Component
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.ObjectAudioComponent
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Transform
import com.pico.spatial.core.math.Vector3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Component holding data for circular flight trajectory with vertical oscillations. */
class FlyTrajectoryComponent(
    var isEnabled: Boolean = false,
    val speed: Float = 0.8f,
    val radius: Float = 3f,
    val baseAltitude: Float = 2.5f
) : Component() {
    var elapsedTime = 0f
    var wingPhase = 0f

    // You need to override the clone method to make the custom component clonable
    override fun clone(): FlyTrajectoryComponent {
        val clone =
            FlyTrajectoryComponent(this.isEnabled, this.speed, this.radius, this.baseAltitude)
        clone.elapsedTime = this.elapsedTime
        clone.wingPhase = this.wingPhase
        return clone
    }
}

/** System that updates the transform of entities with [FlyTrajectoryComponent]. */
class FlyTrajectorySystem : System() {
    override fun update(context: SceneUpdateContext) {
        val deltaTime = context.deltaTime
        val condition =
            EntityQueryCondition.hasComponent(FlyTrajectoryComponent::class.java)
                .and(EntityQueryCondition.hasComponent(ObjectAudioComponent::class.java))

        context.scene.queryEntity(condition).forEach { entity ->
            val trajectory = entity.components[FlyTrajectoryComponent::class.java] ?: return@forEach
            if (!trajectory.isEnabled) return@forEach

            trajectory.elapsedTime += deltaTime
            val transform = entity.components[TransformComponent::class.java] ?: return@forEach

            val newTransform = calculateTransform(trajectory, deltaTime)
            transform.position = newTransform.position
            transform.eulerAngles = newTransform.rotation
        }
    }

    private fun calculateTransform(comp: FlyTrajectoryComponent, deltaTime: Float): Transform {
        val t = comp.elapsedTime
        val yawRate = comp.speed / comp.radius
        val phi = yawRate * t - (PI_FLOAT / 2f)

        // Large-scale climb oscillation
        val climbAmplitude = 0.3f
        val climbFrequency = 0.05f
        val climbRate =
            climbAmplitude *
                (2f * PI_FLOAT * climbFrequency) *
                cos(2f * PI_FLOAT * climbFrequency * t)

        // Position on circle in XZ-plane
        val posX = comp.radius * cos(phi)
        val posZ = comp.radius * sin(phi)

        // Altitude oscillations
        comp.wingPhase = (comp.wingPhase + 2f * PI_FLOAT * deltaTime) % (2f * PI_FLOAT)
        val posY =
            comp.baseAltitude +
                0.2f * sin(2f * PI_FLOAT * 0.2f * t) + // Slow oscillation
                0.02f * sin(comp.wingPhase) // Wingbeat bob

        // Orientation
        val pitch = atan2(climbRate, comp.speed).toDegrees()
        val roll = atan2(comp.speed * yawRate, 9.81f).toDegrees()

        val forward =
            Vector3(-comp.radius * yawRate * sin(phi), climbRate, comp.radius * yawRate * cos(phi))
                .normalize()
        val yaw = atan2(forward.x, forward.z).toDegrees()

        return Transform(Vector3(posX, posY, posZ), EulerAngles(pitch, yaw, roll), Vector3(1f))
    }

    private companion object {
        private val PI_FLOAT = PI.toFloat()

        private fun Float.toDegrees() = this * 180f / PI_FLOAT
    }
}

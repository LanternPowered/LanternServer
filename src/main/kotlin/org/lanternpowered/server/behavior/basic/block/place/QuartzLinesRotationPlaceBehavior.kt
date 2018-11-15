/*
 * This file is part of LanternServer, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the Software), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.server.behavior.basic.block.place

import org.lanternpowered.api.behavior.BehaviorContext
import org.lanternpowered.api.behavior.BehaviorContextKeys
import org.lanternpowered.api.behavior.BehaviorType
import org.lanternpowered.api.behavior.basic.PlaceBlockBehaviorBase
import org.lanternpowered.api.block.BlockSnapshotBuilder
import org.lanternpowered.api.ext.*
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.type.QuartzTypes
import org.spongepowered.api.util.Axis
import org.spongepowered.api.util.Direction

class QuartzLinesRotationPlaceBehavior : PlaceBlockBehaviorBase {

    override fun apply(type: BehaviorType, ctx: BehaviorContext, placed: MutableList<BlockSnapshotBuilder>): Boolean {
        val face = ctx[BehaviorContextKeys.INTERACTION_FACE] ?: Direction.UP
        val axis = Axis.getClosest(face.asOffset())
        val newQuartzType = if (axis == Axis.X) QuartzTypes.LINES_X else if (axis == Axis.Y) QuartzTypes.LINES_Y else QuartzTypes.LINES_Z
        placed.forEach {
            val quartzType = it.blockState.require(Keys.QUARTZ_TYPE)
            if (quartzType == QuartzTypes.LINES_X ||
                    quartzType == QuartzTypes.LINES_Y ||
                    quartzType == QuartzTypes.LINES_Z) {
                it.add(Keys.QUARTZ_TYPE, newQuartzType)
            }
        }
        return true
    }
}

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
package org.lanternpowered.api.behavior.basic.block

import org.lanternpowered.api.behavior.Behavior
import org.lanternpowered.api.behavior.BehaviorContext
import org.lanternpowered.api.behavior.BehaviorContextKeys
import org.lanternpowered.api.behavior.BehaviorType
import org.lanternpowered.api.catalog.CatalogKeys
import org.lanternpowered.api.cause.CauseContextKey
import org.lanternpowered.api.ext.*
import org.spongepowered.api.block.BlockSnapshot

/**
 * The block placement behavior base.
 */
class PlaceBlockBehavior : Behavior {

    override fun apply(type: BehaviorType, ctx: BehaviorContext): Boolean {
        val slot = ctx[BehaviorContextKeys.UsedSlot]
        val stack = (ctx[BehaviorContextKeys.UsedItem]?.createStack() ?: slot?.peek()).orEmpty()
        // A used item or slot is expected for this behavior to work
        if (stack.isEmpty) return false
        // Convert the stack into a snapshot that can be placed

        return false
    }

    companion object {

        /**
         * A list of [BlockSnapshot]s that are being placed by the placement behavior.
         */
        val PlacedSnapshots = CauseContextKey<MutableList<BlockSnapshot.Builder>>(CatalogKeys.minecraft("placed_blocks"))
    }
}
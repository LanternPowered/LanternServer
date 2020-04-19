/*
 * Lantern
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package org.lanternpowered.server.block

import com.google.common.base.Objects
import org.lanternpowered.api.util.ToStringHelper
import org.lanternpowered.server.data.DataQueries
import org.lanternpowered.server.data.ForwardingDataHolder
import org.lanternpowered.server.data.SerializableForwardingDataHolder
import org.spongepowered.api.block.BlockState
import org.spongepowered.api.data.DataHolder
import org.spongepowered.api.data.Key
import org.spongepowered.api.data.SerializableDataHolder
import org.spongepowered.api.data.persistence.DataContainer
import org.spongepowered.api.data.persistence.DataView
import org.spongepowered.api.data.persistence.Queries
import org.spongepowered.api.data.value.MergeFunction
import org.spongepowered.api.data.value.Value
import org.spongepowered.api.world.LocatableBlock
import org.spongepowered.api.world.Location
import java.util.Optional
import java.util.function.Function

class LanternLocatableBlock internal constructor(
        internal val location: Location, internal val blockState: BlockState
) : LocatableBlock, SerializableForwardingDataHolder {

    override fun getBlockState() = this.blockState
    override fun getLocation() = this.location

    override val delegateDataHolder: SerializableDataHolder get() = this.blockState

    override fun toContainer(): DataContainer {
        val blockPos = this.location.position.toInt()
        return super.toContainer()
                .set(Queries.WORLD_ID, this.location.worldUniqueId)
                .set(Queries.POSITION_X, blockPos.x)
                .set(Queries.POSITION_Y, blockPos.y)
                .set(Queries.POSITION_Z, blockPos.z)
                .set(DataQueries.BLOCK_STATE, this.blockState)
    }

    override fun <E> transform(key: Key<out Value<E>>, function: Function<E, E>): Optional<LocatableBlock> =
            this.blockState.transform(key, function).map { state -> LanternLocatableBlock(this.location, state) }

    override fun <E> with(key: Key<out Value<E>>, value: E): Optional<LocatableBlock> =
            this.blockState.with(key, value).map { state -> LanternLocatableBlock(this.location, state) }

    override fun with(value: Value<*>): Optional<LocatableBlock> =
            this.blockState.with(value).map { state -> LanternLocatableBlock(this.location, state) }

    override fun without(key: Key<*>): Optional<LocatableBlock> =
            this.blockState.without(key).map { state -> LanternLocatableBlock(this.location, state) }

    override fun mergeWith(that: LocatableBlock, function: MergeFunction): LocatableBlock {
        val state = this.blockState.mergeWith(that.blockState, function)
        return if (state === this.blockState) this else LanternLocatableBlock(this.location, state)
    }

    override fun withRawData(container: DataView): LocatableBlock =
            LanternLocatableBlock(this.location, this.blockState.withRawData(container))

    override fun copy() = this

    override fun toString() = ToStringHelper(this)
            .add("blockState", this.blockState)
            .add("location", this.location)
            .toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is LanternLocatableBlock) {
            return false
        }
        return this.blockState === other.blockState && this.location == other.location
    }

    override fun hashCode() = Objects.hashCode(this.blockState, this.location)
}

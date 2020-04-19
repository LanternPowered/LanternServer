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
package org.lanternpowered.server.util.gen.block;

import org.lanternpowered.server.game.registry.type.block.BlockRegistryModule;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.world.extent.ImmutableBlockVolume;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.extent.StorageType;
import org.spongepowered.math.vector.Vector3i;

public class IntArrayImmutableBlockBuffer extends AbstractImmutableBlockBuffer {

    private final BlockState air = BlockTypes.AIR.getDefaultState();
    private final int[] blocks;

    public IntArrayImmutableBlockBuffer(int[] blocks, Vector3i start, Vector3i size) {
        super(start, size);
        this.blocks = blocks.clone();
    }

    private IntArrayImmutableBlockBuffer(Vector3i start, Vector3i size, int[] blocks) {
        super(start, size);
        this.blocks = blocks;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        checkRange(x, y, z);
        final int blockState = this.blocks[index(x, y, z)];
        return BlockRegistryModule.get().getStateByInternalId(blockState).orElse(this.air);
    }

    @Override
    public MutableBlockVolume getBlockCopy(StorageType type) {
        switch (type) {
            case STANDARD:
                return new IntArrayMutableBlockBuffer(this.blocks.clone(), this.start, this.size);
            case THREAD_SAFE:
                return new AtomicIntArrayMutableBlockBuffer(this.blocks, this.start, this.size);
            default:
                throw new UnsupportedOperationException(type.name());
        }
    }

    /**
     * This method doesn't clone the array passed into it. INTERNAL USE ONLY.
     * Make sure your code doesn't leak the reference if you're using it.
     *
     * @param blocks The blocks to store
     * @param start The start of the volume
     * @param size The size of the volume
     * @return A new buffer using the same array reference
     */
    public static ImmutableBlockVolume newWithoutArrayClone(int[] blocks, Vector3i start, Vector3i size) {
        return new IntArrayImmutableBlockBuffer(start, size, blocks);
    }
}

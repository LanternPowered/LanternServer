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
package org.lanternpowered.server.world.chunk;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.lanternpowered.api.cause.CauseStack;
import org.lanternpowered.server.game.Lantern;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.world.World;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vector3i;

import java.util.LinkedList;

import org.checkerframework.checker.nullness.qual.Nullable;

class LanternLoadingTicket implements ChunkLoadingTicket {

    final LinkedList<Vector2i> queue = new LinkedList<>();
    private final LanternChunkManager chunkManager;
    private final String plugin;

    // The extra data, can be attached by mods, just keep it safe until
    // sponge decides to add a api for it
    @Nullable DataContainer extraData;

    // The maximum amount of chunks that can be loaded by this ticket
    private final int maxChunks;

    // The amount of chunks that may be loaded by this ticket
    private int numChunks;

    // Whether the ticket is released and may not be used again
    private boolean released;

    LanternLoadingTicket(String plugin, LanternChunkManager chunkManager, int maxChunks) {
        this(plugin, chunkManager, maxChunks, maxChunks);
    }

    LanternLoadingTicket(String plugin, LanternChunkManager chunkManager, int maxChunks, int numChunks) {
        this.numChunks = Math.min(numChunks, maxChunks);
        this.chunkManager = chunkManager;
        this.maxChunks = maxChunks;
        this.plugin = plugin;
    }

    /**
     * Gets whether there no entries are inside the ticket.
     * 
     * @return Is empty
     */
    public boolean isEmpty() {
        synchronized (this.queue) {
            return this.queue.isEmpty();
        }
    }

    @Override
    public boolean setNumChunks(int numChunks) {
        checkArgument(numChunks >= 0, "numChunks may not be negative");
        synchronized (this.queue) {
            if (numChunks > this.maxChunks) {
                return false;
            }
            // Remove the oldest chunks that cannot be loaded anymore
            if (!this.released && numChunks < this.numChunks) {
                int size = this.queue.size();

                if (numChunks < size) {
                    final CauseStack causeStack = CauseStack.currentOrEmpty();
                    for (int i = 0; i < size - numChunks; i++) {
                        this.chunkManager.unforce(this, this.queue.poll(), causeStack);
                    }
                }
            }
            this.numChunks = numChunks;
        }
        return true;
    }

    @Override
    public int getNumChunks() {
        synchronized (this.queue) {
            return this.numChunks;
        }
    }

    @Override
    public int getMaxNumChunks() {
        return this.maxChunks;
    }

    @Override
    public World getWorld() {
        return this.chunkManager.getWorld();
    }

    @Override
    public DataContainer getCompanionData() {
        synchronized (this.queue) {
            return this.extraData == null ? DataContainer.createNew() : this.extraData.copy();
        }
    }

    @Override
    public void setCompanionData(DataContainer container) {
        checkNotNull(container, "container");
        synchronized (this.queue) {
            this.extraData = container.isEmpty() ? null : container.copy();
        }
    }

    @Override
    public String getPlugin() {
        return this.plugin;
    }

    @Override
    public ImmutableSet<Vector3i> getChunkList() {
        synchronized (this.queue) {
            if (this.released) {
                return ImmutableSet.of();
            }
            return this.queue.stream()
                    .map(v -> new Vector3i(v.getX(), 0, v.getY()))
                    .collect(ImmutableSet.toImmutableSet());
        }
    }

    @Override
    public void forceChunk(Vector3i chunk) {
        this.forceChunk(checkNotNull(chunk, "chunk").toVector2(true));
    }

    @Override
    public boolean forceChunk(Vector2i chunk) {
        checkNotNull(chunk, "chunk");
        synchronized (this.queue) {
            if (this.released) {
                Lantern.getLogger().warn("The plugin {} attempted to force load a chunk with an invalid ticket. "
                        + "This is not permitted.", this.plugin);
                return false;
            }
            // Only force if not done before
            if (!this.queue.contains(chunk)) {
                // Remove the oldest chunk if necessary
                if (this.queue.size() >= this.numChunks) {
                    this.chunkManager.unforce(this, this.queue.poll(), CauseStack.currentOrEmpty());
                }
                this.queue.add(chunk);
                this.chunkManager.force(this, chunk);
                return true;
            }
        }
        return false;
    }

    @Override
    public void unforceChunk(Vector3i chunk) {
        this.unforceChunk(checkNotNull(chunk, "chunk").toVector2(true));
    }

    @Override
    public boolean unforceChunk(Vector2i chunk) {
        final Vector2i chunk0 = checkNotNull(chunk, "chunk");
        synchronized (this.queue) {
            if (this.released) {
                return false;
            }
            if (this.queue.remove(chunk0)) {
                this.chunkManager.unforce(this, chunk0, CauseStack.currentOrEmpty());
                return true;
            }
        }
        return false;
    }

    @Override
    public void unforceChunks() {
        synchronized (this.queue) {
            if (this.released) {
                return;
            }
            final CauseStack causeStack = CauseStack.currentOrEmpty();
            while (!this.queue.isEmpty()) {
                this.chunkManager.unforce(this, this.queue.poll(), causeStack);
            }
        }
    }

    @Override
    public boolean isReleased() {
        synchronized (this.queue) {
            return this.released;
        }
    }

    @Override
    public void prioritizeChunk(Vector3i chunk) {
        checkNotNull(chunk, "chunk");
        synchronized (this.queue) {
            if (this.released) {
                return;
            }
            final Vector2i chunk0 = chunk.toVector2(true);
            // Move the chunk to the bottom of the queue if found
            if (this.queue.remove(chunk0)) {
                this.queue.add(chunk0);
            }
        }
    }

    @Override
    public void release() {
        synchronized (this.queue) {
            unforceChunks();
            this.chunkManager.release(this);
            this.released = true;
        }
    }

    MoreObjects.ToStringHelper toStringHelper() {
        synchronized (this.queue) {
            return MoreObjects.toStringHelper(this)
                    .add("plugin", this.plugin)
                    .add("maxChunks", this.maxChunks)
                    .add("numChunks", this.numChunks)
                    .add("released", this.released)
                    .add("chunks", Iterables.toString(this.queue));
        }
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}

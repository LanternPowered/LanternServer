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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.lanternpowered.server.util.Conditions.checkPlugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.lanternpowered.server.config.GlobalConfig;
import org.lanternpowered.server.world.LanternWorld;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.chunk.ChunkTicketManager;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class LanternChunkTicketManager implements ChunkTicketManager {

    private final Multimap<String, Callback> callbacks = HashMultimap.create();
    private final Provider<GlobalConfig> globalConfig;

    @Inject
    private LanternChunkTicketManager(Provider<GlobalConfig> globalConfig) {
        this.globalConfig = globalConfig;
    }

    /**
     * Gets all the registered callbacks.
     * 
     * @return The callbacks
     */
    public Multimap<String, Callback> getCallbacks() {
        return ImmutableMultimap.copyOf(this.callbacks);
    }

    /**
     * Gets the maximum amount of tickets the player can have.
     * 
     * @param playerUUID The player unique id
     * @return The maximum amount of tickets
     */
    public int getMaxTicketsForPlayer(UUID playerUUID) {
        return this.globalConfig.get().getPlayerTicketCount();
    }

    @Override
    public void registerCallback(Object plugin, Callback callback) {
        this.callbacks.put(checkPlugin(plugin, "plugin").getId(), checkNotNull(callback, "callback"));
    }

    @Override
    public Optional<LoadingTicket> createTicket(Object plugin, World world) {
        return ((LanternWorld) checkNotNull(world, "world")).getChunkManager().createTicket(plugin);
    }

    @Override
    public Optional<EntityLoadingTicket> createEntityTicket(Object plugin, World world) {
        return ((LanternWorld) checkNotNull(world, "world")).getChunkManager().createEntityTicket(plugin);
    }

    @Override
    public Optional<PlayerLoadingTicket> createPlayerTicket(Object plugin, World world, UUID player) {
        return ((LanternWorld) checkNotNull(world, "world")).getChunkManager().createPlayerTicket(plugin, player);
    }

    @Override
    public Optional<PlayerEntityLoadingTicket> createPlayerEntityTicket(Object plugin, World world, UUID player) {
        return ((LanternWorld) checkNotNull(world, "world")).getChunkManager().createPlayerEntityTicket(plugin, player);
    }

    @Override
    public int getMaxTickets(Object plugin) {
        return this.getMaxTicketsById(checkPlugin(plugin, "plugin").getId());
    }

    public int getMaxTicketsById(String plugin) {
        return this.globalConfig.get().getChunkLoadingTickets(checkNotNull(plugin, "plugin")).getMaximumTicketCount();
    }

    @Override
    public int getAvailableTickets(Object plugin, World world) {
        final LanternChunkManager chunkManager = ((LanternWorld) checkNotNull(world, "world")).getChunkManager();
        final String pluginId = checkPlugin(plugin, "plugin").getId();
        return chunkManager.getMaxTicketsForPlugin(pluginId) - chunkManager.getTicketsForPlugin(pluginId);
    }

    @Override
    public int getAvailableTickets(UUID player) {
        checkNotNull(player, "player");
        int count = 0;
        for (World world : Sponge.getServer().getWorlds()) {
            count += ((LanternWorld) world).getChunkManager().getTicketsForPlayer(player);
        }
        return this.getMaxTicketsForPlayer(player) - count;
    }

    @Override
    public ImmutableSetMultimap<Vector3i, LoadingTicket> getForcedChunks(World world) {
        return ((LanternWorld) checkNotNull(world, "world")).getChunkManager().getForced();
    }

}

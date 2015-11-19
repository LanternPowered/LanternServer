/*
 * This file is part of LanternServer, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://github.com/LanternPowered/LanternServer>
 * Copyright (c) Contributors
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
package org.lanternpowered.server.world;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.lanternpowered.server.game.LanternGame;
import org.lanternpowered.server.world.LanternWorldPropertiesIO.LevelData;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.storage.WorldProperties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public final class LanternWorldManager {

    // The counter for the executor threads
    private final AtomicInteger counter = new AtomicInteger();
    // The executor for async world manager operations
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(
            runnable -> new Thread(runnable, "worlds-" + this.counter.getAndIncrement())));

    // The prefix used for (dimension) world folders
    static final String DIMENSION_PREFIX = "DIM";

    // The size of the dimension map
    static final int DIMENSION_MAP_SIZE = Long.SIZE << 4;

    // A lookup entry used to store world references
    private static class WorldLookupEntry {

        // The world properties
        public final LanternWorldProperties properties;

        // The folder where all the world files are stored
        public final File folder;

        // The dimension id of the world
        public final int dimensionId;

        // The reference to the world instance
        @Nullable public volatile LanternWorld world;

        public WorldLookupEntry(LanternWorldProperties properties, File folder,
                int dimensionId) {
            this.dimensionId = dimensionId;
            this.properties = properties;
            this.folder = folder;
        }
    }

    // A map with all the world threads
    private final Map<LanternWorld, Thread> worldThreads = Maps.newConcurrentMap();

    // The world entries indexed by the name
    private final Map<LanternWorldProperties, WorldLookupEntry> worldByProperties = Maps.newConcurrentMap();

    // The world entries indexed by the name
    private final Map<String, WorldLookupEntry> worldByName = Maps.newConcurrentMap();

    // The world entries indexed by the unique ids
    private final Map<UUID, WorldLookupEntry> worldByUUID = Maps.newConcurrentMap();

    // The world entries indexed by the dimension ids
    private final Map<Integer, WorldLookupEntry> worldByDimensionId = Maps.newConcurrentMap();

    // The map of all the dimension ids
    private BitSet dimensionMap;

    // The folder of the root world
    private final File rootWorldFolder;

    // The phasers to synchronize the world threads
    private final Phaser tickBegin = new Phaser(1);
    private final Phaser tickEnd = new Phaser(1);

    /**
     * Creates a new world manager.
     * 
     * @param rootWorldFolder the folder of the root (default) world
     */
    public LanternWorldManager(File rootWorldFolder) {
        this.rootWorldFolder = rootWorldFolder;
    }

    /**
     * Gets a loaded {@link World} by name, if it exists.
     *
     * @param worldName name to lookup
     * @return the world, if found
     */
    public Optional<World> getWorld(String worldName) {
        return this.worldByName.containsKey(worldName) ? Optional.ofNullable(
                this.worldByName.get(worldName).world) : Optional.empty();
    }

    /**
     * Gets all currently loaded {@link World}s.
     *
     * @return a collection of loaded worlds
     */
    public Collection<World> getWorlds() {
        return this.worldByUUID.values().stream().map(e -> e.world)
                .collect(GuavaCollectors.toImmutableList());
    }

    /**
     * Gets a loaded {@link World} by its unique id ({@link UUID}), if it
     * exists.
     *
     * @param uniqueId uuid to lookup
     * @return the world, if found
     */
    public Optional<World> getWorld(UUID uniqueId) {
        return this.worldByUUID.containsKey(uniqueId) ? Optional.ofNullable(
                this.worldByUUID.get(uniqueId).world) : Optional.empty();
    }

    /**
     * Gets the properties of all worlds, loaded or otherwise.
     *
     * @return a collection of world properties
     */
    public Collection<WorldProperties> getAllWorldProperties() {
        return this.worldByUUID.values().stream().map(e -> e.properties)
                .collect(GuavaCollectors.toImmutableList());
    }

    /**
     * Gets the properties of all unloaded worlds.
     *
     * @return a collection of world properties
     */
    public Collection<WorldProperties> getUnloadedWorlds() {
        return this.worldByUUID.values().stream().filter(e -> e.world == null)
                .map(e -> e.properties).collect(GuavaCollectors.toImmutableList());
    }

    /**
     * Gets the {@link WorldProperties} of a world. If a world with the given
     * name is loaded then this is equivalent to calling
     * {@link World#getProperties()}. However, if no loaded world is found then
     * an attempt will be made to match unloaded worlds.
     *
     * @param worldName the name to lookup
     * @return the world properties, if found
     */
    public Optional<WorldProperties> getWorldProperties(String worldName) {
        return this.worldByName.containsKey(worldName) ? Optional.ofNullable(
                this.worldByName.get(worldName).properties) : Optional.empty();
    }

    /**
     * Gets the {@link WorldProperties} of a world. If a world with the given
     * UUID is loaded then this is equivalent to calling
     * {@link World#getProperties()}. However, if no loaded world is found then
     * an attempt will be made to match unloaded worlds.
     *
     * @param uniqueId the uuid to lookup
     * @return the world properties, if found
     */
    public Optional<WorldProperties> getWorldProperties(UUID uniqueId) {
        return this.worldByUUID.containsKey(uniqueId) ? Optional.ofNullable(
                this.worldByUUID.get(uniqueId).properties) : Optional.empty();
    }

    /**
     * Gets the properties of default world.
     *
     * @return the world properties
     */
    public Optional<WorldProperties> getDefaultWorld() {
        // Can be empty if the properties aren't loaded yet
        return this.worldByDimensionId.containsKey(0) ? Optional.ofNullable(
                this.worldByDimensionId.get(0).properties) : Optional.empty();
    }

    /**
     * Unloads a {@link World}, if there are any connected players in the given
     * world then no operation will occur.
     *
     * <p>A world which is unloaded will be removed from memory. However if it
     * is still enabled according to {@link WorldProperties#isEnabled()} then it
     * will be loaded again if the server is restarted or an attempt is made by
     * a plugin to transfer an entity to the world using
     * {@link Entity#transferToWorld(String, Vector3d)}.</p>
     *
     * @param world the world to unload
     * @return whether the operation was successful
     */
    public boolean unloadWorld(World world) {
        final LanternWorld world0 = (LanternWorld) world;
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Creates a world copy asynchronously using the new name given and returns
     * the new world properties if the copy was possible.
     *
     * <p>If the world is already loaded then the following will occur:</p>
     *
     * <ul>
     * <li>World is saved.</li>
     * <li>World saving is disabled.</li>
     * <li>World is copied. </li>
     * <li>World saving is enabled.</li>
     * </ul>
     *
     * @param worldProperties The world properties to copy
     * @param copyName The name for copied world
     * @return An {@link Optional} containing the properties of the new world
     *         instance, if the copy was successful
     */
    public ListenableFuture<Optional<WorldProperties>> copyWorld(
            WorldProperties worldProperties, String copyName) {
        return this.executor.submit(() -> {
            // Get the new dimension id
            final int dimensionId = this.getNextFreeDimensionId();

            // TODO: Save the world if loaded
            // TODO: Block world saving

            // Get the lookup entry
            final WorldLookupEntry entry = this.worldByProperties.get(worldProperties);

            // The folder of the new world
            final File targetFolder = this.getWorldFolder(dimensionId);
            // The folder of the original world
            final File folder = entry.folder;

            // Save the changes once more to make sure that they will be saved
            this.saveWorldProperties(worldProperties);

            // Copy the world folder
            final String folderPath = folder.getAbsolutePath();
            try {
                Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        final File dstFile = new File(targetFolder, path.toFile()
                                .getAbsolutePath().substring(folderPath.length()));
                        Files.copy(path, dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LanternGame.log().error("Failed to copy the world folder of {}: {} to {}",
                        copyName, folder, targetFolder, e);
                return Optional.empty();
            }

            final LevelData data;
            try {
                data = LanternWorldPropertiesIO.read(this.rootWorldFolder, copyName);
            } catch (IOException e) {
                LanternGame.log().error("Unable to open the copied world properties of {}", copyName, e);
                return Optional.empty();
            }
            final LanternWorldProperties properties = data.properties;
            final LevelData newData = new LevelData(properties, dimensionId, null);

            // Store the new world
            this.addWorld(targetFolder, newData);

            // Save the changes once more to make sure that they will be saved
            LanternWorldPropertiesIO.write(targetFolder, newData);

            return Optional.of(properties);
        });
    }

    /**
     * Renames an unloaded world.
     *
     * @param worldProperties The world properties to rename
     * @param newName The name that should be used as a replacement for the
     *        current world name
     * @return An {@link Optional} containing the new {@link WorldProperties}
     *         if the rename was successful
     */
    public Optional<WorldProperties> renameWorld(WorldProperties worldProperties, String newName) {
        final WorldLookupEntry entry = this.worldByProperties.get(worldProperties);
        // You cannot rename a active world or if there is
        // already a world present with that name
        if (entry.world != null || this.getWorld(newName).isPresent()) {
            return Optional.empty();
        }
        final LanternWorldProperties worldProperties0 = (LanternWorldProperties) worldProperties;
        this.worldByName.put(newName, entry);
        this.worldByName.remove(worldProperties0.name);
        worldProperties0.name = newName;

        // Save the changes once more to make sure that they will be saved
        this.saveWorldProperties(worldProperties0);
        return Optional.of(worldProperties0);
    }

    /**
     * Deletes the provided world's files asynchronously from the disk.
     *
     * @param worldProperties the world properties to delete
     * @return true if the deletion was successful
     */
    public ListenableFuture<Boolean> deleteWorld(WorldProperties worldProperties) {
        return this.executor.submit(() -> {
            final WorldLookupEntry entry = this.worldByProperties.get(worldProperties);
            if (entry.world != null) {
                return false;
            }
            Files.walkFileTree(entry.folder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        LanternGame.log().error("Unable to delete the file {} of world {}",
                                path.toFile().getAbsolutePath(), worldProperties.getWorldName(), e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            this.worldByName.remove(worldProperties.getWorldName());
            this.worldByDimensionId.remove(entry.dimensionId);
            this.worldByProperties.remove(worldProperties);
            this.worldByUUID.remove(worldProperties.getUniqueId());
            return entry.folder.delete();
        });
    }

    /**
     * Persists the given {@link WorldProperties} to the world storage for it,
     * updating any modified values.
     *
     * @param worldProperties the world properties to save
     * @return true if the save was successful
     */
    public boolean saveWorldProperties(WorldProperties worldProperties) {
        final WorldLookupEntry entry = this.worldByProperties.get(worldProperties);
        final LanternWorldProperties worldProperties0 = (LanternWorldProperties) worldProperties;
        final BitSet dimensionMap = entry.dimensionId == 0 ? (BitSet) this.dimensionMap.clone() : null;
        try {
            LanternWorldPropertiesIO.write(entry.folder, new LevelData(worldProperties0,
                    entry.dimensionId, dimensionMap));
        } catch (IOException e) {
            LanternGame.log().warn("Unable to save the world properties of {}: {}",
                    worldProperties.getWorldName(), e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Creates a new world from the given {@link WorldCreationSettings}. For the
     * creation of the WorldCreationSettings please see
     * {@link WorldBuilder}.
     *
     * <p>If the world already exists then the existing {@link WorldProperties}
     * are returned else a new world is created and the new WorldProperties
     * returned.</p>
     *
     * <p>Although the world is created it is not loaded at this time. Please
     * see one of the following methods for loading the world.</p>
     *
     * <ul> <li>{@link #loadWorld(String)}</li> <li>{@link #loadWorld(UUID)}
     * </li> <li>{@link #loadWorld(WorldProperties)}</li> </ul>
     *
     * @param settings the settings for creation
     * @return the new or existing world properties, if creation was successful
     */
    public Optional<WorldProperties> createWorld(WorldCreationSettings settings) {
        final LanternWorldCreationSettings settings0 = (LanternWorldCreationSettings) settings;
        if (this.worldByName.containsKey(settings.getWorldName())) {
            return Optional.empty();
        }
        // Get the next dimension id
        final int dimensionId = this.getNextFreeDimensionId();
        // Create the world properties
        final LanternWorldProperties worldProperties = new LanternWorldProperties(settings0);
        // Get the world folder
        final File worldFolder = this.getWorldFolder(dimensionId);
        worldFolder.mkdirs();
        // Store the new properties
        this.addWorldProperties(worldProperties, worldFolder, dimensionId);
        // Save the world properties to reserve the world folder
        this.saveWorldProperties(worldProperties);
        return Optional.of(worldProperties);
    }

    /**
     * Loads a {@link World} from the default storage container. If a world with
     * the given UUID is already loaded then it is returned instead.
     *
     * @param uniqueId the uuid to lookup
     * @return the world, if found
     */
    public Optional<World> loadWorld(UUID uniqueId) {
        return this.loadWorld(this.worldByUUID.get(uniqueId));
    }

    /**
     * Loads a {@link World} from the default storage container. If the world
     * associated with the given properties is already loaded then it is
     * returned instead.
     *
     * @param properties the properties of the world to load
     * @return the world, if found
     */
    public Optional<World> loadWorld(WorldProperties properties) {
        return this.loadWorld(this.worldByProperties.get(properties));
    }

    /**
     * Loads a {@link World} from the default storage container. If a world with
     * the given name is already loaded then it is returned instead.
     *
     * @param worldName the name to lookup
     * @return the world, if found
     */
    public Optional<World> loadWorld(String worldName) {
        return this.loadWorld(this.worldByName.get(worldName));
    }

    /**
     * Loads a {@link World} for the world entry if possible.
     * 
     * @param worldEntry the world entry
     * @return the world, if found
     */
    Optional<World> loadWorld(@Nullable WorldLookupEntry worldEntry) {
        if (worldEntry == null) {
            return Optional.empty();
        }
        if (worldEntry.world != null) {
            return Optional.of(worldEntry.world);
        }
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Adds the task for the world to tick it.
     */
    void addWorldTask(LanternWorld world) {
        if (this.worldThreads.containsKey(world)) {
            return;
        }
        final Thread thread = new Thread("world-" + world.getName()) {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted() && !tickEnd.isTerminated()) {
                        tickBegin.arriveAndAwaitAdvance();
                        try {
                            world.pulse();
                        } catch (Exception e) {
                            LanternGame.log().error("Error occurred while pulsing world {}",
                                    world.getName(), e);
                        } finally {
                            tickEnd.arriveAndAwaitAdvance();
                        }
                    }
                } finally {
                    tickBegin.arriveAndDeregister();
                    tickEnd.arriveAndDeregister();
                }
            }
        };
        this.worldThreads.put(world, thread);
        this.tickBegin.register();
        this.tickEnd.register();
        thread.start();
    }

    /**
     * Removes the task for the world to tick it.
     */
    void removeWorldTask(LanternWorld world) {
        if (!this.worldThreads.containsKey(world)) {
            return;
        }
        // this.tickBegin.arriveAndDeregister();
        // this.tickEnd.arriveAndDeregister();
        this.worldThreads.remove(world).interrupt();
    }

    // The current tick that is executing
    private volatile int currentTick = -1;

    // Task used to finish the current tick
    private final Runnable tickEndTask = () -> {
        // Mark ourselves as arrived so world threads automatically trigger advance once done
        int endPhase = this.tickEnd.arriveAndAwaitAdvance();
        int nextTick = this.currentTick + 1;
        if (endPhase != nextTick) {
            LanternGame.log().warn("Tick end barrier {} has advanced differently from tick begin barrier: {}",
                    endPhase, nextTick);
        }
    };

    /**
     * Pulses the world for the next tick.
     */
    public void pulse() {
        try {
            this.tickEnd.awaitAdvanceInterruptibly(this.currentTick);
            this.currentTick = this.tickBegin.arrive();

            try {
                this.executor.submit(this.tickEndTask);
            } catch (RejectedExecutionException ex) {
                this.shutdown();
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown the all the world threads and the executor.
     */
    public void shutdown() {
        this.tickBegin.forceTermination();
        this.tickEnd.forceTermination();
        for (Thread thread : this.worldThreads.values()) {
            thread.interrupt();
        }
        this.worldThreads.clear();
        this.executor.shutdown();
    }

    /**
     * Gets the world folder for the dimension id.
     * 
     * @param dimensionId the dimension id
     * @return the world folder
     */
    File getWorldFolder(int dimensionId) {
        return dimensionId == 0 ? this.rootWorldFolder : new File(
                this.rootWorldFolder, DIMENSION_PREFIX + dimensionId);
    }

    /**
     * Gets the next available dimension id.
     * 
     * @return the next dimension id
     */
    int getNextFreeDimensionId() {
        int next = 0;
        while (true) {
            next = this.dimensionMap.nextClearBit(next);
            if (this.worldByDimensionId.containsKey(next)) {
                this.dimensionMap.set(next);
            } else {
                return next;
            }
        }
    }

    /**
     * Adds the world data that was read from a level data file.
     * 
     * @param worldFolder the folder of the world
     * @param levelData the level data
     */
    void addWorld(File worldFolder, LevelData levelData) {
        final UUID uniqueId = levelData.properties.getUniqueId();
        // The world is already added
        if (this.worldByUUID.containsKey(uniqueId)) {
            return;
        }
        // Get the dimension id and make sure that it's not already used
        Integer dimensionId = levelData.dimensionId;
        if (dimensionId == null) {
            dimensionId = this.getNextFreeDimensionId();
        // Ignore the root dimension
        } else if (dimensionId != 0) {
            this.dimensionMap.set(dimensionId);
        }
        this.addWorldProperties(levelData.properties, worldFolder, dimensionId);
    }

    /**
     * Adds the world properties.
     * 
     * @param properties the properties
     * @param worldFolder the folder of the world
     * @param dimensionId the id of the world (dimension)
     */
    void addWorldProperties(LanternWorldProperties properties, File worldFolder, int dimensionId) {
        final WorldLookupEntry entry = new WorldLookupEntry(properties, worldFolder, dimensionId);
        this.worldByUUID.put(properties.getUniqueId(), entry);
        this.worldByName.put(properties.getWorldName(), entry);
        this.worldByDimensionId.put(dimensionId, entry);
        this.worldByProperties.put(properties, entry);
    }

    /**
     * Initializes the root world and the dimension id map.
     */
    void init() {
        if (this.rootWorldFolder.exists()) {
            try {
                final LevelData data = LanternWorldPropertiesIO.read(this.rootWorldFolder, null);
                // Already store the data
                this.addWorld(this.rootWorldFolder, data);
                // Get the dimension map (nullable)
                this.dimensionMap = data.dimensionMap;
            } catch (FileNotFoundException e) {
                // We can ignore this exception, because this means
                // that we have to generate the world
            } catch (IOException e) {
                LanternGame.log().error("Unable to load world folder.", e);
            }
        }
        // Create a new dimension map if it wasn't possible to load it
        if (this.dimensionMap == null) {
            this.dimensionMap = new BitSet(DIMENSION_MAP_SIZE);
        } else {
            // Get all the dimensions (worlds) that should be loaded
            final List<WorldLookupEntry> loadQueue = Lists.newArrayList();
            // Add the root dimension
            loadQueue.add(this.worldByDimensionId.get(0));
            for (int i = 0; i < DIMENSION_MAP_SIZE; i++) {
                if (this.dimensionMap.get(i)) {
                    final File folder = this.getWorldFolder(i);
                    try {
                        final LevelData data = LanternWorldPropertiesIO.read(folder, null);
                        // Store the data
                        this.addWorld(folder, data);
                        // Check if it should be loaded on startup
                        if (data.properties.loadOnStartup) {
                            loadQueue.add(this.worldByProperties.get(data.properties));
                        }
                    } catch (IOException e) {
                        LanternGame.log().error("Unable to load world (dimension) folder: {}", folder.getName(), e);
                    }
                }
            }
        }
    }
}

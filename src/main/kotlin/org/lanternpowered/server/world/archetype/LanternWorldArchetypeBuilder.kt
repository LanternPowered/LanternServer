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
package org.lanternpowered.server.world.archetype

import org.lanternpowered.server.catalog.AbstractCatalogBuilder
import org.lanternpowered.server.world.LanternWorldProperties
import org.lanternpowered.server.world.dimension.LanternDimensionType
import org.lanternpowered.server.world.portal.LanternPortalAgentType
import org.lanternpowered.api.key.NamespacedKey
import org.spongepowered.api.data.persistence.DataContainer
import org.spongepowered.api.entity.living.player.gamemode.GameMode
import org.spongepowered.api.entity.living.player.gamemode.GameModes
import org.spongepowered.api.world.SerializationBehavior
import org.spongepowered.api.world.SerializationBehaviors
import org.spongepowered.api.world.WorldArchetype
import org.spongepowered.api.world.difficulty.Difficulties
import org.spongepowered.api.world.difficulty.Difficulty
import org.spongepowered.api.world.dimension.DimensionType
import org.spongepowered.api.world.dimension.DimensionTypes
import org.spongepowered.api.world.gen.GeneratorType
import org.spongepowered.api.world.storage.WorldProperties
import org.spongepowered.api.world.teleport.PortalAgentType
import org.spongepowered.api.world.teleport.PortalAgentTypes

class LanternWorldArchetypeBuilder : AbstractCatalogBuilder<WorldArchetype, WorldArchetype.Builder>(), WorldArchetype.Builder {

    private lateinit var gameMode: GameMode
    private lateinit var difficulty: Difficulty
    private lateinit var portalAgentType: LanternPortalAgentType<*>
    private lateinit var serializationBehavior: SerializationBehavior
    private lateinit var dimensionType: LanternDimensionType<*>

    // If not specified, fall back to dimension default
    private var generatorType: GeneratorType? = null
    private var generatorSettings: DataContainer? = null
    private var keepSpawnLoaded: Boolean? = null
    private var waterEvaporates: Boolean? = null // Non-sponge property
    private var allowPlayerRespawns: Boolean? = null // Non-sponge property

    private var buildHeight = 0 // Non-sponge property
    private var hardcore = false
    private var enabled = false
    private var loadOnStartup = false
    private var generateStructures = true
    private var commandEnabled = false
    private var pvpEnabled = false
    private var generateSpawnOnLoad = false
    private var generateBonusChest = false
    private var seedProvider: SeedProvider = SeedProvider.Random

    init {
        reset()
    }

    override fun from(archetype: WorldArchetype) = apply {
        archetype as LanternWorldArchetype
        this.difficulty = archetype.difficulty
        this.hardcore = archetype.isHardcore
        this.enabled = archetype.isEnabled
        this.gameMode = archetype.gameMode
        this.keepSpawnLoaded = archetype.keepSpawnLoaded
        this.dimensionType = archetype.dimensionType
        this.generatorType = archetype.generatorType
        this.generatorSettings = archetype.generatorSettings
        this.generateStructures = archetype.areStructuresEnabled()
        this.commandEnabled = archetype.areCommandsEnabled()
        this.waterEvaporates = archetype.waterEvaporates
        this.buildHeight = archetype.buildHeight
        this.allowPlayerRespawns = archetype.allowPlayerRespawns
        this.pvpEnabled = archetype.isPVPEnabled
        this.generateSpawnOnLoad = archetype.doesGenerateSpawnOnLoad()
        this.generateBonusChest = archetype.doesGenerateBonusChest()
        this.portalAgentType = archetype.portalAgentType
        this.seedProvider = archetype.seedProvider
    }

    override fun from(properties: WorldProperties) = apply {
        properties as LanternWorldProperties
        this.difficulty = properties.difficulty
        this.hardcore = properties.isHardcore
        this.enabled = properties.isEnabled
        this.gameMode = properties.gameMode
        this.keepSpawnLoaded = properties.doesKeepSpawnLoaded()
        this.seedProvider = SeedProvider.Constant(properties.seed)
        this.dimensionType = properties.dimensionType
        this.generatorType = properties.generatorType
        this.generatorSettings = properties.generatorSettings.copy()
        this.generateStructures = properties.areStructuresEnabled()
        this.waterEvaporates = properties.doesWaterEvaporate
        this.buildHeight = properties.maxBuildHeight
        this.pvpEnabled = properties.isPVPEnabled
        this.generateSpawnOnLoad = properties.doesGenerateSpawnOnLoad()
        this.generateBonusChest = properties.doesGenerateBonusChest()
        this.portalAgentType = properties.portalAgentType
    }

    override fun enabled(state: Boolean) = apply { this.enabled = state }
    override fun loadOnStartup(state: Boolean) = apply { this.loadOnStartup = state }
    override fun keepSpawnLoaded(state: Boolean) = apply { this.keepSpawnLoaded = state }
    override fun generateSpawnOnLoad(state: Boolean) = apply { this.generateSpawnOnLoad = state }
    override fun seed(seed: Long) = apply { this.seedProvider = SeedProvider.Constant(seed) }
    override fun randomSeed() = apply { this.seedProvider = SeedProvider.Random }
    override fun gameMode(gameMode: GameMode) = apply { this.gameMode = gameMode }
    override fun generatorType(type: GeneratorType) = apply { this.generatorType = type }
    override fun dimensionType(type: DimensionType) = apply { this.dimensionType = type as LanternDimensionType<*> }
    override fun difficulty(difficulty: Difficulty) = apply { this.difficulty = difficulty }
    override fun generateStructures(state: Boolean) = apply { this.generateStructures = state }
    override fun hardcore(enabled: Boolean) = apply { this.hardcore = enabled }
    override fun generatorSettings(settings: DataContainer) = apply { this.generatorSettings = settings }
    override fun portalAgent(type: PortalAgentType) = apply { this.portalAgentType = type as LanternPortalAgentType<*> }
    override fun pvpEnabled(enabled: Boolean) = apply { this.pvpEnabled = enabled }
    override fun commandsEnabled(enabled: Boolean) = apply { this.commandEnabled = enabled }
    override fun serializationBehavior(behavior: SerializationBehavior) = apply { this.serializationBehavior = behavior }
    override fun generateBonusChest(enabled: Boolean) = apply { this.generateBonusChest = enabled }

    fun waterEvaporates(evaporates: Boolean) = apply { this.waterEvaporates = evaporates }
    fun allowPlayerRespawns(allow: Boolean) = apply { this.allowPlayerRespawns = allow }

    fun buildHeight(buildHeight: Int) = apply {
        check(buildHeight <= 256) { "the build height cannot be greater then 256" }
        this.buildHeight = buildHeight
    }

    override fun build(key: NamespacedKey): WorldArchetype {
        return LanternWorldArchetype(key,
                allowPlayerRespawns = this.allowPlayerRespawns,
                buildHeight = this.buildHeight,
                commandsEnabled = this.commandEnabled,
                difficulty = this.difficulty,
                dimensionType = this.dimensionType,
                enabled = this.enabled,
                gameMode = this.gameMode,
                generateStructures = this.generateStructures,
                generateSpawnOnLoad = this.generateSpawnOnLoad,
                generateBonusChest = this.generateBonusChest,
                generatorSettings = this.generatorSettings,
                generatorType = this.generatorType,
                hardcore = this.hardcore,
                keepSpawnLoaded = this.keepSpawnLoaded,
                loadsOnStartup = this.loadOnStartup,
                seedProvider = this.seedProvider,
                serializationBehavior = this.serializationBehavior,
                portalAgentType = this.portalAgentType,
                pvpEnabled = this.pvpEnabled,
                waterEvaporates = this.waterEvaporates
        )
    }

    override fun reset() = apply {
        this.gameMode = GameModes.SURVIVAL.get()
        this.difficulty = Difficulties.NORMAL.get()
        this.portalAgentType = PortalAgentTypes.DEFAULT.get() as LanternPortalAgentType<*>
        this.hardcore = false
        this.keepSpawnLoaded = null
        this.loadOnStartup = false
        this.generateSpawnOnLoad = true
        this.enabled = true
        this.generateStructures = true
        this.commandEnabled = true
        this.dimensionType = DimensionTypes.OVERWORLD.get() as LanternDimensionType<*>
        this.seedProvider = SeedProvider.Random
        this.generatorType = null
        this.generatorSettings = null
        this.waterEvaporates = null
        this.buildHeight = 256
        this.serializationBehavior = SerializationBehaviors.AUTOMATIC.get()
        this.generateBonusChest = false
    }
}

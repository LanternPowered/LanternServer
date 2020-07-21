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
@file:JvmName("FireworkShapeRegistry")
package org.lanternpowered.server.registry.type.data

import org.lanternpowered.api.ResourceKey
import org.lanternpowered.api.effect.firework.FireworkShape
import org.lanternpowered.server.catalog.DefaultCatalogType
import org.lanternpowered.server.registry.internalCatalogTypeRegistry

@get:JvmName("get")
val FireworkShapeRegistry = internalCatalogTypeRegistry<FireworkShape> {
    fun register(id: String) =
            register(LanternFireworkShape(ResourceKey.minecraft(id)))

    register("ball")
    register("large_ball")
    register("star")
    register("creeper")
    register("burst")
}

private class LanternFireworkShape(key: ResourceKey) : DefaultCatalogType(key), FireworkShape
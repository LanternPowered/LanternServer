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
@file:JvmName("RabbitTypeRegistry")
package org.lanternpowered.server.registry.type.data

import org.lanternpowered.api.key.NamespacedKey
import org.lanternpowered.api.key.minecraftKey
import org.lanternpowered.server.catalog.DefaultCatalogType
import org.lanternpowered.server.registry.internalCatalogTypeRegistry
import org.spongepowered.api.data.type.RabbitType

@get:JvmName("get")
val RabbitTypeRegistry = internalCatalogTypeRegistry<RabbitType> {
    fun register(id: String, internalId: Int = -1) {
        val type = LanternRabbitType(minecraftKey(id))
        if (internalId == -1) {
            register(type)
        } else {
            register(internalId, type)
        }
    }

    register("brown")
    register("white")
    register("black")
    register("black_and_white")
    register("gold")
    register("salt_and_pepper")
    register("killer", 99)
}

private class LanternRabbitType(key: NamespacedKey) : DefaultCatalogType(key), RabbitType

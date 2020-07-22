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
package org.lanternpowered.server.registry.type.data

import org.lanternpowered.server.catalog.DefaultCatalogType
import org.lanternpowered.server.registry.internalCatalogTypeRegistry
import org.lanternpowered.api.key.NamespacedKey
import org.lanternpowered.api.key.minecraftKey
import org.spongepowered.api.data.type.PickupRule

val PickupRuleRegistry = internalCatalogTypeRegistry<PickupRule> {
    fun register(id: String) =
            register(LanternPickupRule(minecraftKey(id)))

    register("disallowed")
    register("allowed")
    register("creative_only")
}

private class LanternPickupRule(key: NamespacedKey) : DefaultCatalogType(key), PickupRule

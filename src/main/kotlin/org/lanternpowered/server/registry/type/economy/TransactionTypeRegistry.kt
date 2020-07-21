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
package org.lanternpowered.server.registry.type.economy

import org.lanternpowered.api.ResourceKey
import org.lanternpowered.api.registry.catalogTypeRegistry
import org.lanternpowered.server.catalog.DefaultCatalogType
import org.spongepowered.api.service.economy.transaction.TransactionType

val TransactionTypeRegistry = catalogTypeRegistry<TransactionType> {
    fun register(id: String) =
            register(LanternTransactionType(ResourceKey.sponge(id)))

    register("deposit")
    register("withdraw")
    register("transfer")
}

private class LanternTransactionType(key: ResourceKey) : DefaultCatalogType(key), TransactionType
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
package org.lanternpowered.server.world.difficulty

import org.lanternpowered.api.catalog.CatalogKey
import org.lanternpowered.server.catalog.DefaultCatalogType
import org.lanternpowered.server.catalog.InternalCatalogType
import org.lanternpowered.server.text.translation.Translated
import org.spongepowered.api.text.translation.Translatable
import org.spongepowered.api.world.difficulty.Difficulty

class LanternDifficulty(key: CatalogKey, override val internalId: Int) :
        DefaultCatalogType(key), Difficulty, InternalCatalogType,
        Translatable by Translated("options.difficulty.${key.value}")

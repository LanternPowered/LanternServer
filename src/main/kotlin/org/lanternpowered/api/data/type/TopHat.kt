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
package org.lanternpowered.api.data.type

import org.lanternpowered.api.catalog.CatalogType
import org.lanternpowered.api.data.DyeColor
import org.lanternpowered.api.catalog.CatalogedBy

/**
 * Represents a [TopHat].
 */
@CatalogedBy(TopHats::class)
interface TopHat : CatalogType {

    /**
     * The [DyeColor], if present.
     */
    val dyeColor: DyeColor?
}

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
package org.lanternpowered.server.scoreboard

import org.lanternpowered.api.catalog.CatalogKey
import org.lanternpowered.server.catalog.DefaultCatalogType
import org.spongepowered.api.scoreboard.Visibility
import org.spongepowered.api.text.translation.FixedTranslation

class LanternVisibility(key: CatalogKey) : DefaultCatalogType(key), Visibility {

    override fun getTranslation() = FixedTranslation(this.key.value) // TODO
}

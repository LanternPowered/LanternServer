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
package org.lanternpowered.server.world.update

import org.lanternpowered.server.catalog.DefaultCatalogType
import org.spongepowered.api.CatalogKey
import org.spongepowered.api.scheduler.TaskPriority

class LanternTaskPriority(key: CatalogKey, val value: Int) : DefaultCatalogType(key), TaskPriority

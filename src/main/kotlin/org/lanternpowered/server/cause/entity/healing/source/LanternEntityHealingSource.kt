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
package org.lanternpowered.server.cause.entity.healing.source

import org.spongepowered.api.event.cause.entity.health.source.common.AbstractEntityHealingSource
import org.spongepowered.api.event.cause.entity.health.source.common.AbstractEntityHealingSourceBuilder

internal class LanternEntityHealingSource(builder: AbstractEntityHealingSourceBuilder<*, *>) :
        AbstractEntityHealingSource(builder)

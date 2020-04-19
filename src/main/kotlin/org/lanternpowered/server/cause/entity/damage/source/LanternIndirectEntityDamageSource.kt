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
package org.lanternpowered.server.cause.entity.damage.source

import org.spongepowered.api.entity.Entity
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource

internal class LanternIndirectEntityDamageSource(builder: LanternIndirectEntityDamageSourceBuilder) :
        LanternEntityDamageSource(builder), IndirectEntityDamageSource {

    private val indirectSource: Entity = builder.indirect!!

    override fun getIndirectSource(): Entity = this.indirectSource
}

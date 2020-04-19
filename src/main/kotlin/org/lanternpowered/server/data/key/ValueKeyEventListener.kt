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
package org.lanternpowered.server.data.key

import org.lanternpowered.server.event.LanternEventListener
import org.spongepowered.api.data.DataHolder
import org.spongepowered.api.data.Key
import org.spongepowered.api.event.EventListener
import org.spongepowered.api.event.data.ChangeDataHolderEvent

class ValueKeyEventListener internal constructor(
        private val listener: EventListener<ChangeDataHolderEvent.ValueChange>, val dataHolderFilter: (DataHolder) -> Boolean, val key: Key<*>
) : LanternEventListener<ChangeDataHolderEvent.ValueChange> {

    override fun getHandle() = this.listener

    override fun handle(event: ChangeDataHolderEvent.ValueChange) {
        this.listener.handle(event)
    }
}

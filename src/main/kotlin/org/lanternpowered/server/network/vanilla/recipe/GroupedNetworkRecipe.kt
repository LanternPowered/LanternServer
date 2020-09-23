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
package org.lanternpowered.server.network.vanilla.recipe

import org.lanternpowered.server.network.buffer.ByteBuffer
import org.lanternpowered.server.network.packet.codec.CodecContext

abstract class GroupedNetworkRecipe(id: String, type: String, private val group: String?) : NetworkRecipe(id, type) {

    override fun writeProperties(ctx: CodecContext, buf: ByteBuffer) {
        buf.writeString(this.group ?: "")
    }
}

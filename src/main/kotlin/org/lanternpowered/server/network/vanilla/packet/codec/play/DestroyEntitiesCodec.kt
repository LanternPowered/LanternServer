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
package org.lanternpowered.server.network.vanilla.packet.codec.play

import org.lanternpowered.server.network.buffer.ByteBuffer
import org.lanternpowered.server.network.packet.PacketEncoder
import org.lanternpowered.server.network.packet.codec.CodecContext
import org.lanternpowered.server.network.vanilla.packet.type.play.DestroyEntitiesPacket

object DestroyEntitiesCodec : PacketEncoder<DestroyEntitiesPacket> {

    override fun encode(context: CodecContext, packet: DestroyEntitiesPacket): ByteBuffer {
        val buf = context.byteBufAlloc().buffer()
        val entityIds = packet.entityIds
        buf.writeVarInt(entityIds.size)
        for (entityId in entityIds) {
            buf.writeVarInt(entityId)
        }
        return buf
    }
}
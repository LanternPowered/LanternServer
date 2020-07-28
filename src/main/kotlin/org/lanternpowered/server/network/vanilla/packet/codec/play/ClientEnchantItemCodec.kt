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
import org.lanternpowered.server.network.packet.PacketDecoder
import org.lanternpowered.server.network.packet.codec.CodecContext
import org.lanternpowered.server.network.vanilla.packet.type.play.ClientEnchantItemPacket

object ClientEnchantItemCodec : PacketDecoder<ClientEnchantItemPacket> {

    override fun decode(context: CodecContext, buf: ByteBuffer): ClientEnchantItemPacket {
        val windowId = buf.readByte().toInt()
        val slot = buf.readByte().toInt()
        return ClientEnchantItemPacket(windowId, slot)
    }
}
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
import org.lanternpowered.server.network.vanilla.packet.type.play.SetActiveScoreboardObjectivePacket
import org.lanternpowered.server.registry.type.scoreboard.DisplaySlotRegistry

object SetActiveScoreboardObjectiveCodec : PacketEncoder<SetActiveScoreboardObjectivePacket> {

    override fun encode(context: CodecContext, packet: SetActiveScoreboardObjectivePacket): ByteBuffer {
        val buf = context.byteBufAlloc().buffer()
        buf.writeByte(DisplaySlotRegistry.getId(packet.displaySlot).toByte())
        buf.writeString(packet.objectiveName ?: "")
        return buf
    }
}
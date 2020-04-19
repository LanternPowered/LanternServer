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
package org.lanternpowered.server.network.vanilla.message.codec.play;

import io.netty.handler.codec.CodecException;
import org.lanternpowered.server.network.buffer.ByteBuffer;
import org.lanternpowered.server.network.buffer.contextual.ContextualValueTypes;
import org.lanternpowered.server.network.message.codec.Codec;
import org.lanternpowered.server.network.message.codec.CodecContext;
import org.lanternpowered.server.network.vanilla.message.type.play.MessagePlayOutTabComplete;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;

public final class CodecPlayOutTabComplete implements Codec<MessagePlayOutTabComplete> {

    @Override
    public ByteBuffer encode(CodecContext context, MessagePlayOutTabComplete message) throws CodecException {
        final ByteBuffer buf = context.byteBufAlloc().buffer();
        buf.writeVarInt(message.getId());
        buf.writeVarInt(message.getStart());
        buf.writeVarInt(message.getLength());
        final List<MessagePlayOutTabComplete.Match> matches = message.getMatches();
        buf.writeVarInt(matches.size());
        for (MessagePlayOutTabComplete.Match match : matches) {
            buf.writeString(match.getValue());
            final Optional<Text> tooltip = match.getTooltip();
            buf.writeBoolean(tooltip.isPresent());
            tooltip.ifPresent(text -> context.write(buf, ContextualValueTypes.TEXT, text));
        }
        return buf;
    }
}

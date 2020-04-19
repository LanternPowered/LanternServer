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
import org.lanternpowered.server.network.vanilla.message.type.play.MessagePlayOutTabListHeaderAndFooter;
import org.spongepowered.api.text.Text;

public final class CodecPlayOutTabListHeaderAndFooter implements Codec<MessagePlayOutTabListHeaderAndFooter> {

    // This is the only text type that can be empty on the client
    // for the result of #getFormattedText
    private static final String EMPTY_TEXT = "{\"translate\":\"\"}";

    @Override
    public ByteBuffer encode(CodecContext context, MessagePlayOutTabListHeaderAndFooter message) throws CodecException {
        ByteBuffer buf = context.byteBufAlloc().buffer();
        Text header = message.getHeader();
        Text footer = message.getFooter();
        if (header != null) {
            context.write(buf, ContextualValueTypes.TEXT, header);
        } else {
            buf.writeString(EMPTY_TEXT);
        }
        if (footer != null) {
            context.write(buf, ContextualValueTypes.TEXT, footer);
        } else {
            buf.writeString(EMPTY_TEXT);
        }
        return buf;
    }
}

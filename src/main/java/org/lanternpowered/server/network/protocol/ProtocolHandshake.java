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
package org.lanternpowered.server.network.protocol;

import org.lanternpowered.server.network.vanilla.message.codec.handshake.CodecHandshakeIn;
import org.lanternpowered.server.network.vanilla.message.handler.handshake.HandlerHandshakeIn;
import org.lanternpowered.server.network.vanilla.message.type.handshake.MessageHandshakeIn;

final class ProtocolHandshake extends ProtocolBase {

    ProtocolHandshake() {
        inbound().bind(CodecHandshakeIn.class, MessageHandshakeIn.class)
                .bindHandler(new HandlerHandshakeIn());
    }
}

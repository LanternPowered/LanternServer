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
package org.lanternpowered.server.network.vanilla.message.type.play;

import static com.google.common.base.Preconditions.checkNotNull;

import org.lanternpowered.server.network.message.Message;

import java.util.Set;

public final class MessagePlayInOutRegisterChannels implements Message {

    private final Set<String> channels;

    /**
     * Creates a new register channels message.
     * 
     * @param channels the channels
     */
    public MessagePlayInOutRegisterChannels(Set<String> channels) {
        this.channels = checkNotNull(channels, "channels");
    }

    /**
     * Gets the channels.
     * 
     * @return the channels
     */
    public Set<String> getChannels() {
        return this.channels;
    }

}

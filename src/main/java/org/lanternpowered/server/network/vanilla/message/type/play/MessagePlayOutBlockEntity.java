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

import org.lanternpowered.server.network.message.Message;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.math.vector.Vector3i;

public final class MessagePlayOutBlockEntity implements Message {

    private final String type;
    private final Vector3i position;
    private final DataView tileData;

    public MessagePlayOutBlockEntity(String type, Vector3i position, DataView tileData) {
        this.type = type;
        this.position = position;
        this.tileData = tileData;
    }

    public String getType() {
        return this.type;
    }

    public Vector3i getPosition() {
        return this.position;
    }

    public DataView getTileData() {
        return this.tileData;
    }
}

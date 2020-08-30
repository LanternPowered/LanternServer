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
package org.lanternpowered.server.inventory.client;

import static org.lanternpowered.server.text.translation.TranslationHelper.t;

import org.lanternpowered.server.inventory.container.ClientWindowTypes;
import org.lanternpowered.server.network.packet.Packet;
import org.lanternpowered.server.network.vanilla.packet.type.play.OpenWindowPacket;
import org.spongepowered.api.text.Text;

public class CraftingTableClientContainer extends ClientContainer {

    private static final int[] TOP_SLOT_FLAGS = new int[] {
            FLAG_REVERSE_SHIFT_INSERTION | FLAG_DISABLE_SHIFT_INSERTION | FLAG_IGNORE_DOUBLE_CLICK, // Output slot
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 1
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 2
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 3
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 4
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 5
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 6
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 7
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 8
            FLAG_DISABLE_SHIFT_INSERTION, // Input slot 9
    };
    private static final int[] ALL_SLOT_FLAGS = compileAllSlotFlags(TOP_SLOT_FLAGS);

    static class Title {
        static final Text DEFAULT = t("container.crafting");
    }

    public CraftingTableClientContainer() {
        super(Title.DEFAULT);
    }

    @Override
    protected Packet createInitMessage() {
        return new OpenWindowPacket(containerId, ClientWindowTypes.CRAFTING, getTitle());
    }

    @Override
    protected int[] getTopSlotFlags() {
        return TOP_SLOT_FLAGS;
    }

    @Override
    protected int[] getSlotFlags() {
        return ALL_SLOT_FLAGS;
    }
}

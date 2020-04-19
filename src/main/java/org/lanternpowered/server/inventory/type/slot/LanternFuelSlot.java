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
package org.lanternpowered.server.inventory.type.slot;

import org.lanternpowered.server.inventory.AbstractContainerSlot;
import org.spongepowered.api.item.inventory.slot.FuelSlot;

public class LanternFuelSlot extends LanternInputSlot implements FuelSlot {

    @Override
    protected AbstractContainerSlot constructContainerSlot() {
        return new ContainerSlot();
    }

    protected static class ContainerSlot extends LanternInputSlot.ContainerSlot implements FuelSlot {
    }
}

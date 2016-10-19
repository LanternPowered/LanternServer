/*
 * This file is part of LanternServer, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the Software), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.server.inventory;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.OrderedInventory;

import java.util.Optional;

import javax.annotation.Nullable;

public class AltParentProxyOrderedInventory extends AltParentProxyInventory implements OrderedInventory {

    protected AltParentProxyOrderedInventory(@Nullable Inventory parent, OrderedInventory delegate) {
        super(parent, delegate);
    }

    @Override
    public Optional<ItemStack> poll(SlotIndex index) {
        return ((OrderedInventory) this.delegate).poll(index);
    }

    @Override
    public Optional<ItemStack> poll(SlotIndex index, int limit) {
        return ((OrderedInventory) this.delegate).poll(index, limit);
    }

    @Override
    public Optional<ItemStack> peek(SlotIndex index) {
        return ((OrderedInventory) this.delegate).peek(index);
    }

    @Override
    public Optional<ItemStack> peek(SlotIndex index, int limit) {
        return ((OrderedInventory) this.delegate).peek(index, limit);
    }

    @Override
    public InventoryTransactionResult set(SlotIndex index, ItemStack stack) {
        return ((OrderedInventory) this.delegate).set(index, stack);
    }

    @Override
    public Optional<Slot> getSlot(SlotIndex index) {
        return ((OrderedInventory) this.delegate).getSlot(index);
    }
}

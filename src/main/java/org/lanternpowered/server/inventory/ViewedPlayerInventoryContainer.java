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

import org.lanternpowered.server.inventory.vanilla.LanternPlayerInventory;
import org.lanternpowered.server.inventory.vanilla.ViewedPlayerInventory;
import org.spongepowered.api.item.inventory.crafting.CraftingInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

/**
 * Represents a player inventory container which can be viewed.
 *
 * <p>All the top inventory slots will be put into a chest inventory.</p>
 */
final class ViewedPlayerInventoryContainer extends ViewedPlayerInventory {

    ViewedPlayerInventoryContainer(LanternPlayerInventory playerInventory, AbstractChildrenInventory openInventory) {
        super(playerInventory, openInventory);
    }

    @Override
    protected Layout buildLayout() {
        final CraftingInventory craftingInventory = (CraftingInventory) getOpenInventory()
                .query(QueryOperationTypes.TYPE.of(CraftingInventory.class)).first();
        final AbstractGridInventory craftingGridInventory = (AbstractGridInventory) craftingInventory.getCraftingGrid();

        // Expand the player inventory layout with crafting grid and result
        final Layout layout = super.buildLayout();
        // Crafting grid
        for (int x = 0; x < craftingGridInventory.getColumns(); x++) {
            for (int y = 0; y < craftingGridInventory.getRows(); y++) {
                layout.bind(4 + x, 1 + y, craftingGridInventory.getSlot(x, y).get());
            }
        }
        // Crafting result
        layout.bind(7, 1, craftingInventory.getResult());
        return layout;
    }
}

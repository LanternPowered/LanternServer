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
package org.lanternpowered.server.network.vanilla.message.handler.play;

import org.lanternpowered.server.entity.living.player.LanternPlayer;
import org.lanternpowered.server.inventory.AbstractSlot;
import org.lanternpowered.server.inventory.LanternItemStack;
import org.lanternpowered.server.network.NetworkContext;
import org.lanternpowered.server.network.message.handler.Handler;
import org.lanternpowered.server.network.vanilla.message.type.play.MessagePlayInModifyBook;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemTypes;

public class HandlerPlayInEditBook implements Handler<MessagePlayInModifyBook.Edit> {

    @Override
    public void handle(NetworkContext context, MessagePlayInModifyBook.Edit message) {
        final LanternPlayer player = context.getSession().getPlayer();
        final AbstractSlot slot = player.getInventory().getHotbar().getSelectedSlot();

        final LanternItemStack itemStack = slot.peek();
        if (itemStack.getType() == ItemTypes.WRITABLE_BOOK) {
            itemStack.offer(Keys.PLAIN_BOOK_PAGES, message.getPages());
            slot.set(itemStack);
        }
    }
}

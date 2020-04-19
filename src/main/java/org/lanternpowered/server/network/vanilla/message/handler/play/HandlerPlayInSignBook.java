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
import org.lanternpowered.server.inventory.LanternItemStack;
import org.lanternpowered.server.network.NetworkContext;
import org.lanternpowered.server.network.message.handler.Handler;
import org.lanternpowered.server.network.vanilla.message.type.play.MessagePlayInModifyBook;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.stream.Collectors;

public class HandlerPlayInSignBook implements Handler<MessagePlayInModifyBook.Sign> {

    @Override
    public void handle(NetworkContext context, MessagePlayInModifyBook.Sign message) {
        final LanternPlayer player = context.getSession().getPlayer();

        LanternItemStack itemStack = slot.peek();
        if (itemStack.isNotEmpty() && itemStack.getType() == ItemTypes.WRITABLE_BOOK) {
            final ItemStack writtenBookStack = new LanternItemStack(ItemTypes.WRITTEN_BOOK);
            itemStack.getValues().stream()
                    .filter(value -> value.getKey() != Keys.PLAIN_BOOK_PAGES)
                    .forEach(writtenBookStack::offer);
            writtenBookStack.offer(Keys.BOOK_PAGES, message.getPages().stream().map(Text::of).collect(Collectors.toList()));
            writtenBookStack.offer(Keys.BOOK_AUTHOR, Text.of(message.getAuthor()));
            writtenBookStack.offer(Keys.DISPLAY_NAME, Text.of(message.getTitle()));
            slot.set(writtenBookStack);
        }
    }
}

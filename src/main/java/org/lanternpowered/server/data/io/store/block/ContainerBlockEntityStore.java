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
package org.lanternpowered.server.data.io.store.block;

import static org.lanternpowered.server.data.io.store.InventorySnapshotSerializer.SLOT;

import org.lanternpowered.server.block.entity.vanilla.ContainerBlockEntity;
import org.lanternpowered.server.data.io.store.ObjectSerializer;
import org.lanternpowered.server.data.io.store.ObjectSerializerRegistry;
import org.lanternpowered.server.data.io.store.SimpleValueContainer;
import org.lanternpowered.server.inventory.LanternItemStack;
import org.lanternpowered.server.text.LanternTexts;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryProperties;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.slot.SlotIndex;

import java.util.ArrayList;
import java.util.List;

public class ContainerBlockEntityStore<T extends ContainerBlockEntity> extends BlockEntityObjectStore<T> {

    private static final DataQuery DISPLAY_NAME = DataQuery.of("CustomName");
    private static final DataQuery ITEMS = DataQuery.of("Items");

    @Override
    public void deserialize(T object, DataView dataView) {
        final List<DataView> itemViews = dataView.getViewList(ITEMS).orElse(null);
        if (itemViews != null) {
            dataView.remove(ITEMS);
            final Inventory inventory = object.getInventory();
            final ObjectSerializer<LanternItemStack> itemStackSerializer = ObjectSerializerRegistry.get().get(LanternItemStack.class).get();
            for (DataView itemView : itemViews) {
                final int slot = itemView.getByte(SLOT).get() & 0xff;
                final LanternItemStack itemStack = itemStackSerializer.deserialize(itemView);
                inventory.set(SlotIndex.of(slot), itemStack);
            }
        }
        super.deserialize(object, dataView);
    }

    @Override
    public void serialize(T object, DataView dataView) {
        super.serialize(object, dataView);
        final ObjectSerializer<LanternItemStack> itemStackSerializer =  ObjectSerializerRegistry.get().get(LanternItemStack.class).get();
        final List<DataView> itemViews = new ArrayList<>();
        final Inventory inventory = object.getInventory();
        final Iterable<Slot> slots = inventory.slots();
        for (Slot slot : slots) {
            final ItemStack itemStack = slot.peek();
            if (itemStack.isEmpty()) {
                continue;
            }
            final DataView itemView = itemStackSerializer.serialize((LanternItemStack) itemStack);
            itemView.set(SLOT, (byte) inventory.getProperty(InventoryProperties.SLOT_INDEX).get().getIndex());
            itemViews.add(itemView);
        }
        dataView.set(ITEMS, itemViews);
    }

    @Override
    public void deserializeValues(T object, SimpleValueContainer valueContainer, DataView dataView) {
        dataView.getString(DISPLAY_NAME).ifPresent(name -> valueContainer.set(Keys.DISPLAY_NAME, LanternTexts.fromLegacy(name)));
        super.deserializeValues(object, valueContainer, dataView);
    }

    @Override
    public void serializeValues(T object, SimpleValueContainer valueContainer, DataView dataView) {
        valueContainer.remove(Keys.DISPLAY_NAME).ifPresent(text ->
                dataView.set(DISPLAY_NAME, LanternTexts.toLegacy(text)));
        super.serializeValues(object, valueContainer, dataView);
    }
}

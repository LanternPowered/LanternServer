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
package org.lanternpowered.server.block.behavior.vanilla;

import org.lanternpowered.server.behavior.Behavior;
import org.lanternpowered.server.behavior.BehaviorContext;
import org.lanternpowered.server.behavior.BehaviorResult;
import org.lanternpowered.server.behavior.ContextKeys;
import org.lanternpowered.server.behavior.pipeline.BehaviorPipeline;
import org.lanternpowered.server.block.behavior.types.InteractWithBlockBehavior;
import org.lanternpowered.server.block.entity.vanilla.LanternJukebox;
import org.spongepowered.api.block.tileentity.Jukebox;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.property.item.MusicDiscProperty;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.world.Location;

import java.util.Optional;

public class JukeboxInteractionBehavior implements InteractWithBlockBehavior {

    @Override
    public BehaviorResult tryInteract(BehaviorPipeline<Behavior> pipeline, BehaviorContext context) {
        final Location location = context.requireContext(ContextKeys.INTERACTION_LOCATION);
        final Optional<TileEntity> optTile = location.getTileEntity();
        if (optTile.isPresent()) {
            final TileEntity tile = optTile.get();
            if (tile instanceof Jukebox) {
                final LanternJukebox jukebox = (LanternJukebox) tile;
                final Optional<Entity> optEjectedItem = jukebox.ejectRecordItem();
                boolean success = false;
                if (optEjectedItem.isPresent()) {
                    final Entity entity = optEjectedItem.get();
                    entity.getWorld().spawnEntity(optEjectedItem.get());
                    // TODO: Include the entity in the behavior context
                    success = true;
                }
                final Optional<ItemStack> optItemStack = context.getContext(ContextKeys.USED_ITEM_STACK);
                if (optItemStack.isPresent()) {
                    final ItemStack itemStack = optItemStack.get();
                    final MusicDiscProperty property = itemStack.getProperty(MusicDiscProperty.class).orElse(null);
                    final MusicDisc recordType = property == null ? null : property.getValue();
                    if (recordType != null) {
                        final ItemStackSnapshot oldSnapshot = itemStack.createSnapshot();
                        itemStack.setQuantity(itemStack.getQuantity() - 1);
                        final ItemStackSnapshot newSnapshot = itemStack.createSnapshot();
                        context.getContext(ContextKeys.PLAYER).ifPresent(player -> {
                            if (!player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET).equals(GameModes.CREATIVE)) {
                                context.getContext(ContextKeys.USED_SLOT).ifPresent(slot -> context.addSlotChange(
                                        new SlotTransaction(slot, oldSnapshot, newSnapshot)));
                            }
                        });
                        itemStack.setQuantity(1);
                        jukebox.insert(itemStack);
                        jukebox.play();
                        success = true;
                    }
                }
                if (success) {
                    return BehaviorResult.SUCCESS;
                }
            }
        }
        return BehaviorResult.PASS;
    }
}

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
package org.lanternpowered.server.block.entity.vanilla;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.lanternpowered.server.block.entity.BlockEntityCreationData;
import org.lanternpowered.server.block.entity.ICarrierBlockEntity;
import org.lanternpowered.server.block.entity.LanternBlockEntity;
import org.lanternpowered.server.block.state.BlockStateProperties;
import org.lanternpowered.server.game.Lantern;
import org.lanternpowered.server.inventory.vanilla.VanillaInventoryArchetypes;
import org.lanternpowered.server.inventory.vanilla.block.JukeboxInventory;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.Jukebox;
import org.spongepowered.api.block.entity.carrier.CarrierBlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.property.Properties;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.BlockEntityInventory;
import org.spongepowered.api.world.Location;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

public final class LanternJukebox extends LanternBlockEntity implements Jukebox, ICarrierBlockEntity {

    // The internal inventory of the jukebox
    private JukeboxInventory inventory = VanillaInventoryArchetypes.JUKEBOX.builder()
            .withCarrier(this).build(Lantern.getMinecraftPlugin());
    private boolean playing;

    public LanternJukebox(BlockEntityCreationData creationData) {
        super(creationData);
    }

    @Override
    public void play() {
        final ItemStack recordItem = this.inventory.getRawItemStack();
        if (recordItem.isEmpty()) {
            return;
        }
        this.playing = true;
        final Location location = getLocation();
        final MusicDisc musicDisc = recordItem.getProperty(Properties.MUSIC_DISC).orElse(null);
        if (musicDisc != null) {
            location.getWorld().playMusicDisc(location.getBlockPosition(), musicDisc);
        }
    }

    @Override
    public void stop() {
        if (!this.playing) {
            return;
        }
        this.playing = false;
        final Location location = getLocation();
        location.getWorld().stopMusicDisc(location.getBlockPosition());
    }

    @Override
    public void eject() {
        ejectRecordItem().ifPresent(entity -> entity.getWorld().spawnEntity(entity));
    }

    private void updateBlockState() {
        final Location location = getLocation();
        final BlockState block = location.getBlock();
        location.setBlock(block.withStateProperty(BlockStateProperties.HAS_MUSIC_DISC, this.inventory.totalItems() > 0).orElse(block));
    }

    /**
     * Resets the record {@link ItemStackSnapshot} and
     * returns it. If present.
     *
     * @return The record item
     */
    public Optional<Entity> ejectRecordItem() {
        final ItemStack recordItem = this.inventory.getRawItemStack();
        if (recordItem.isEmpty()) {
            return Optional.empty();
        }
        stop();
        final Location location = getLocation();
        final Vector3d entityPosition = location.getBlockPosition().toDouble().add(0.5, 0.9, 0.5);
        final Entity item = location.getWorld().createEntity(EntityTypes.ITEM, entityPosition);
        item.offer(Keys.VELOCITY, new Vector3d(0, 0.1, 0));
        item.offer(Keys.ITEM_STACK_SNAPSHOT, recordItem.createSnapshot());
        this.inventory.clear();
        updateBlockState();
        return Optional.of(item);
    }

    @Override
    public void insert(ItemStack record) {
        checkNotNull(record, "record");
        eject();
        checkState(this.inventory.set(record).getType() == InventoryTransactionResult.Type.SUCCESS,
                "Invalid record item stack: " + record);
        updateBlockState();
    }

    @Override
    public BlockEntityInventory<CarrierBlockEntity> getInventory() {
        return this.inventory;
    }
}

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
package org.lanternpowered.server.network.entity.vanilla;

import static org.lanternpowered.server.network.vanilla.packet.codec.play.CodecUtils.wrapAngle;

import com.google.common.base.Objects;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.lanternpowered.server.data.key.LanternKeys;
import org.lanternpowered.server.entity.LanternEntity;
import org.lanternpowered.server.entity.LanternLiving;
import org.lanternpowered.server.entity.Pose;
import org.lanternpowered.server.entity.event.CollectEntityEvent;
import org.lanternpowered.server.entity.event.EntityEvent;
import org.lanternpowered.server.inventory.IInventory;
import org.lanternpowered.server.network.entity.AbstractEntityProtocol;
import org.lanternpowered.server.network.entity.EntityProtocolUpdateContext;
import org.lanternpowered.server.network.entity.parameter.DefaultParameterList;
import org.lanternpowered.server.network.entity.parameter.EmptyParameterList;
import org.lanternpowered.server.network.entity.parameter.ParameterList;
import org.lanternpowered.server.network.vanilla.packet.type.play.DestroyEntitiesPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityCollectItemPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityHeadLookPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityLookPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityLookAndRelativeMovePacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityMetadataPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityRelativeMovePacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityTeleportPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityVelocityPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.SetEntityPassengersPacket;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class EntityProtocol<E extends LanternEntity> extends AbstractEntityProtocol<E> {

    private static class Holder {

        private final static EquipmentType[] EQUIPMENT_TYPES =
                {
                        EquipmentTypes.MAIN_HAND.get(),
                        EquipmentTypes.OFF_HAND.get(),
                        EquipmentTypes.BOOTS.get(),
                        EquipmentTypes.LEGGINGS.get(),
                        EquipmentTypes.CHESTPLATE.get(),
                        EquipmentTypes.HEADWEAR.get()
                };

        /*

        TODO: Fix this

        private final static QueryOperation<?>[] EQUIPMENT_QUERIES;

        static {
            EQUIPMENT_QUERIES = new QueryOperation[EQUIPMENT_TYPES.length];
            for (int i = 0; i < EQUIPMENT_QUERIES.length; i++) {
                EQUIPMENT_QUERIES[i] = QueryOperationTypes.PROPERTY.of(
                        PropertyMatcher.of(InventoryProperties.EQUIPMENT_TYPE, EQUIPMENT_TYPES[i]));
            }
        }
        */
    }

    private long lastX;
    private long lastY;
    private long lastZ;

    protected byte lastYaw;
    protected byte lastPitch;
    protected byte lastHeadYaw;

    private double lastVelX;
    private double lastVelY;
    private double lastVelZ;

    protected byte lastFlags;
    private boolean lastSilent;
    private short lastAirLevel;
    private boolean lastCustomNameVisible;

    @Nullable private Optional<Text> lastCustomName;
    @Nullable private Pose lastPose;

    protected IntSet lastPassengers = IntSets.EMPTY_SET;

    private final Int2ObjectMap<ItemStack> lastEquipment = new Int2ObjectOpenHashMap<>();

    public EntityProtocol(E entity) {
        super(entity);
    }

    @Override
    protected void destroy(EntityProtocolUpdateContext context) {
        context.sendToAllExceptSelf(new DestroyEntitiesPacket(getRootEntityId()));
    }

    protected void spawnWithMetadata(EntityProtocolUpdateContext context) {
        final ParameterList parameterList = new DefaultParameterList();
        spawn(parameterList);
        if (!parameterList.isEmpty()) {
            context.sendToAll(() -> new EntityMetadataPacket(getRootEntityId(), parameterList));
        }
    }

    protected void spawnWithEquipment(EntityProtocolUpdateContext context) {
        if (this.entity.isOnGround()) {
            context.sendToAllExceptSelf(() -> new EntityRelativeMovePacket(getRootEntityId(), 0, 0, 0, true));
        }
        if (hasEquipment() && this.entity instanceof Carrier) {
            final IInventory inventory = (IInventory) ((Carrier) this.entity).getInventory();
            // TODO: Fix
            /*
            for (int i = 0; i < Holder.EQUIPMENT_TYPES.length; i++) {
                final LanternItemStack itemStack = inventory.query(Holder.EQUIPMENT_QUERIES[i]).first().peek();
                final int slotIndex = i;
                if (itemStack.isNotEmpty()) {
                    context.sendToAllExceptSelf(() -> new MessagePlayOutEntityEquipment(getRootEntityId(), slotIndex, itemStack));
                }
            }
            */
        }
    }

    @Override
    protected void update(EntityProtocolUpdateContext context) {
        final Vector3d rot = this.entity.getRotation();
        final Vector3d headRot = this.entity.get(Keys.HEAD_ROTATION).orElse(null);
        final Vector3d pos = this.entity.getPosition();

        final long xu = (long) (pos.getX() * 4096);
        final long yu = (long) (pos.getY() * 4096);
        final long zu = (long) (pos.getZ() * 4096);

        final byte yaw = wrapAngle(rot.getY());
        // All living entities have a head rotation and changing the pitch
        // would only affect the head pitch.
        final byte pitch = wrapAngle((headRot != null ? headRot : rot).getX());

        boolean dirtyPos = xu != this.lastX || yu != this.lastY || zu != this.lastZ;
        boolean dirtyRot = yaw != this.lastYaw || pitch != this.lastPitch;

        // TODO: On ground state
        boolean onGround = this.entity.getOnGround();

        final int entityId = getRootEntityId();
        final boolean passenger = this.entity.getVehicle() != null;

        if (dirtyRot) {
            this.lastYaw = yaw;
            this.lastPitch = pitch;
        }
        if (dirtyPos) {
            final long dxu = xu - this.lastX;
            final long dyu = yu - this.lastY;
            final long dzu = zu - this.lastZ;
            this.lastX = xu;
            this.lastY = yu;
            this.lastZ = zu;

            // Don't send movement messages if the entity
            // is a passengers, otherwise glitches will
            // rule the world.
            if (!passenger) {
                if (Math.abs(dxu) <= Short.MAX_VALUE && Math.abs(dyu) <= Short.MAX_VALUE && Math.abs(dzu) <= Short.MAX_VALUE) {
                    if (dirtyRot) {
                        context.sendToAllExceptSelf(new EntityLookAndRelativeMovePacket(entityId,
                                (int) dxu, (int) dyu, (int) dzu, yaw, pitch, onGround));
                        // The rotation is already send
                        dirtyRot = false;
                    } else {
                        context.sendToAllExceptSelf(new EntityRelativeMovePacket(entityId,
                                (int) dxu, (int) dyu, (int) dzu, onGround));
                    }
                } else {
                    context.sendToAllExceptSelf(new EntityTeleportPacket(entityId,
                            pos, yaw, pitch, onGround));
                    // The rotation is already send
                    dirtyRot = false;
                }
            }
        }
        if (dirtyRot) {
            context.sendToAllExceptSelf(() -> new EntityLookPacket(entityId, yaw, pitch, onGround));
        } else if (!passenger) {
            if (headRot != null) {
                final byte headYaw = wrapAngle(headRot.getY());
                if (headYaw != this.lastHeadYaw) {
                    context.sendToAllExceptSelf(() -> new EntityHeadLookPacket(entityId, headYaw));
                    this.lastHeadYaw = headYaw;
                }
            }
        }
        final Vector3d velocity = this.entity.get(Keys.VELOCITY).orElse(Vector3d.ZERO);
        final double vx = velocity.getX();
        final double vy = velocity.getY();
        final double vz = velocity.getZ();
        if (vx != this.lastVelX || vy != this.lastVelY || vz != this.lastVelZ) {
            context.sendToAll(() -> new EntityVelocityPacket(entityId, vx, vy, vz));
            this.lastVelX = vx;
            this.lastVelY = vy;
            this.lastVelZ = vz;
        }
        final ParameterList parameterList = context == EntityProtocolUpdateContext.empty() ?
                EmptyParameterList.INSTANCE : new DefaultParameterList();
        update(parameterList);
        // There were parameters applied
        if (!parameterList.isEmpty()) {
            context.sendToAll(() -> new EntityMetadataPacket(entityId, parameterList));
        }
        if (hasEquipment() && this.entity instanceof Carrier) {
            final IInventory inventory = (IInventory) ((Carrier) this.entity).getInventory();
            /*
            TODO: Fix
            for (int i = 0; i < Holder.EQUIPMENT_TYPES.length; i++) {
                final ItemStack itemStack = inventory.query(Holder.EQUIPMENT_QUERIES[i]).first().peek();
                final ItemStack oldItemStack = this.lastEquipment.get(i);
                if (!LanternItemStack.areSimilar(itemStack, oldItemStack)) {
                    this.lastEquipment.put(i, itemStack);
                    final int slotIndex = i;
                    context.sendToAllExceptSelf(() -> new MessagePlayOutEntityEquipment(getRootEntityId(), slotIndex, itemStack));
                }
            }
            */
        }
        // TODO: Update attributes
    }

    @Override
    protected void updateTranslations(EntityProtocolUpdateContext context) {
        final ParameterList parameterList = new DefaultParameterList();
        updateTranslations(parameterList);
        // There were parameters applied
        if (!parameterList.isEmpty()) {
            context.sendToAll(() -> new EntityMetadataPacket(getRootEntityId(), parameterList));
        }
    }

    /**
     * Gets whether the entity can hold equipment
     * on the client side.
     *
     * @return Has equipment
     */
    protected boolean hasEquipment() {
        return false;
    }

    @Override
    protected void handleEvent(EntityProtocolUpdateContext context, EntityEvent event) {
        if (event instanceof CollectEntityEvent) {
            final LanternLiving collector = (LanternLiving) ((CollectEntityEvent) event).getCollector();
            context.getId(collector).ifPresent(id -> {
                final int count = ((CollectEntityEvent) event).getCollectedItemsCount();
                context.sendToAll(() -> new EntityCollectItemPacket(id, getRootEntityId(), count));
            });
        } else {
            super.handleEvent(context, event);
        }
    }

    @Override
    protected void postUpdate(EntityProtocolUpdateContext context) {
        final IntSet passengers = getPassengerIds(context);
        if (!passengers.equals(this.lastPassengers)) {
            this.lastPassengers = passengers;
            context.sendToAll(new SetEntityPassengersPacket(getRootEntityId(), passengers.toIntArray()));
        }
    }

    @Override
    public void postSpawn(EntityProtocolUpdateContext context) {
        context.sendToAll(new SetEntityPassengersPacket(getRootEntityId(), getPassengerIds(context).toIntArray()));
    }

    protected IntSet getPassengerIds(EntityProtocolUpdateContext context) {
        final IntSet passengerIds = new IntOpenHashSet();
        for (Entity passenger : this.entity.getPassengers()) {
            context.getId(passenger).ifPresent(passengerIds::add);
        }
        return passengerIds;
    }

    /**
     * Fills a {@link ParameterList} with parameters to spawn the {@link Entity}.
     *
     * @return The byte buffer
     */
    ParameterList fillSpawnParameters() {
        final ParameterList parameterList = new DefaultParameterList();
        spawn(parameterList);
        return parameterList;
    }

    protected boolean isSprinting() {
        return false;
    }

    /**
     * Fills the {@link ParameterList} with parameters to spawn the {@link Entity} on
     * the client.
     *
     * @param parameterList The parameter list to fill
     */
    protected void spawn(ParameterList parameterList) {
        parameterList.add(EntityParameters.Base.FLAGS, packFlags());
        parameterList.add(EntityParameters.Base.AIR_LEVEL, getAirLevel());
        parameterList.add(EntityParameters.Base.CUSTOM_NAME, getCustomName());
        parameterList.add(EntityParameters.Base.CUSTOM_NAME_VISIBLE, isCustomNameVisible());
        parameterList.add(EntityParameters.Base.IS_SILENT, isSilent());
        parameterList.add(EntityParameters.Base.NO_GRAVITY, hasNoGravity());
        parameterList.add(EntityParameters.Base.POSE, getPose());
    }

    Pose getPose() {
        return this.entity.get(LanternKeys.POSE).orElse(Pose.STANDING);
    }

    boolean isCustomNameVisible() {
        return this.entity.get(Keys.IS_CUSTOM_NAME_VISIBLE).orElse(true);
    }

    Optional<Text> getCustomName() {
        return this.entity.get(Keys.DISPLAY_NAME);
    }

    boolean isSilent() {
        // Always silent for regular entities, we will handle our own sounds
        return true;
    }

    boolean hasNoGravity() {
        // Always disable gravity for regular entities, we will handle our own physics
        return true;
    }

    private byte packFlags() {
        byte flags = 0;
        if (this.entity.get(Keys.FIRE_TICKS).orElse(0) > 0) {
            flags |= 0x01;
        }
        final Pose pose = getPose();
        if (pose == Pose.SNEAKING) {
            flags |= 0x02;
        }
        if (isSprinting()) {
            flags |= 0x08;
        }
        if (pose == Pose.SWIMMING) {
            flags |= 0x10;
        }
        if (this.entity.get(Keys.IS_INVISIBLE).orElse(false)) {
            flags |= 0x20;
        }
        if (this.entity.get(Keys.IS_GLOWING).orElse(false)) {
            flags |= 0x40;
        }
        if (this.entity.get(Keys.IS_ELYTRA_FLYING).orElse(false)) {
            flags |= 0x80;
        }
        return flags;
    }

    /**
     * Fills the {@link ParameterList} with parameters to update the {@link Entity} on
     * the client.
     *
     * @param parameterList The parameter list to fill
     */
    protected void update(ParameterList parameterList) {
        final byte flags = packFlags();
        if (flags != this.lastFlags) {
            parameterList.add(EntityParameters.Base.FLAGS, flags);
            this.lastFlags = flags;
        }
        final boolean silent = isSilent();
        if (silent != this.lastSilent) {
            parameterList.add(EntityParameters.Base.IS_SILENT, silent);
            this.lastSilent = silent;
        }
        final boolean customNameVisible = isCustomNameVisible();
        if (customNameVisible != this.lastCustomNameVisible) {
            parameterList.add(EntityParameters.Base.CUSTOM_NAME_VISIBLE, customNameVisible);
            this.lastCustomNameVisible = customNameVisible;
        }
        final Optional<Text> customName = getCustomName();
        if (!Objects.equal(customName, this.lastCustomName)) {
            parameterList.add(EntityParameters.Base.CUSTOM_NAME, customName);
            this.lastCustomName = customName;
        }
        final short airLevel = getAirLevel();
        if (airLevel != this.lastAirLevel) {
            parameterList.add(EntityParameters.Base.AIR_LEVEL, airLevel);
            this.lastAirLevel = airLevel;
        }
        final Pose pose = getPose();
        if (!Objects.equal(pose, this.lastPose)) {
            parameterList.add(EntityParameters.Base.POSE, pose);
            this.lastPose = pose;
        }
    }

    /**
     * Fills the {@link ParameterList} with parameters to update the {@link Entity} on
     * the client related to localized {@link Text}.
     *
     * @param parameterList The parameter list to fill
     */
    protected void updateTranslations(ParameterList parameterList) {
        final Optional<Text> customName = getCustomName();
        customName.filter(TranslationHelper::containsNonMinecraftTranslation).ifPresent(text ->
                parameterList.add(EntityParameters.Base.CUSTOM_NAME, customName));
    }

    /**
     * Gets the air level of the entity.
     *
     * The air is by default 300 because the entities don't even use the air level,
     * except for the players. This method can be overridden if needed.
     *
     * @return The air level
     */
    protected short getAirLevel() {
        return 300;
    }
}

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
package org.lanternpowered.server.entity.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.lanternpowered.api.cause.CauseStack;
import org.lanternpowered.server.behavior.BehaviorContext;
import org.lanternpowered.server.behavior.BehaviorContextImpl;
import org.lanternpowered.server.behavior.BehaviorResult;
import org.lanternpowered.server.behavior.ContextKeys;
import org.lanternpowered.server.block.LanternBlockType;
import org.lanternpowered.server.block.behavior.types.BreakBlockBehavior;
import org.lanternpowered.server.block.behavior.types.InteractWithBlockBehavior;
import org.lanternpowered.server.data.key.LanternKeys;
import org.lanternpowered.server.entity.event.SwingHandEntityEvent;
import org.lanternpowered.server.game.Lantern;
import org.lanternpowered.server.game.LanternGame;
import org.lanternpowered.server.game.registry.type.block.BlockRegistryModule;
import org.lanternpowered.server.inventory.AbstractSlot;
import org.lanternpowered.server.inventory.LanternItemStack;
import org.lanternpowered.server.inventory.PlayerInventoryContainer;
import org.lanternpowered.server.inventory.client.ClientContainer;
import org.lanternpowered.server.item.ItemKeys;
import org.lanternpowered.server.item.LanternItemType;
import org.lanternpowered.server.item.behavior.types.FinishUsingItemBehavior;
import org.lanternpowered.server.item.behavior.types.InteractWithItemBehavior;
import org.lanternpowered.server.network.vanilla.packet.type.play.ClientBlockPlacementPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.FinishUsingItemPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.ClientDiggingPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.ClientPlayerSwingArmPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.ClientUseItemPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.BlockBreakAnimationPacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.BlockChangePacket;
import org.lanternpowered.server.network.vanilla.packet.type.play.EntityAnimationPacket;
import org.lanternpowered.server.world.LanternWorld;
import org.lanternpowered.server.world.LanternWorldNew;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.World;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

@SuppressWarnings("ConstantConditions")
public final class PlayerInteractionHandler {

    private final LanternPlayer player;

    /**
     * The block that is being digged.
     */
    @Nullable private Vector3i diggingBlock;
    @Nullable private BlockType diggingBlockType;

    /**
     * The time when the digging should end.
     */
    private long diggingEndTime;
    /**
     * The amount of time the digging takes.
     */
    private long diggingDuration;
    /**
     * The last send break state.
     */
    private int lastBreakState = -1;

    private long lastInteractionTime = -1;

    @Nullable private HandType lastActiveHand;
    @Nullable private ItemStack lastActiveItemStack;

    private long activeHandStartTime = -1L;

    public PlayerInteractionHandler(LanternPlayer player) {
        this.player = player;
    }

    ItemStackSnapshot getActiveItem() {
        return this.lastActiveItemStack == null ? ItemStackSnapshot.empty() : this.lastActiveItemStack.createSnapshot();
    }

    /**
     * Resets the player interaction handler, is called when the
     * player gets teleported to a different world or is removed.
     */
    void reset() {
        sendBreakUpdate(-1);
    }

    /**
     * Pulses the interaction handler.
     */
    void pulse() {
        if (this.diggingBlock != null) {
            final int breakState = (int) Math.round(((double) Math.max(0, this.diggingEndTime - System.nanoTime())
                    / (double) this.diggingDuration) * 10.0);
            if (this.lastBreakState != breakState) {
                sendBreakUpdate(breakState);
                this.lastBreakState = breakState;
            }
        }
        final HandType activeHand = this.player.get(LanternKeys.ACTIVE_HAND).orElse(null);
        final AbstractSlot slot = activeHand == null ? null : activeHand == HandTypes.MAIN_HAND.get() ?
                this.player.getInventory().getHotbar().getSelectedSlot() : this.player.getInventory().getOffhand();
        // The interaction just started
        if (!Objects.equals(activeHand, this.lastActiveHand)) {
            this.lastActiveHand = activeHand;
            this.lastActiveItemStack = slot == null ? null : slot.getRawItemStack();
        } else if (activeHand != null) {
            if (this.activeHandStartTime == -1L) {
                this.activeHandStartTime = LanternGame.currentTimeTicks();
            }
            final ItemStack itemStack = slot.getRawItemStack();
            if (itemStack == null || this.lastActiveItemStack != itemStack) {
                // Stop the interaction
                resetItemUseTime();
            } else {
                final OptionalInt property = itemStack.getInt(ItemKeys.MAXIMUM_USE_DURATION);
                if (property.isPresent()) {
                    // Check if the interaction reached it's max time
                    final long time = LanternGame.currentTimeTicks();
                    if (time - this.activeHandStartTime > property.getAsInt()) {
                        handleFinishItemInteraction0(slot, activeHand);
                    }
                }
            }
        }
    }

    /**
     * TODO: Maybe also send this to the player this handler is attached to for
     * custom break times? Only allowed by faster breaking.
     */
    private void sendBreakUpdate(int breakState) {
        final LanternWorldNew world = this.player.getNullableWorld();
        if (world == null) {
            return;
        }
        final Collection<LanternPlayer> players = this.player.getWorld().getUnsafePlayers();
        // Update for all the players except the breaker
        if (players.size() - 1 <= 0) {
            final BlockBreakAnimationPacket message = new BlockBreakAnimationPacket(
                    this.diggingBlock, this.player.getNetworkId(), breakState);
            players.forEach(player -> {
                if (player != this.player) {
                    player.getConnection().send(message);
                }
            });
        }
    }

    /**
     * Handles the {@link ClientDiggingPacket}.
     *
     * @param message The message
     */
    public void handleDigging(ClientDiggingPacket message) {
        final ClientDiggingPacket.Action action = message.getAction();
        final Vector3i blockPos = message.getPosition();

        if (action == ClientDiggingPacket.Action.START) {
            // Check if the block is within the players reach
            if (this.player.getPosition().distanceSquared(blockPos.toDouble().add(0.5, 2.0, 0.5)) > 6.0 * 6.0) {
                return;
            }
            if (this.diggingBlock != null) {
                Lantern.getLogger().warn("{} started breaking a block without finishing the last one.", this.player.getName());
            }

            final BlockType blockType = this.player.getWorld().getBlock(blockPos).getType();
            if (blockType == BlockTypes.AIR) {
                return;
            }

            this.diggingBlock = blockPos;
            this.diggingBlockType = blockType;

            this.diggingDuration = getDiggingDuration(blockPos);
            // The client won't send a finish message
            if (this.diggingDuration == 0) {
                handleBrokenBlock();
            } else {
                this.diggingEndTime = this.diggingDuration == -1 ? -1 : System.nanoTime() + this.diggingDuration;
            }
        } else if (action == ClientDiggingPacket.Action.CANCEL) {
            if (this.diggingBlock == null || !this.diggingBlock.equals(blockPos)) {
                return;
            }

            if (this.lastBreakState != -1) {
                sendBreakUpdate(-1);
            }
            this.diggingBlock = null;
            this.diggingBlockType = null;
        } else {
            if (this.diggingBlock == null) {
                return;
            }
            final BlockType blockType = this.player.getWorld().getBlock(blockPos).getType();
            if (blockType != this.diggingBlockType) {
                return;
            }
            if (this.diggingEndTime == -1) {
                Lantern.getLogger().warn("{} attempted to break a unbreakable block.", this.player.getName());
            } else {
                final long deltaTime = System.nanoTime() - this.diggingEndTime;
                if (deltaTime < 0) {
                    Lantern.getLogger().warn("{} finished breaking a block too early, {}ms too fast.",
                            this.player.getName(), -(deltaTime / 1000));
                }
                handleBrokenBlock();
            }
        }
    }

    private void handleBrokenBlock() {
        final ServerLocation location = ServerLocation.of(this.player.getWorld(), this.diggingBlock);

        final CauseStack causeStack = CauseStack.current();
        try (CauseStack.Frame frame = causeStack.pushCauseFrame()) {
            frame.pushCause(this.player);

            // Add context
            frame.addContext(EventContextKeys.PLAYER, this.player);
            frame.addContext(ContextKeys.INTERACTION_LOCATION, location);
            frame.addContext(ContextKeys.BLOCK_LOCATION, location);

            final BehaviorContextImpl context = new BehaviorContextImpl(causeStack);

            final BlockState blockState = location.getBlock();
            final LanternBlockType blockType = (LanternBlockType) blockState.getType();
            if (context.process(blockType.getPipeline().pipeline(BreakBlockBehavior.class),
                    (ctx, behavior) -> behavior.tryBreak(blockType.getPipeline(), ctx)).isSuccess()) {
                context.accept();

                this.diggingBlock = null;
                this.diggingBlockType = null;
            } else {
                context.revert();

                // TODO: Resend entity entity data, action data, ... ???
                this.player.sendBlockChange(this.diggingBlock, blockState);
            }
            if (this.lastBreakState != -1) {
                sendBreakUpdate(-1);
            }
        }
    }

    private long getDiggingDuration(Vector3i pos) {
        if (this.player.get(Keys.GAME_MODE).get() == GameModes.CREATIVE) {
            return 0;
        }
        // Don't pass the players profile through, this avoids an
        // unnecessary user lookup
        return this.player.getWorld().getBlockDigTimeWith(pos.getX(), pos.getY(), pos.getZ(),
                this.player.getItemInHand(HandTypes.MAIN_HAND), null);
    }

    public void handleBlockPlacing(ClientBlockPlacementPacket message) {
        handleBlockPlacing0(message);

        // Send some updates to the client
        Vector3i position = message.getPosition();
        final World world = this.player.getWorld();
        this.player.getConnection().send(new BlockChangePacket(position,
                BlockRegistryModule.get().getStateInternalId(world.getBlock(position))));
        position = position.add(message.getFace().asBlockOffset());
        this.player.getConnection().send(new BlockChangePacket(position,
                BlockRegistryModule.get().getStateInternalId(world.getBlock(position))));
    }

    private void handleBlockPlacing0(ClientBlockPlacementPacket message) {
        final HandType handType = message.getHandType();
        // Ignore the off hand interaction type for now, a main hand message
        // will always be send before this message. So we will only listen for
        // the main hand message.
        if (handType == HandTypes.OFF_HAND) {
            return;
        }

        // Try the action of the hotbar item first
        final AbstractSlot hotbarSlot = this.player.getInventory().getHotbar().getSelectedSlot();
        final AbstractSlot offHandSlot = this.player.getInventory().getOffhand();
        // The offset can round up to 1, causing
        // an incorrect clicked block location
        final Vector3d pos2 = message.getClickOffset();
        final double dx = Math.min(pos2.getX(), 0.999);
        final double dy = Math.min(pos2.getY(), 0.999);
        final double dz = Math.min(pos2.getZ(), 0.999);

        final ServerLocation clickedLocation = ServerLocation.of(this.player.getWorld(),
                message.getPosition().toDouble().add(dx, dy, dz));
        final Direction face = message.getFace();

        final CauseStack causeStack = CauseStack.current();
        try (CauseStack.Frame frame = causeStack.pushCauseFrame()) {
            frame.pushCause(this.player);

            // Add context
            frame.addContext(ContextKeys.INTERACTION_FACE, face);
            frame.addContext(ContextKeys.INTERACTION_LOCATION, clickedLocation);
            frame.addContext(ContextKeys.BLOCK_LOCATION, ServerLocation.of(clickedLocation.getWorld(), message.getPosition()));
            frame.addContext(ContextKeys.PLAYER, this.player);

            final BehaviorContextImpl context = new BehaviorContextImpl(causeStack);
            final BehaviorContext.Snapshot snapshot = context.pushSnapshot();

            if (!this.player.get(Keys.IS_SNEAKING).orElse(false)) {
                final BlockState blockState = this.player.getWorld().getBlock(message.getPosition());

                final LanternBlockType blockType = (LanternBlockType) blockState.getType();
                frame.addContext(ContextKeys.BLOCK_TYPE, blockState.getType());
                frame.addContext(ContextKeys.USED_BLOCK_STATE, blockState);

                BehaviorContext.Snapshot snapshot1 = context.pushSnapshot();

                // Try first with the main hand
                hotbarSlot.peek().ifNotEmpty(stack -> frame.addContext(ContextKeys.USED_ITEM_STACK, stack));
                frame.addContext(ContextKeys.USED_SLOT, hotbarSlot);
                frame.addContext(ContextKeys.INTERACTION_HAND, HandTypes.MAIN_HAND);

                BehaviorResult result = context.process(blockType.getPipeline().pipeline(InteractWithBlockBehavior.class),
                        (ctx, behavior) -> behavior.tryInteract(blockType.getPipeline(), ctx));
                if (!result.isSuccess()) {
                    context.popSnapshot(snapshot1);
                    snapshot1 = context.pushSnapshot();

                    // Try again with the off hand
                    offHandSlot.peek().ifNotEmpty(stack -> frame.addContext(ContextKeys.USED_ITEM_STACK, stack));
                    frame.addContext(ContextKeys.USED_SLOT, offHandSlot);
                    frame.addContext(ContextKeys.INTERACTION_HAND, HandTypes.OFF_HAND);

                    result = context.process(blockType.getPipeline().pipeline(InteractWithBlockBehavior.class),
                            (ctx, behavior) -> behavior.tryInteract(blockType.getPipeline(), ctx));
                }
                if (result.isSuccess()) {
                    snapshot1 = context.pushSnapshot();
                    // We can still continue, doing other operations
                    if (result == BehaviorResult.CONTINUE) {
                        handleMainHandItemInteraction(context, snapshot1);
                    }
                    context.accept();
                    return;
                }

                context.popSnapshot(snapshot1);
                snapshot1 = context.pushSnapshot();

                if (result.isSuccess()) {
                    snapshot1 = context.pushSnapshot();
                    // We can still continue, doing other operations
                    if (result == BehaviorResult.CONTINUE) {
                        handleOffHandItemInteraction(context, snapshot1);
                    }
                    context.accept();
                    return;
                }

                context.popSnapshot(snapshot1);
            }

            handleItemInteraction(context, snapshot);
        }
    }

    public void handleSwingArm(ClientPlayerSwingArmPacket message) {
        if (message.getHandType() == HandTypes.OFF_HAND) {
            return;
        }
        this.player.triggerEvent(SwingHandEntityEvent.of(HandTypes.MAIN_HAND));
    }

    public void handleFinishItemInteraction(FinishUsingItemPacket message) {
        final Optional<HandType> activeHand = this.player.get(LanternKeys.ACTIVE_HAND);
        // The player is already interacting
        if (!activeHand.isPresent() || this.activeHandStartTime == -1L) {
            return;
        }

        // Try the action of the hotbar item first
        final AbstractSlot slot = activeHand.get() == HandTypes.MAIN_HAND ?
                this.player.getInventory().getHotbar().getSelectedSlot() : this.player.getInventory().getOffhand();

        final ItemStack rawItemStack = slot.getRawItemStack();
        if (rawItemStack == null) {
            return;
        }

        // Require a minimum amount of ticks for the interaction to succeed
        final Optional<Integer> minUseDuration = rawItemStack.get(ItemKeys.MINIMUM_USE_DURATION);
        if (minUseDuration.isPresent()) {
            final long time = LanternGame.currentTimeTicks();
            if (time - this.activeHandStartTime < minUseDuration.get()) {
                resetItemUseTime();
                return;
            }
        }

        handleFinishItemInteraction0(slot, activeHand.get());
    }

    private void handleFinishItemInteraction0(AbstractSlot slot, HandType handType) {
        final LanternItemStack handItem = slot.peek();
        if (!handItem.isEmpty()) {
            final CauseStack causeStack = CauseStack.current();
            try (CauseStack.Frame frame = causeStack.pushCauseFrame()) {
                frame.pushCause(this.player);
                frame.addContext(ContextKeys.PLAYER, this.player);

                final LanternItemType itemType = (LanternItemType) handItem.getType();
                frame.addContext(ContextKeys.USED_ITEM_STACK, handItem);
                frame.addContext(ContextKeys.USED_SLOT, slot);
                frame.addContext(ContextKeys.INTERACTION_HAND, handType);
                frame.addContext(ContextKeys.ITEM_TYPE, itemType);

                final BehaviorContextImpl context = new BehaviorContextImpl(causeStack);
                if (context.process(itemType.getPipeline().pipeline(FinishUsingItemBehavior.class),
                        (ctx, behavior) -> behavior.tryUse(itemType.getPipeline(), ctx)).isSuccess()) {
                    context.accept();
                }
            }
        }
        resetItemUseTime();
    }

    private void resetItemUseTime() {
        this.lastActiveItemStack = null;
        this.lastActiveHand = null;
        this.activeHandStartTime = -1L;
        this.player.remove(LanternKeys.ACTIVE_HAND);
    }

    void cancelActiveItem() {
        // Refresh the active item slot
        final PlayerInventoryContainer container = this.player.getInventoryContainer();
        final ClientContainer clientContainer = container.getClientContainer();
        final Slot activeSlot = container.getPlayerInventory().getHotbar().getSelectedSlot();
        clientContainer.queueSlotChange(activeSlot);
        // Reset use time
        resetItemUseTime();
    }

    public void handleItemInteraction(ClientUseItemPacket message) {
        // Prevent duplicate messages
        final long time = System.currentTimeMillis();
        if (this.lastInteractionTime != -1L && time - this.lastInteractionTime < 40) {
            return;
        }
        this.lastInteractionTime = time;

        final CauseStack causeStack = CauseStack.current();
        try (CauseStack.Frame frame = causeStack.pushCauseFrame()) {
            final BehaviorContextImpl context = new BehaviorContextImpl(causeStack);
            frame.pushCause(this.player);
            frame.addContext(ContextKeys.PLAYER, this.player);

            final BehaviorContext.Snapshot snapshot = context.pushSnapshot();
            if (!handleItemInteraction(context, snapshot)) {
                if (!this.player.get(LanternKeys.CAN_DUAL_WIELD).orElse(false)) {
                    return;
                }
                final AbstractSlot offHandSlot = this.player.getInventory().getOffhand();
                final LanternItemStack handItem = offHandSlot.peek();
                if (handItem.isNotEmpty()) {
                    if (handItem.get(LanternKeys.IS_DUAL_WIELDABLE).orElse(false)) {
                    /*
                    final Vector3d position = this.player.getPosition().add(0, this.player.get(Keys.IS_SNEAKING).get() ? 1.54 : 1.62, 0);
                    final Optional<BlockRayHit<LanternWorld>> hit = BlockRay.from(this.player.getWorld(), position)
                            .direction(this.player.getDirectionVector())
                            .distanceLimit(5)
                            // Is this supposed to be inverted?
                            .skipFilter(Predicates.not(BlockRay.onlyAirFilter()))
                            .build()
                            .end();
                    if (hit.isPresent() && hit.get().getLocation().getBlock().getType() != BlockTypes.AIR) {
                        return;
                    }
                    */
                        this.player.getConnection().send(new EntityAnimationPacket(this.player.getNetworkId(), 3));
                        this.player.triggerEvent(SwingHandEntityEvent.of(HandTypes.OFF_HAND));
                    /*
                    final CooldownTracker cooldownTracker = this.player.getCooldownTracker();
                    cooldownTracker.set(handItem.get().getType(), 15);
                    */
                    }
                }
            }
        }
    }

    private boolean isInteracting() {
        return this.player.get(LanternKeys.ACTIVE_HAND).isPresent();
    }

    private boolean handleOffHandItemInteraction(BehaviorContextImpl context, BehaviorContext.@Nullable Snapshot snapshot) {
        return handleHandItemInteraction(context, HandTypes.OFF_HAND.get(),
                this.player.getInventory().getOffhand(), snapshot);
    }

    private boolean handleMainHandItemInteraction(BehaviorContextImpl context, BehaviorContext.@Nullable Snapshot snapshot) {
        return handleHandItemInteraction(context, HandTypes.MAIN_HAND.get(), this.player.getInventory().getHotbar().getSelectedSlot(), snapshot);
    }

    private boolean handleHandItemInteraction(BehaviorContextImpl context, HandType handType, AbstractSlot slot,
            BehaviorContext.@Nullable Snapshot snapshot) {
        final Optional<HandType> activeHand = this.player.get(LanternKeys.ACTIVE_HAND);
        // The player is already interacting
        if (activeHand.isPresent()) {
            return true;
        }
        final LanternItemStack handItem = slot.peek();
        if (handItem.isNotEmpty()) {
            final LanternItemType itemType = (LanternItemType) handItem.getType();
            context.addContext(ContextKeys.USED_ITEM_STACK, handItem);
            context.addContext(ContextKeys.USED_SLOT, slot);
            context.addContext(ContextKeys.INTERACTION_HAND, handType);
            context.addContext(ContextKeys.ITEM_TYPE, itemType);

            final BehaviorResult result = context.process(itemType.getPipeline().pipeline(InteractWithItemBehavior.class),
                    (ctx, behavior) -> behavior.tryInteract(itemType.getPipeline(), ctx));
            if (result.isSuccess()) {
                return true;
            }
            if (snapshot != null) {
                context.popSnapshot(snapshot);
            }
        }
        return false;
    }

    private boolean handleItemInteraction(BehaviorContextImpl context, BehaviorContext.@Nullable Snapshot snapshot) {
        final Optional<HandType> activeHand = this.player.get(LanternKeys.ACTIVE_HAND);
        if (activeHand.isPresent() || handleMainHandItemInteraction(context, snapshot) || handleOffHandItemInteraction(context, null)) {
            context.accept();
            return true;
        } else {
            context.revert();
            return false;
        }
    }
}
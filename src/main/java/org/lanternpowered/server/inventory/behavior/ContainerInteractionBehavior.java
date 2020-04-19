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
package org.lanternpowered.server.inventory.behavior;

import org.lanternpowered.api.cause.CauseStack;
import org.lanternpowered.server.inventory.AbstractContainer;
import org.lanternpowered.server.inventory.behavior.event.ContainerEvent;
import org.lanternpowered.server.inventory.client.ClientContainer;
import org.lanternpowered.server.inventory.client.ClientSlot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the behavior when a {@link Player} interacts with
 * a {@link ClientContainer} and {@link AbstractContainer}.
 * <p>
 * The current {@link CauseStack} will always be populated before
 * a handler method is invoked. In the process will the {@link Player}
 * be added as normal cause and context value. The {@link ClientContainer}
 * will also be available in the cause.
 * A {@link CauseStack.Frame} will be entered before the method is executed
 * and exited when it is done.
 */
public interface ContainerInteractionBehavior {

    /**
     * Handles a shift-click operation with a specific {@link MouseButton}
     * for the target {@link ClientSlot}.
     * <p>
     * Shift-clicking in combination with {@link MouseButton#MIDDLE} is
     * currently not supported by the client.
     *
     * @param clientContainer The client container
     * @param clientSlot The client slot that was clicked
     * @param mouseButton The mouse button that was used in the shift click
     */
    void handleShiftClick(ClientContainer clientContainer, ClientSlot clientSlot, MouseButton mouseButton);

    /**
     * Handles a double-click operation for the target {@link ClientSlot}.
     *
     * @param clientContainer The client container
     * @param clientSlot The client slot that was double clicked
     */
    void handleDoubleClick(ClientContainer clientContainer, ClientSlot clientSlot);

    /**
     * Handles a regular click operation for the target {@link ClientSlot}
     * ({@code null} can occur when clicking outside the container),
     * only a specific {@link MouseButton} is used (no other keys/buttons).
     *
     * @param clientContainer The client container
     * @param clientSlot The client slot that was clicked
     * @param mouseButton The mouse button that was used in the regular click
     */
    void handleClick(ClientContainer clientContainer, @Nullable ClientSlot clientSlot, MouseButton mouseButton);

    /**
     * Handles a drop key operation for the target {@link ClientSlot}. {@code ctrl}
     * defines that the control key was pressed when pressing the drop key.
     *
     * @param clientContainer The client container
     * @param clientSlot The client slot that was selected when pressing the key
     * @param ctrl Is the control key pressed
     */
    void handleDropKey(ClientContainer clientContainer, ClientSlot clientSlot, boolean ctrl);

    /**
     * Handles a number key operation for the target {@link ClientSlot}. {@code number}
     * defines which number key was pressed.
     *
     * @param clientContainer The client container
     * @param clientSlot The client slot that was selected when pressing the key
     * @param number The pressed number key, counting from 1 to 9
     */
    void handleNumberKey(ClientContainer clientContainer, ClientSlot clientSlot, int number);

    /**
     * Handles a drag operation for the target {@link ClientSlot}s. While dragging
     * was a specific {@link MouseButton} used. The {@link ClientSlot} are provided
     * in the order that they were dragged, the list will never be empty.
     *
     * @param clientContainer The client container
     * @param clientSlots The client slots
     * @param mouseButton The mouse button
     */
    void handleDrag(ClientContainer clientContainer, List<ClientSlot> clientSlots, MouseButton mouseButton);

    /**
     * Handles a creative click operation for the target {@link ClientSlot}
     * ({@code null} can occur when clicking outside the container).
     * On vanilla minecraft will the provided {@link ItemStack} be put in
     * the target {@link ClientSlot}.
     *
     * @param clientContainer The client container
     * @param clientSlot The client slot
     * @param itemStack The item stack
     */
    void handleCreativeClick(ClientContainer clientContainer, @Nullable ClientSlot clientSlot, ItemStack itemStack);

    /**
     * Handles a item pick operation. The client sends a {@link ClientSlot} to swap
     * the contents with the hotbar slot, this occurs when a player middle clicks a
     * block. The block item stack must exactly match the contents in the target slot
     * before a operation will be executed.
     *
     * @param clientContainer The client container
     * @param clientSlot The client slot
     */
    void handlePick(ClientContainer clientContainer, @Nullable ClientSlot clientSlot);

    /**
     * Handles the target {@link ContainerEvent}.
     *
     * @param clientContainer The client container
     * @param event The event to process
     */
    void handleEvent(ClientContainer clientContainer, ContainerEvent event);
}

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
@file:Suppress("NOTHING_TO_INLINE")

package org.lanternpowered.api.item.inventory

import org.lanternpowered.api.item.inventory.stack.fix

typealias ItemStack = org.spongepowered.api.item.inventory.ItemStack

/**
 * Gets an empty [ItemStack].
 */
inline fun emptyItemStack(): ExtendedItemStack =
        ItemStack.empty().fix()

/**
 * An extended version of [ItemStack].
 */
interface ExtendedItemStack : ItemStack {

    /**
     * Gets whether this item stack is similar to the other one.
     *
     * Stacks are similar if all the data matches, excluding
     * the quantity.
     *
     * @param other The other stack to match with
     * @return Whether the stacks are similar
     */
    infix fun isSimilarTo(other: ItemStack): Boolean

    /**
     * Gets whether this item stack is similar to the other one.
     *
     * Stacks are similar if all the data matches, excluding
     * the quantity.
     *
     * @param other The other stack to match with
     * @return Whether the stacks are similar
     */
    infix fun isSimilarTo(other: ItemStackSnapshot): Boolean

    /**
     * Gets whether this item stack is similar to the other one.
     *
     * Stacks are equal if all the data matches, including
     * the quantity.
     *
     * @param other The other stack to match with
     * @return Whether the stacks are similar
     */
    infix fun isEqualTo(other: ItemStack): Boolean

    /**
     * Gets whether this item stack is similar to the other one.
     *
     * Stacks are equal if all the data matches, including
     * the quantity.
     *
     * @param other The other stack to match with
     * @return Whether the stacks are similar
     */
    infix fun isEqualTo(other: ItemStackSnapshot): Boolean

    @Deprecated(message = "Prefer to use isEqualTo", replaceWith = ReplaceWith("this.isEqualTo(other)"))
    override fun equalTo(other: ItemStack): Boolean =
            this.isEqualTo(other)

    /**
     * Creates a *view* of this [ItemStack] as an [ItemStackSnapshot], changes
     * to the item stack will reflect to the snapshot.
     *
     * This should only be used if you know what you're doing, one use case can
     * be reducing the amount of copies that are being created by conversions.
     */
    @UnsafeInventoryApi
    fun asSnapshot(): ExtendedItemStackSnapshot
}
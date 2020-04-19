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
package org.lanternpowered.server.item.recipe

data class RecipeBookState(
        val isCurrentlyOpen: Boolean,
        val isFilterActive: Boolean
) {

    companion object {

        @JvmField
        val DEFAULT = RecipeBookState(isCurrentlyOpen = false, isFilterActive = false)
    }
}

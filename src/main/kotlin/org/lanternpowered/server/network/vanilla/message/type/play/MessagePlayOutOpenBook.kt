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
package org.lanternpowered.server.network.vanilla.message.type.play

import org.lanternpowered.server.network.message.Message
import org.spongepowered.api.data.type.HandType

/**
 * This message opens the book gui of the item the player has currently in its hand.
 */
data class MessagePlayOutOpenBook(val handType: HandType) : Message

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
package org.lanternpowered.server.network.vanilla.message.handler

import org.lanternpowered.server.game.Lantern
import org.lanternpowered.server.network.NetworkContext
import org.lanternpowered.server.network.message.handler.Handler
import org.lanternpowered.server.network.vanilla.message.type.play.MessagePlayInLockDifficulty

class HandlerPlayInLockDifficulty : Handler<MessagePlayInLockDifficulty> {

    override fun handle(context: NetworkContext, message: MessagePlayInLockDifficulty) {
        Lantern.getLogger().info("${context.session.player.name} attempted to ${ if (message.locked) "lock" else "unlock" } the difficulty.")
    }
}

/*
 * This file is part of LanternServer, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the Software), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.server.text.channel

import org.lanternpowered.api.text.Text
import org.spongepowered.api.text.chat.ChatType
import org.spongepowered.api.text.channel.MessageReceiver
import org.spongepowered.api.text.channel.MessageChannel
import java.util.Optional


/**
 * A mutable message channel that leaves transforming and
 * members to the delegate channel passed.
 *
 * The members from the provided channel are copied into our
 * own local collection.
 *
 * @param delegate The delegate channel
 */
class DelegateMutableMessageChannel(private val delegate: MessageChannel) : AbstractMutableMessageChannel() {

    init {
        this.members.addAll(this.delegate.members)
    }

    override fun transformMessage(sender: Any?, recipient: MessageReceiver, original: Text, type: ChatType): Optional<Text> =
            this.delegate.transformMessage(sender, recipient, original, type)
}
